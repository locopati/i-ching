(ns i-ching.core-test
  (:require [expectations :refer :all]
            [i-ching.core :refer :all]))

(defn distinct-results
  "Repeatedly run a function and return a sequence of its distinct results."
  [fn]
  (-> (repeatedly 1000 fn)
      flatten
      sort
      distinct))

;; test that gather-sticks only returns 1,2,3 or 4
(expect [1 2 3 4] (distinct-results #(gather-sticks (inc (rand-int 49)))))

;; test that yarrow-sticks only returns 4,5,8 or 9
(expect 3 (count (yarrow-sticks)))
(expect [4 5 8 9] (distinct-results yarrow-sticks))

;; test that two-or-three only returns 2 or 3
(expect IllegalArgumentException (two-or-three 1))
(expect [2 3] (distinct-results #(two-or-three (rand-nth [4 5 8 9]))))

;; test that hexagram-lines are only 6,7,8 or 9
(expect [6 7 8 9] (distinct-results hexagram-line))

;; test that yarrow-stalk frequencies meet expectations
(let [iter 10000
      freq (frequencies (repeatedly iter hexagram-line))]
  (expect (approximately (/ 1 16) 0.01) (/ (freq 6) iter))
  (expect (approximately (/ 5 16) 0.01) (/ (freq 7) iter))
  (expect (approximately (/ 7 16) 0.01) (/ (freq 8) iter))
  (expect (approximately (/ 3 16) 0.01) (/ (freq 9) iter)))

;; test that yin-or-yang only has certain possible values
(expect [0 1] (yin-or-yang 6)) ;; old yin
(expect [1 1] (yin-or-yang 7)) ;; yang
(expect [0 0] (yin-or-yang 8)) ;; yin
(expect [1 0] (yin-or-yang 9)) ;; old yang

;; test that hexagram produces two hexagrams
(expect 2 (count (hexagram)))
(expect 6 (count (first (hexagram))))
(expect 6 (count (last (hexagram))))
(expect [0 1] (distinct-results hexagram)) ;; hexagrams only contain 0 or 1

;; test changing-lines
(expect [] (changing-lines '([1 1 1 1 1 1] [1 1 1 1 1 1]))) ;; no changes
(expect [0] (changing-lines '([1 1 1 1 1 1] [0 1 1 1 1 1]))) ;; first changes
(expect [1 3] (changing-lines '([1 0 1 0 1 1] [1 1 1 1 1 1]))) ;; middle changes
(expect [4 5] (changing-lines '([1 1 1 1 0 1] [1 1 1 1 1 0]))) ;; last changes
(expect [0 1 2 3 4 5] (changing-lines '([0 0 0 1 1 1] [1 1 1 0 0 0]))) ;; all change


