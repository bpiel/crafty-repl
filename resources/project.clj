(defproject crafty-repl "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :repositories
  [["bukkit.snapshots" "https://hub.spigotmc.org/nexus/content/repositories/snapshots"]
   ["bukkit.release"   "https://hub.spigotmc.org/nexus/content/repositories/releases"]]
  
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.spigotmc/spigot-api "1.14.4-R0.1-SNAPSHOT"]
                 [nrepl "0.7.0"]
                 [cider/cider-nrepl "0.25.9"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [cheshire                    "5.9.0"]   
                 [com.taoensso/timbre "5.1.2"]
                 [org.reflections/reflections "0.9.12"]]
  
  ;;  :main ^:skip-aot craftyrepl.core
  :main craftyrepl.core
  :target-path "target/%s"

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]

  :javac-options ["-d" "classes/" "-source" "1.8" "-target" "1.8"]
  :uberjar-exclusions
  [#"(org|com|gnu)[/](bukkit|avaje|yaml|getspout|json|trove)[/](.*)"
   #"com[/]google[/]common[/](.*)"
   #"org[/]apache[/]commons[/](.*)"
   #"javax[/]persistence[/](.*)"
   #"net[/]sf[/]cglib[/](.*)"]

  :profiles {:uberjar {:aot :all}}

  )
