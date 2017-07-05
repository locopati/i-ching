(ns i-ching.data
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.labels :as nl]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [clojurewerkz.neocons.rest.batch :as nb]))

(defn convert-maps
  "Convenience function to convert maps to flattened arrays for Neo4J."
  [hexagram]
  (-> hexagram
      (update :image #(vals %))
      (update :judgment #(vals %))
      (update :lines #(flatten (map vals %)))))

(defn open-connection
  []
  (nr/connect "http://localhost:7474/db/data" "neo4j" "neo4j"))

(defn create-trigrams
  "Create the trigrams in the database"
  [trigrams]
  (let [conn (open-connection)
        nodes (doall (nn/create-batch conn trigrams))]
    (map #(nl/add conn % ["Trigram"]) nodes)))

(defn create-hexagrams
  "Create the hexagrams in the database"
  [hexagrams]
  (let [conn (open-connection)
        nodes (doall (nn/create-batch conn (map convert-maps hexagrams)))]
    (map #(nl/add conn % ["Hexagram"]) nodes)))

(defn binary-uri-map
  "Return a map of the node's binary property to the node's relative uri."
  [label]
  (let [conn (open-connection)]
    (into {} (:data (cy/query conn
                              "match (n) where {label} in labels(n) return n.binary, '/node/'+id(n)"
                              {:label label})))))

(defn trigrams-in-hexagram
  "Return a list of the trigrams in a hexagram."
  [hexagram-binary]
  [hexagram-binary (map #(subvec hexagram-binary % (+ % 3)) (range 4))])

(defn trigram-position-name
  "Return the name of a trigram based on its index"
  [index]
  (case index
      0 :lower
      1 :inner
      2 :outer
      3 :upper))

(defn batch-relationship-map
  "Return a map for creating relationships on a hexagram in a batch request."
  [[hexagram-uri {:as trigram-uris}]]
  (map (fn [[name uri]] {:method "POST"
                         :to (str hexagram-uri "/relationships")
                         :body {:to uri
                                :type "CONTAINS"
                                :data {:type name}}})
       trigram-uris))

(defn hexagram-trigram-relationship
  "Create relationships between hexagrams and trigrams"
  []
  (let [conn (open-connection)
        trigram-lookup (binary-uri-map "Trigram")
        hexagram-lookup (binary-uri-map "Hexagram")]
    (->> hexagram-lookup
         keys
         (map trigrams-in-hexagram)
         (map (fn [[hex tri-list]] [(hexagram-lookup hex)
                                   (flatten
                                    (map-indexed
                                     (fn [idx tri] [(trigram-position-name idx)
                                                    (trigram-lookup tri)])
                                     tri-list))]))
         (map batch-relationship-map)
         (nb/perform conn))))
