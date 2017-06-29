(defproject i-ching "0.1.0-SNAPSHOT"
  :description "I-Ching"
  :url "https://github.com/AndyKriger/i-ching/tree/master"
  :plugins [[cider/cider-nrepl "0.15.0-SNAPSHOT"]
            [lein-ring "0.12.0"]
            [lein-expectations "0.0.8"]
            [lein-autoexpect "1.9.0"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.trace "0.7.9"]
                 [cheshire "5.7.1"]
                 [compojure "1.6.0" :exclusions [ring/ring-core commons-fileupload]]]
  :profiles {:dev {:dependencies [[ring "1.6.0"] ;; this is declared here otherwise eval breaks in emacs
                                  [expectations "2.2.0-beta1"]]}}
  :ring {:handler i-ching.web/app}
  )
