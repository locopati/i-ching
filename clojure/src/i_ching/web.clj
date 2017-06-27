(ns i-ching.web
  (:require [cheshire.core :as cheshire]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.coercions :as coerce]))

(def hexagrams
  "Store the 64 hexagrams info in a local cache as a sequence of maps. 
  We do the loading here rather than in an init function so that it loads everytime this file changes."
  (atom (cheshire/parse-string (slurp "resources/hexagrams.json") true)))

(defn hexagram-by
  "Get the hexagram (a map) from the cache whose key (a keyword) has val"
  [key val]
  (first (filter #(= (key %) val) @hexagrams)))

(defroutes app
  "This is the main ring handler for the application."
  (GET "/hex/:num" [num :<< coerce/as-int]
       {:body (cheshire/generate-string (hexagram-by :king-wen-number num))})
  (GET "/" []  "Hello World")
  (GET "/favicon.ico" [] "")
 )
