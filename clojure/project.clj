(defproject i-ching "0.1.0-SNAPSHOT"
  :description "I-Ching"
  :url "https://github.com/AndyKriger/i-ching/tree/master"
  :plugins [[cider/cider-nrepl "0.15.0-SNAPSHOT"]
            [lein-ring "0.12.0"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.trace "0.7.9"]
                 [cheshire "5.7.1"]
                 ;;[ring/ring-core "1.5.0"]
                 ;;[ring/ring-jetty-adapter "1.5.0"]
                 [compojure "1.6.0" :exclusions [ring/ring-core commons-fileupload]]]
  :ring {:handler i-ching.web/app
         :init i-ching.web/init}
  :main i-ching.core)
