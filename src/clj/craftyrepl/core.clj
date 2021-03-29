(ns craftyrepl.core
  (:require [cljminecraft.config :as cfg]
            [cljminecraft.logging :as log]
            [cljminecraft.files :as files]
            [cljminecraft.events :as ev]
            [cljminecraft.entity :as ent]
            [cljminecraft.player :as plr]
            [cljminecraft.commands :as cmd]            
            [taoensso.timbre :as tlog])
  (:gen-class))


(defmacro with-ns
  "Evaluates body in another namespace.  ns is either a namespace
  object or a symbol.  This makes it possible to define functions in
  namespaces other than the current one."
  [ns & body]
  `(binding [*ns* (the-ns ~ns)]
     ~@(map (fn [form] `(eval '~form)) body)))


(defn mk-cider-nrepl-version []
  (create-ns 'cider.nrepl.version)
  (with-ns 'cider.nrepl.version
    (clojure.core/use 'clojure.core)
    (def version-string "0.21.1")
    (def version {:major 0
                  :minor 25
                  :incremental 9
                  :qualifier ""
                  :version-string version-string})))

#_
 (mk-cider-nrepl-version)

(defonce nrepl-server& (atom nil))

(tlog/set-level! :info)

#_
(def addl-middleware
  '[com.billpiel.sayid.nrepl-middleware/wrap-sayid])

(def addl-middleware [])


(defn resolve-default-handler-fn []
  (try
    (load "/nrepl/server")
    (ns-resolve 'nrepl.server 'default-handler)
    (catch Throwable t
      nil)))


(defn resolve-cider-middleware []
  (try
    (load "/cider/nrepl")
    (some-> (ns-resolve 'cider.nrepl 'cider-middleware)
            deref)
    (catch Throwable t
      nil)))




(defn load-sayid-nrepl-middleware []
  (try
    (load "/com/billpiel/sayid/nrepl_middleware")
    (catch Throwable t
      nil)))

(defn cider-nrepl-handler-override
  []
  (mk-cider-nrepl-version)
  (let [cider-middleware (resolve-cider-middleware)
        default-handler-fn (resolve-default-handler-fn)]
    (->> cider-middleware
         (into addl-middleware)
         (keep resolve)
         (apply default-handler-fn))))

(defn resolve-start-server-fn []
  (try
    (load "/nrepl/server")
    (ns-resolve 'nrepl.server 'start-server)
    (catch Throwable t
      nil)))

(defn resolve-stop-server-fn []
  (try
    (load "/nrepl/server")
    (ns-resolve 'nrepl.server 'stop-server)
    (catch Throwable t
      nil)))

(defn try-start-nrepl-server [host port]
  (try
    (if-not @nrepl-server&
      (if-let [start-fn (resolve-start-server-fn)]
        (do (log/info "starting nrepl server...")
            (reset! nrepl-server& (start-fn :bind host
                                            :port port
                                            :handler (cider-nrepl-handler-override))) 
            (log/info (str "started nrepl server on port " port))
            @nrepl-server&)
        (log/info "Could not resolve `nrepl.server/start-server`"))
      (log/info "nrepl server already running"))
    (catch Throwable t
      (log/error "EXCEPTION while trying to start nrepl server: %s"
                 (with-out-str (clojure.pprint/pprint t))))))

(defn try-stop-nrepl-server []
  (try
    (if-let [nrepl-server @nrepl-server&]
      (if-let [stop-fn (resolve-stop-server-fn)]
        (do (log/info "stoping nrepl server...")
            (stop-fn nrepl-server)
            (reset! nrepl-server& nil)
            (log/info "stopped nrepl server")
            true)
        (log/info "Could not resolve `nrepl.server/stop-server`"))
      (log/info "nrepl server is not running"))
    (catch Throwable t
      (log/error "EXCEPTION while trying to stop nrepl server: %s"
                 (with-out-str (clojure.pprint/pprint t))))))

;; ==== START --- FROM clj-minecraft

(defn start-repl-if-needed [plugin]
  (let [repl-host (cfg/get-string plugin "repl.host")
        repl-port (cfg/get-int plugin "repl.port")]
    (cond
     (not (cfg/get-boolean plugin "repl.enabled"))
     (log/info "REPL Disabled")
     :else
     (let [{:keys [msg] :as response}
           (try-start-nrepl-server repl-host repl-port)
           #_(start-repl repl-host repl-port)]
       (log/info "Repl options: %s %s" repl-host repl-port)
       (if msg (log/info msg))))))

(defonce clj-plugin (atom nil))

(defn script [file]
  (load-file (files/join-path (cfg/get-string @clj-plugin "scripts-directory") file)))

(defn script-command [sender file]
  (log/info "Running script command with %s" file)
  (script file))

(defn repl-command [sender cmd & [port]]
  (log/info "Running repl command with %s %s" cmd port)
  (case cmd
    :start (try-start-nrepl-server "0.0.0.0" (or port (cfg/get-int @clj-plugin "repl.port")))
    #_(start-repl "0.0.0.0" (or port (cfg/get-int @clj-plugin "repl.port")))
    :stop (try-stop-nrepl-server)
    #_(stop-repl)))

(defn tabtest-command [sender & args]
  (.sendMessage sender (apply str args)))

(defn tabcomplete-reverse-first [sender command alias args]
  [(apply str (reverse (first args)))])

(defn addevent-command [sender eventname message]
  (ev/register-event @clj-plugin eventname (fn [ev] (.sendMessage sender (str message ": " ev))))
  {:msg (format "Adding event %s with message %s" eventname message)})

(defn spawn-command [sender entity]
  (ent/spawn-entity (.getLocation sender) entity)
  (log/info "Spawning %s in front of %s" entity (.getName sender)))

;; cljminecraft basic permission system
(defn permission-command [sender player permission allow-type]
  (plr/set-permission player permission allow-type))

(defn player-permission-attach [ev]
  (plr/permission-attach-player! @clj-plugin ev))

(defn player-permission-detach [ev]
  (plr/permission-detach-player! ev))

(defn setup-permission-system
  [plugin]
  (ev/register-eventlist
   plugin
   [(ev/event "player.player-join" #'player-permission-attach)
    (ev/event "player.player-quit" #'player-permission-detach)
    (ev/event "player.player-kick" #'player-permission-detach)])
  (plr/permission-attach-all! plugin))

(defn disable-permission-system
  []
  (plr/permission-detach-all!))

;; cljminecraft specific setup
(defn start
  "onEnable cljminecraft"
  [plugin]
  (reset! clj-plugin plugin)
  (cmd/register-command @clj-plugin "clj.script" #'script-command :string)
  (cmd/register-command @clj-plugin "clj.repl" #'repl-command [:keyword [:start :stop]] [:int [(cfg/get-int plugin "repl.port")]])
  (cmd/register-command @clj-plugin "clj.tabtest" #'tabtest-command :player :material [:keyword [:start :stop]] [:string #'tabcomplete-reverse-first])
  (cmd/register-command @clj-plugin "clj.addevent" #'addevent-command :event :string)
  (cmd/register-command @clj-plugin "clj.spawnentity" #'spawn-command :entity)
  (cmd/register-command @clj-plugin "clj.permission" #'permission-command :player :permission [:keyword [:allow :disallow :release]])
  (setup-permission-system plugin)
  (start-repl-if-needed plugin))

(defn stop
  "onDisable cljminecraft"
  [plugin]
  (try-stop-nrepl-server)
  #_(stop-repl)
  (disable-permission-system))


(defn on-enable
  "to enable self or any child plugins"
  [plugin]
  (cfg/config-defaults plugin)
  (let [plugin-name (.getName plugin)
        resolved (resolve (symbol (str (.getName plugin) ".core/start")))]
    (if (not resolved)
      (log/warn "plugin %s didn't have a start function" plugin-name)
      (do
        ;; the following line is for debugging purposes only, to be removed:
        (log/info "second Repl options: %s %s %s"
                   (cfg/get-string plugin "repl.host")
                   (cfg/get-int plugin "repl.port")
                   (cfg/get-boolean plugin "repl.enabled"))
        (log/info "calling child `start` for %s" plugin-name)
        (resolved plugin))
      )
    )
  (log/info "Clojure started - %s" plugin))

(defn on-disable
  "to disable self or any child plugins"
  [plugin]
  (when-let [resolved (resolve (symbol (str (.getName plugin) ".core/stop")))]
    (resolved plugin))
  (log/info "Clojure stopped - %s" plugin)
  ;the following line is for debugging purposes only, to be removed:
  (log/info "third Repl options: %s %s %s" (cfg/get-string plugin "repl.host") (cfg/get-int plugin "repl.port") (cfg/get-boolean plugin "repl.enabled"))
  )

;; ==== END --- FROM clj-minecraft

(defn -main [& _]
  (try-start-nrepl-server "0.0.0.0" 4005))

#_
(try-start-nrepl-server "0.0.0.0" 4005)

