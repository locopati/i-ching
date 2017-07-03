;; convert trigrams exported as tsv from spreadsheet to json
(require '[clojure.string :as str])
(require '[cheshire.core :as cheshire])
(def tsv (slurp "resources/trigrams.tsv"))
(def lines (map #(str/split % #"\t") (str/split tsv #"\r\n")))
(def header (map keyword (first lines)))
(def trigrams (map #(update
                     (apply assoc {}  (interleave header %))
                     :binary
                     (fn [b] (vec (map read-string (str/split b #"")))))
                   (rest lines)))
(with-open [wrt (clojure.java.io/writer "resources/trigrams.json")]
  (cheshire/generate-stream trigrams wrt))
