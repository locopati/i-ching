(ns i-ching.parser)
(require '[clojure.core.match])

;; used for translating trigram names to sequences of yang (1) and yin (0)
(def TRIGRAMS
  {"HEAVEN" [1 1 1],
   "EARTH" [0 0 0],
   "WATER" [0 1 0],
   "FLAME" [1 0 1],
   "THUNDER" [1 0 0],
   "MOUNTAIN" [0 0 1],
   "LAKE" [1 1 0],
   "WIND" [0 1 1]
   })

(def HEXAGRAMS
  {1 "䷀" 2 "䷁" 3 "䷂" 4 "䷃" 5 "䷄" 6 "䷅" 7 "䷆" 8 "䷇" 
   9 "䷈" 10 "䷉" 11 "䷊" 12 "䷋" 13 "䷌" 14 "䷍" 15 "䷎" 16 "䷏" 
   17 "䷐" 18 "䷑" 19 "䷒" 20 "䷓" 21 "䷔" 22 "䷕" 23 "䷖" 24 "䷗" 
   25 "䷘" 26 "䷙" 27 "䷚" 28 "䷛" 29 "䷜" 30 "䷝" 31 "䷞" 32 "䷟" 
   33 "䷠" 34 "䷡" 35 "䷢" 36 "䷣" 37 "䷤" 38 "䷥" 39 "䷦" 40 "䷧" 
   41 "䷨" 42 "䷩" 43 "䷪" 44 "䷫" 45 "䷬" 46 "䷭" 47 "䷮" 48 "䷯" 
   49 "䷰" 50 "䷱" 51 "䷲" 52 "䷳" 53 "䷴" 54 "䷵" 55 "䷶" 56 "䷷" 
   57 "䷸" 58 "䷹" 59 "䷺" 60 "䷻" 61 "䷼" 62 "䷽" 63 "䷾" 64 "䷿"})

  ;; define the state transitions for parsing i-ching.html
  ;; each transition is a regex for testing the current line of text
  ;; and a handler for how to respond to a match or failure to match
  (def PRETTY-STATE-MACHINE
    {:do-nothing {:regex #"^((?!name=\"1\").)*$"
                  :handler (fn [match hexagram] (if match
                                                  (vector :do-nothing hexagram)
                                                  (vector :new-hexagram nil)))}
     :new-hexagram {:regex #"^\s+(\d{1,2})\.\s(.*)\s/\s(.*)"
                    :handler (fn [match hexagram] (if match
                                                    (vector 
                                                     :new-hexagram
                                                     {:king-wen-number (Integer. (match 1))
                                                      :hexagram (HEXAGRAMS (Integer. (match 1)))
                                                      :pinyin-name (match 2)
                                                      :english-name (match 3)})
                                                    (vector :trigram hexagram)))}
     :trigram {:regex #"\s+(?:above|below).*,\s(.*)"
               :handler (fn [match hexagram] (println match hexagram) (if match
                                                  (vector
                                                   :trigram
                                                   (merge
                                                    hexagram
                                                    {:hexagram-binary
                                                     (apply
                                                      (fnil conj [])
                                                      (:hexagram-binary hexagram) (TRIGRAMS (match 1)))}))
                                                  (vector :do-nothing nil)))}
     })


;; produce a seq of hexagram maps
;; keys...
;; king-wen-number
;; pinyin-name
;; english-name
(defn parse-wilhelm []
  (with-open [rdr (clojure.java.io/reader "resources/i-ching.html")]
    (let [hexagrams (atom (sorted-map))
          state (atom :do-nothing)
          current-hexagram (atom {})]
      (doseq [line (line-seq rdr)]
        (let [;;_ (println "state" @state) ;; for debugging
              sm (@state PRETTY-STATE-MACHINE)
              m (re-matches (:regex sm) line)
              ;;_ (println "match" m) ;; for debugging
              [s new-hexagram] ((:handler sm) m @current-hexagram)]
          (reset! state s)
          (reset! current-hexagram new-hexagram)
          (swap! hexagrams assoc (:king-wen-number new-hexagram) new-hexagram)))
      (vals (dissoc @hexagrams nil)))))










