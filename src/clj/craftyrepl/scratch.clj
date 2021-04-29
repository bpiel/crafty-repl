(ns craftyrepl.scratch
  (:require [bukkitclj.task :as btsk]
            [cljminecraft.blocks :as bl]
            [cljminecraft.items :as i]
            [cljminecraft.bukkit :as bk]
            [cljminecraft.config :as cfg]
            [cljminecraft.logging :as log]
            [cljminecraft.files :as files]
            [cljminecraft.events :as ev]
            [cljminecraft.entity :as ent]
            [cljminecraft.player :as plr]
            [cljminecraft.commands :as cmd]            
            [taoensso.timbre :as tlog])
  (:import [org.bukkit Bukkit Material Location]
           [org.bukkit.block Block]
           [org.bukkit.entity EntityType]
           org.bukkit.util.Vector
           [java.util UUID]))

(defn buk-vec [x y z]
  (org.bukkit.util.Vector. x y z))



(comment

  (def plugin craftyrepl.core/clj-plugin)
  
  (def ctx (bl/setup-context (first (.getOnlinePlayers (bk/server)))))

  (def player (first (.getOnlinePlayers (bk/server))))

  (clojure.pprint/pprint ctx)

  (def loc (:origin ctx))

  (def w (.getWorld loc))

  (def x1 (.getBlockX loc))
  
  (def y1 (.getBlockY loc))
  
  (def z1 (.getBlockZ loc))

  (def b1 (.getBlockAt w x1 y1 z1))

  (let [ctx (bl/setup-context (first (.getOnlinePlayers (bk/server))))
        loc (:origin ctx)
        x (.getBlockX loc)
        y (.getBlockY loc)
        z (.getBlockZ loc)
        bt1 (btsk/bukkit-runnable
             (fn [& _]
               (doall
                (for [dx (range 1 30)
                      dz (range 1 30)]
                  (let [bk1 (.getBlockAt w (+ x dx) y1 (+ z dz))]
                    (.setType bk1 Material/DIAMOND_BLOCK ))))))]
    (.runTask bt1 @plugin))
  
  (def br1 (btsk/bukkit-runnable
            (fn [& _]
              (.setType b1 Material/DIAMOND_BLOCK ))))

  (.runTask br1 @plugin)

  (def br2 (btsk/bukkit-runnable
            (fn [& _]
              (doall
               (for [dz (range 1 30)
                     dy (range 1 30)]
                 (let [bk1 (.getBlockAt w x1 (+ y1 dy) (+ z1 dz))]
                   (.setType bk1 Material/DIAMOND_BLOCK )))))))

  (.runTask br2 @plugin)

  (def x& (atom nil))

  (def fireball-type @x&)

  (.runTask
   (btsk/bukkit-runnable
    (fn [& _]
      (dotimes [_ 4]
        (Thread/sleep 1000)
        (let [v1 (buk-vec 0 2 0)
              p1 (.launchProjectile player fireball-type)]
          (.setIsIncendiary p1 true)
          (.setFireTicks p1 10000)
          (.setBounce p1 true)
          #_          (reset! x& (type p1))
          (.setDirection p1 v1)))))
   @plugin)

  (def ex& (atom nil))

  (.runTask
   (btsk/bukkit-runnable
    (fn [& _]
      (try
        (let [player (first (.getOnlinePlayers (bk/server)))
              ctx (bl/setup-context (first (.getOnlinePlayers (bk/server))))]
          (dotimes [_ 4]
            (Thread/sleep 1000)
            (let [v1 (buk-vec 0 2 0)
                  p1 (.launchProjectile player pig-type)]
              (.setIsIncendiary p1 true)
              (.setFireTicks p1 10000)
              (.setBounce p1 true)
              #_          (reset! x& (type p1))
              #_(.setDirection p1 v1))))
        (catch Throwable t
          (reset! ex& t)))))
   @plugin)
  
  (.runTask
   (btsk/bukkit-runnable
    (fn [& _]
      (dotimes [_ 4]
        (Thread/sleep 1000)
        (let [v1 (buk-vec 0 2 0)
              p1 (.spawnEntity w loc EntityType/FIREBALL)]
          #_          (.setPassenger p1 player)
          (.setIsIncendiary p1 true)
          (.setFireTicks p1 10000)
          (.setBounce p1 true)
          (reset! x& (type p1))
          (.setDirection p1 v1)))))
   @plugin)


  (def pig-type& (atom nil))
  (def pig-type @pig-type&)
  
  (let [pl1 (first (.getOnlinePlayers (bk/server)))
        ctx (bl/setup-context pl1)
        dir (-> pl1 .getLocation .getDirection)
        loc (:origin ctx)
        w (.getWorld loc)
        x (.getBlockX loc)
        y (.getBlockY loc)
        z (.getBlockZ loc)]
    (dotimes [_ 5]
      (Thread/sleep 2000)
      (let [bt1 (btsk/bukkit-runnable
                 (fn [& _]
                   (try
                     (let [v1 (buk-vec -20. 0.1 0.)
                           p1 (.spawnEntity w loc EntityType/PIG )]
                       (.setFireTicks p1 10000)
                       (.setVelocity p1 dir))
                     (catch Throwable t
                       (reset! ex& t)))))]
        (.runTask bt1 @plugin))))

  (reset! ex& nil)
  
  (let [ctx (bl/setup-context (first (.getOnlinePlayers (bk/server))))
        loc (:origin ctx)
        x (.getBlockX loc)
        y (.getBlockY loc)
        z (.getBlockZ loc)
        pigs& (atom [])]

    (let [bt1 (btsk/bukkit-runnable
               (fn [& _]
                 (dotimes [n 10]
                   (Thread/sleep 50)
                   (swap! pigs&
                          conj
                          [n
                           (let [n' (* 16 n)
                                 j' (/ (* Math/PI n') 5)
                                 lx (+ x (/ (Math/sin j')
                                            2))
                                 lz (+ z (/ (Math/cos j')
                                            2))]
                             (.spawnEntity w
                                           (Location. w
                                                      lx
                                                      (double y)
                                                      lz)
                                           EntityType/PIG ))]))))]
      (.runTask bt1 @plugin))
    
    (dotimes [i 160]
      (Thread/sleep 100)
      (let [bt1 (btsk/bukkit-runnable
                 (fn [& _]
                   (doseq [[n p] @pigs&]
                     (let [j (+ i ( * 16 n))
                           j' (/ (* Math/PI j) 5)
                           vx (/ (Math/cos j')
                                 2)
                           vz (/ (Math/sin j')
                                 2)
                           v1 (buk-vec vx
                                       0.04
                                       vz)]
                       (.setVelocity p v1)))))]
        (.runTask bt1 @plugin))))

  
  
  (defn floor-part []
    [(bl/forward 5) (bl/turn-right) (bl/forward 1) (bl/turn-right) (bl/forward 5) (bl/turn-left)
     (bl/forward 1) (bl/turn-left)])

  (defn floor []
    [(floor-part) (floor-part) (floor-part) (floor-part) (floor-part) (floor-part) (floor-part) (floor-part)])

  (bl/run-actions ctx
                  (bl/material :wool)
                  (floor) (bl/turn-around) (bl/up) (floor))

  (run-actions
   ctx
   (material :air)
   (extrude
    :up 10
    (forward 10) (right 10) (back 8) (left 2) (back 2) (left 8))
   )

  (run-actions
   ctx
                                        ;(material :air)
   (line 10 10 10)
   (line 1 2 3)
   (line -5 0 0)
   (line 0 -5 0)
   (line 0 0 -5))
  

  (bk/ui-sync
   @cljminecraft.core/clj-plugin
   #(run-actions ctx (material :air) (mark :start) (left 100) (forward 100) (up 40) (cut-to-mark :start) (clear-mark :start))))
