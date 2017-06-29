(ns i-ching.web-test
  (:require [expectations :refer :all]
            [i-ching.web :as web]
            [cheshire.core :as cheshire]))

(defn request [resource web-app & params]
  (web-app {:request-method :get :uri resource :params (first params)}))

(defn request-hexagram [num]
  (cheshire/parse-string (:body (request (str "/hex/" num) web/app)) true))

;; !!! this test is very important !!!
;; it discovered a bug in parser.clj that prevented the proper parsing of hexagram 38
;; in i-ching.html, the image section of that hexagram is titled "THE IMAGE."
;; in all the other hexagrams, the section is title "THE IMAGE" (no period)
;; the parser had been using equality instead of starts-with? to look for the section header
(expect (more-of json
                 
                 11 (count (keys json))
                 #{:king-wen-number :description :hexagram :hexagram-binary
                   :name :chinese :pinyin :wade-giles :image :judgment :lines} (set (keys json))

                 Integer (:king-wen-number json)
                 #"\p{InCJK_Unified_Ideographs}{1,2}" (:chinese json)
                 #"\p{InYijing_Hexagram_Symbols}{1}" (:hexagram json)                 
                 String (:description json)
                 String (:pinyin json)
                 String (:wade-giles json)

                 map? (:name json)
                 #{:wilhelm} (set (keys (:name json)))

                 map? (:judgment json)
                 #{:verse :commentary} (set (keys (:judgment json)))
                 String (get-in json [:judgment :verse])
                 String (get-in json [:judgment :commentary])

                 map? (:image json)
                 #{:verse :commentary} (set (keys (:image json)))
                 String (get-in json [:image :verse])
                 String (get-in json [:image :commentary])
                 
                 vector? (:lines json)
                 6 (count (:lines json))
                 #{:verse :commentary} (set (distinct (flatten (map keys (:lines json)))))

                 vector? (:hexagram-binary json)
                 6 (count (:hexagram-binary json))
                 true (every? #(or (zero? %) (= 1 %)) (:hexagram-binary json)))
        
        (from-each [hexagram (map request-hexagram (range 1 65))] hexagram))

