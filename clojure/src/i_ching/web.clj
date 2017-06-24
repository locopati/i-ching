(ns i-ching.web)
(use '[cheshire.core :only (parse-stream)])

(def hexagrams
  "Store the 64 hexagram info in a local cache"
  (atom {}))

(defn init []
  (with-open [rdr (parse-stream (clojure.java.io/reader "resources/hexagram.json") true)]
    (swap! hexagrams #(zipmap (map :hexagram-binary %) %) rdr)
    (atom (zipmap (map :hexagram-binary rdr) rdr))))

(defn app [handler]
  (println "the app is running")
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello World - How are you?"})
