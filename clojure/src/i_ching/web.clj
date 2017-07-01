(ns i-ching.web
  (:require [i-ching.core :as i-ching]
            [cheshire.core :as cheshire]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.coercions :as coerce]
            [ring.util.response :as response]
            [hiccup.core :refer :all]
            [hiccup.page :as page]))

(def hexagrams
  "Store the 64 hexagrams info in a local cache as a sequence of maps. 
  We do the loading here rather than in an init function so that it loads everytime this file changes."
  (atom (cheshire/parse-string (slurp "resources/hexagrams.json") true)))

(defn hexagram-by
  "Get the hexagram (a map) from the cache whose key (a keyword) has val"
  [key val]
  (first (filter #(= (key %) val) @hexagrams)))

(defn verse-commentary-view
  "Display verse and commentary"
  [verse-commentary]
  [:div
   [:div.verse (:verse verse-commentary)]
   [:div.commentary (:commentary verse-commentary)]])

(defn section-view
  "Display a section that is composed of one or more verse & commentary"
  ([section-name hexagram]
   (section-view section-name hexagram nil))
  ([section-name hexagram changing-lines]
   [:div {:class section-name}
    [:div.heading section-name]
    (if (seq changing-lines)
      (->> changing-lines ;; the lines section is a vector of verse-commentaries
           (map #(nth (hexagram (keyword section-name)) %))
           (map verse-commentary-view))
      (verse-commentary-view (hexagram (keyword section-name))))]))

(defn hexagram-view
  "Display a single hexagram"
  ([hexagram]
   (hexagram-view hexagram nil))
  
  ([hexagram changing-lines]
   [:div.hexagram-container
    [:div.hexagram-header
     [:div.ideogram (:chinese hexagram)]
     [:div.hexagram (:hexagram hexagram)]
     [:div.pinyin (:pinyin hexagram)]
     ]
    [:div.name (get-in hexagram [:name :wilhelm])]
    [:div.description
     [:div.heading "description"]
     [:div (:description hexagram)]]
    (section-view "image" hexagram)
    (section-view "judgment" hexagram)
    (if (seq changing-lines) (section-view "lines" hexagram changing-lines))]))

(defn consult-view
  "Display an I Ching reading"
  []
  (let [hexagrams (i-ching/hexagram)
        changing-lines (i-ching/changing-lines hexagrams)
        [primary-hexagram related-hexagram]
        (map #(hexagram-by :hexagram-binary %) hexagrams)]
    (html
     (page/include-css "i-ching.css")
     [:div#consult-container
      (hexagram-view primary-hexagram changing-lines)
      (if (seq changing-lines) (hexagram-view related-hexagram))])))

(defroutes app
  "This is the main ring handler for the application."
  (GET "/hex/:num" [num :<< coerce/as-int]
       {:body (cheshire/generate-string (hexagram-by :king-wen-number num))})
  (GET "/consult" [] (consult-view))
  (GET "/" [] (response/redirect "/consult"))
  (route/resources "/")
  (route/not-found "Page not found"))
