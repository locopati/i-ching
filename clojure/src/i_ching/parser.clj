(ns i-ching.parser)
(use '[cheshire.core :only (generate-stream)])
(use '[clojure.string :only (trim upper-case includes?)])

;; used for translating trigram names to sequences of yang (1) and yin (0)
(def TRIGRAMS
  {"HEAVEN" [1 1 1],
   "EARTH" [0 0 0],
   "WATER" [0 1 0],
   "FLAME" [1 0 1], ;; both FLAME and FIRE are used interchangably
   "FIRE" [1 0 1],
   "THUNDER" [1 0 0],
   "MOUNTAIN" [0 0 1],
   "LAKE" [1 1 0],
   "WIND" [0 1 1], ;; both WIND and WOOD are used interchangably
   "WOOD" [0 1 1]
   })

;; used for looking up the unicode hexgram character
(def HEXAGRAMS
  {1 "䷀" 2 "䷁" 3 "䷂" 4 "䷃" 5 "䷄" 6 "䷅" 7 "䷆" 8 "䷇" 
   9 "䷈" 10 "䷉" 11 "䷊" 12 "䷋" 13 "䷌" 14 "䷍" 15 "䷎" 16 "䷏" 
   17 "䷐" 18 "䷑" 19 "䷒" 20 "䷓" 21 "䷔" 22 "䷕" 23 "䷖" 24 "䷗" 
   25 "䷘" 26 "䷙" 27 "䷚" 28 "䷛" 29 "䷜" 30 "䷝" 31 "䷞" 32 "䷟" 
   33 "䷠" 34 "䷡" 35 "䷢" 36 "䷣" 37 "䷤" 38 "䷥" 39 "䷦" 40 "䷧" 
   41 "䷨" 42 "䷩" 43 "䷪" 44 "䷫" 45 "䷬" 46 "䷭" 47 "䷮" 48 "䷯" 
   49 "䷰" 50 "䷱" 51 "䷲" 52 "䷳" 53 "䷴" 54 "䷵" 55 "䷶" 56 "䷷" 
   57 "䷸" 58 "䷹" 59 "䷺" 60 "䷻" 61 "䷼" 62 "䷽" 63 "䷾" 64 "䷿"})

;; isolates the lookup logic used by get-in/update-in in the verse-commentary-handler
;; for judgment and image, this is straightforward because each is a map of verse and commentary
;; for lines, this is complicated by it being an array of maps of verse and commentary
(defn verse-commentary-lookup [section-symbol verse-or-commentary hexagram]
  (cond
    (and (= section-symbol :lines)
         (= 0 (count (:lines hexagram))))
    [:lines 0 verse-or-commentary]
    
    (and (= section-symbol :lines)
         (= verse-or-commentary :verse)
         (not (nil? (:commentary (last (:lines hexagram))))))
    [:lines (count (:lines hexagram)) verse-or-commentary]

    (= section-symbol :lines)
    [:lines (dec (count (:lines hexagram))) verse-or-commentary]

    :else
    [section-symbol verse-or-commentary]))

;; the judgment, image, and lines sections all have a similar pattern - verse then commentary
;; the lines section is more complicated because that pattern repeats six times, once for each line
;; so in addition to jumping out of the pattern using the next header, for lines, 
;; we also need to jump back into the pattern when we finish a commentary block
(defn verse-commentary-handler
  ([section-symbol next-section-symbol]
   (fn [match hexagram]
     (cond
      (= match "")
      [vector section-symbol hexagram]

      (includes? match ".html#index")
      [vector :do-nothing nil]

      (= (trim match) (upper-case (str "THE " (name next-section-symbol))))
      [next-section-symbol hexagram]

      (or
       (re-find #"^\s+.*(?:Six|Nine).*(?:beginning|second|third|fourth|fifth|top)" match)
       (and (nil? (get-in hexagram (verse-commentary-lookup section-symbol :commentary hexagram)))
            (re-find #"^\s+" match)))
      [section-symbol
       (update-in hexagram
                  (verse-commentary-lookup section-symbol :verse hexagram)
                  (fn [old new] (str old new))
                  (str (trim match) "\n"))]

      :else
      [section-symbol
       (update-in hexagram
                  (verse-commentary-lookup section-symbol :commentary hexagram)
                  (fn [old new] (str old new))
                  (str (trim match) " "))]))))

;; define the state transitions for parsing i-ching.html
;; each transition is a regex for testing the current line of text
;; and a handler for how to respond to a match or failure to match
;; the handler returns the new state and new hexagram
(def PRETTY-STATE-MACHINE
  {:do-nothing {:regex #"^((?!name=\"\d{1,2}\").)*$"
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
                                                    :english-name (match 3)
                                                    :lines []})
                                                  (vector :trigram hexagram)))}
   :trigram {:regex #"\s+(?:above|below).*,\s(.*)"
             :handler (fn [match hexagram] (if match
                                             (vector
                                              :trigram
                                              (merge
                                               hexagram
                                               {:hexagram-binary
                                                ;; for trigram lookup it is necessary to trim
                                                ;; a line may have extra space at the end 
                                                (into [] (concat (TRIGRAMS (trim (match 1)))
                                                                 (:hexagram-binary hexagram)))}))
                                             (vector :description hexagram)))}
   :description {:regex #".*"
                 :handler (fn [match hexagram] (case (trim match)
                                                 "" (vector :description hexagram)
                                                 "THE JUDGMENT" (vector :judgment hexagram)
                                                 (vector :description
                                                         (merge hexagram
                                                                {:description
                                                                 (str (:description hexagram)
                                                                      (trim match)
                                                                      " ")}))))}
   :judgment {:regex #".*"
              :handler (verse-commentary-handler :judgment :image)}

   :image {:regex #".*"
           :handler (verse-commentary-handler :image :lines)}
   :lines {:regex #".*"
           :handler (verse-commentary-handler :lines :lines)}
   })                   

;; parse the Wilhelm HTML file
;; returns an accumulator object
;; map keys...
;; king-wen-number (integer)
;; hexagram (string)
;; hexagram-binary (array of 6 1s or 0s)
;; pinyin-name (string)
;; english-name (string)
;; description (string)
;; judgment - map of verse (string) and commentary (string)
;; image - map of verse (string) and commentary (string)
;; lines - array of maps of verse (string) and commentary (string)
(defn parse-wilhelm []
  (with-open [rdr (clojure.java.io/reader "resources/i-ching.html" :encoding "windows-1252")]
    (reduce (fn [acc line]
              (let [{:keys [state hexagrams current-hexagram]} acc
                    state-machine (state PRETTY-STATE-MACHINE)
                    line-match (re-matches (:regex state-machine) line)
                    [new-state new-hexagram] ((:handler state-machine) line-match current-hexagram)]
                {:state new-state
                 :current-hexagram new-hexagram
                 :hexagrams (assoc hexagrams (:king-wen-number new-hexagram) new-hexagram)}))
            {:state :do-nothing
             :current-hexagram {}
             :hexagrams (sorted-map)}
            (line-seq rdr))))

;; extract the results from the Wilhelm parsing accumulator
(defn wilhelm-results []
  (vals (dissoc (:hexagrams (parse-wilhelm)) nil)))

;; convenience method to output our map of hexgram info to JSON
(defn emit-json-file []
  (with-open [wrt (clojure.java.io/writer "resources/i-ching.json" :encoding "UTF-8")]
    (generate-stream (wilhelm-results) wrt)))
