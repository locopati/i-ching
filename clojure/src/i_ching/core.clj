(ns i-ching.core)

(defn gather-sticks [sticks]
  (let [sticks-mod (mod sticks 4)]
    (if (zero? sticks-mod) 4 sticks-mod)))

(defn split-sticks [sticks]
  (let [left-min (+ 5 (int (rand 12))) ;; when we split, we hold onto a minimum number of sticks
        right-min (+ 5 (int (rand 12)))
        left (+ left-min (int (rand (- sticks left-min right-min))))
        ;;left (int (rand sticks))
        right (- sticks 1 left)]
    (+ 1 (gather-sticks left) (gather-sticks right))))

(defn yarrow-sticks
  ([]
   (yarrow-sticks '() 49))
  ([accum sticks]
   (if (= 3 (count accum))
     accum
     (let [split-count (split-sticks sticks)]
       (yarrow-sticks (conj accum split-count) (- sticks split-count))))))

(defn two-or-three [value]
  (case value
    4 3
    5 3
    8 2
    9 2
    (throw (IllegalArgumentException. (str value " is not a valid value for split-sticks")))))

(defn hexagram-line []
  (reduce + (map two-or-three (yarrow-sticks))))

(defn yin-or-yang [value]
  (case value
    6 '(0 1)
    7 '(1 1)
    8 '(0 0)
    9 '(1 0)))

(defn hexagram []
  (->> (repeatedly 6 hexagram-line)
       (map yin-or-yang)
       (apply map vector)))
