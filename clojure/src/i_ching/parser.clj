(ns i-ching.parser
  (:require [cheshire.core :as cheshire]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(def TRIGRAMS
  "Used to translate trigram names to sequences of yang (1) and yin (0)"
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

(def HEXAGRAMS
  "Used to lookup Unicode hexagram character"
  {1 "䷀" 2 "䷁" 3 "䷂" 4 "䷃" 5 "䷄" 6 "䷅" 7 "䷆" 8 "䷇" 
   9 "䷈" 10 "䷉" 11 "䷊" 12 "䷋" 13 "䷌" 14 "䷍" 15 "䷎" 16 "䷏" 
   17 "䷐" 18 "䷑" 19 "䷒" 20 "䷓" 21 "䷔" 22 "䷕" 23 "䷖" 24 "䷗" 
   25 "䷘" 26 "䷙" 27 "䷚" 28 "䷛" 29 "䷜" 30 "䷝" 31 "䷞" 32 "䷟" 
   33 "䷠" 34 "䷡" 35 "䷢" 36 "䷣" 37 "䷤" 38 "䷥" 39 "䷦" 40 "䷧" 
   41 "䷨" 42 "䷩" 43 "䷪" 44 "䷫" 45 "䷬" 46 "䷭" 47 "䷮" 48 "䷯" 
   49 "䷰" 50 "䷱" 51 "䷲" 52 "䷳" 53 "䷴" 54 "䷵" 55 "䷶" 56 "䷷" 
   57 "䷸" 58 "䷹" 59 "䷺" 60 "䷻" 61 "䷼" 62 "䷽" 63 "䷾" 64 "䷿"})

(def CHINESE
  "Used to lookup the Chinese character and Pinyin transliteration"
  {1 "乾 (qián)" 2 "坤 (kūn)" 3 "屯 (zhūn)" 4 "蒙 (méng)"
   5 "需 (xū)" 6 "訟 (sòng)" 7 "師 (shī)" 8 "比 (bǐ)"
   9 "小畜 (xiǎo chù)" 10 "履 (lǚ)" 11 "泰 (tài)" 12 "否 (pǐ)"
   13 "同人 (tóng rén)" 14 "大有 (dà yǒu)" 15 "謙 (qiān)" 16 "豫 (yù)"
   17 "隨 (suí)" 18 "蠱 (gǔ)" 19 "臨 (lín)" 20 "觀 (guān)"
   21 "噬嗑 (shì kè)" 22 "賁 (bì)" 23 "剝 (bō)" 24 "復 (fù)"
   25 "無妄 (wú wàng)" 26 "大畜 (dà chù)" 27 "頤 (yí)" 28 "大過 (dà guò)"
   29 "坎 (kǎn)" 30 "離 (lí)" 31 "咸 (xián)" 32 "恆 (héng)"
   33 "遯 (dùn)" 34 "大壯 (dà zhuàng)" 35 "晉 (jìn)" 36 "明夷 (míng yí)"
   37 "家人 (jiā rén)" 38 "睽 (kuí)" 39 "蹇 (jiǎn)" 40 "解 (xiè)"
   41 "損 (sǔn)" 42 "益 (yì)" 43 "夬 (guài)" 44 "姤 (gòu)"
   45 "萃 (cuì)" 46 "升 (shēng)" 47 "困 (kùn)" 48 "井 (jǐng)"
   49 "革 (gé)" 50 "鼎 (dǐng)" 51 "震 (zhèn)" 52 "艮 (gèn)"
   53 "漸 (jiàn)" 54 "歸妹 (guī mèi)" 55 "豐 (fēng)" 56 "旅 (lǚ)"
   57 "巽 (xùn)" 58 "兌 (duì)" 59 "渙 (huàn)" 60 "節 (jié)"
   61 "中孚 (zhōng fú)" 62 "小過 (xiǎo guò)" 63 "既濟 (jì jì)" 64 "未濟 (wèi jì)"})

(defn verse-commentary-lookup [section-symbol verse-or-commentary hexagram]
  "Isolates the lookup logic used by get-in/update-in the verse-commentary-handler. 
For judgement and image, this is straightfowrad because each is a map of verse and commentary. 
For lines, this is complicated by the lines being an array of six maps of verse and commentary"
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

(defn verse-commentary-handler
  "Judgement, image, and lines all have a similar pattern of verse then commentary. The lines
section is more complicated because there are six verse/commentaries, one for each changing line."
  ([section-symbol next-section-symbol]
   (fn [match hexagram]
     (cond
      (= match "")
      [section-symbol hexagram]

      (str/includes? match ".html#index")
      [:do-nothing nil]

      (str/starts-with? (str/trim match) (str/upper-case (str "THE " (name next-section-symbol))))
      [next-section-symbol hexagram]

      (or
       (re-find #"^\s+.*(?:Six|Nine).*(?:beginning|second|third|fourth|fifth|top)" match)
       (and (nil? (get-in hexagram (verse-commentary-lookup section-symbol :commentary hexagram)))
            (re-find #"^\s+" match)))
      [section-symbol
       (update-in hexagram
                  (verse-commentary-lookup section-symbol :verse hexagram)
                  (fn [old new] (str old new))
                  (str (str/trim match) "\n"))]

      :else
      [section-symbol
       (update-in hexagram
                  (verse-commentary-lookup section-symbol :commentary hexagram)
                  (fn [old new] (str old new))
                  (str (str/trim match) " "))]))))

(def PRETTY-STATE-MACHINE
  "Define the state transitions for parsing i-ching.html. Each transition is a regex for testing
the current line of text and a handler for how to respond to a match or failure to match. The handler
returns the new state and new hexagram."
  {:do-nothing {:regex #"^((?!name=\"\d{1,2}\").)*$"
                :handler (fn [match hexagram] (if match
                                                [:do-nothing hexagram]
                                                [:new-hexagram nil]))}
   :new-hexagram {:regex #"^\s+(\d{1,2})\.\s(.*)\s/\s(.*)"
                  :handler (fn [match hexagram] (if match ;; this is the initial hexagram map
                                                  [:new-hexagram
                                                   {:king-wen-number (Integer. (match 1))
                                                    :hexagram (HEXAGRAMS (Integer. (match 1)))
                                                    :chinese
                                                    (first (str/split (CHINESE (Integer. (match 1))) #" "))
                                                    :pinyin
                                                    (last (str/split (CHINESE (Integer. (match 1))) #"[()]"))
                                                    :wade-giles (str/lower-case (match 2))
                                                    :name {:wilhelm (match 3)}
                                                    :lines []}]
                                                  [:trigram hexagram]))}
   :trigram {:regex #"\s+(?:above|below).*,\s(.*)"
             :handler (fn [match hexagram] (if match
                                             [:trigram
                                              (merge
                                               hexagram
                                               {:hexagram-binary
                                                ;; for trigram lookup it is necessary to trim
                                                ;; a line may have extra space at the end 
                                                (into [] (concat (TRIGRAMS (str/trim (match 1)))
                                                                 (:hexagram-binary hexagram)))})]
                                             [:description hexagram]))}
   :description {:regex #".*"
                 :handler (fn [match hexagram] (case (str/trim match)
                                                 "" [:description hexagram]
                                                 "THE JUDGMENT" [:judgment hexagram]
                                                 [:description
                                                  (merge hexagram
                                                         {:description
                                                          (str (:description hexagram)
                                                               (str/trim match)
                                                               " ")})]))}
   :judgment {:regex #".*"
              :handler (verse-commentary-handler :judgment :image)}
   :image {:regex #".*"
           :handler (verse-commentary-handler :image :lines)}
   :lines {:regex #".*"
           :handler (verse-commentary-handler :lines :lines)}
   })                   

(defn parse-wilhelm
  "Parse the Wilhelm HTML file. Returns an accumulator object containing the current state,
the current hexagrams, and a sorted map of hexagram numbers to hexagrams."
  []
  (with-open [rdr (io/reader "resources/i-ching.html" :encoding "windows-1252")]
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

(defn wilhelm-results
  "Extract the results from the accumulator returned by parse-wilhelm"
  []
  (vals (dissoc (:hexagrams (parse-wilhelm)) nil)))

(defn emit-json-file []
  "Convenience method to output our map of hexgram info to JSON."
  (with-open [wrt (io/writer "resources/hexagrams.json" :encoding "UTF-8")]
    (cheshire/generate-stream (wilhelm-results) wrt)))
