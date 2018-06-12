(ns foosball-score.util
  "Shared utilities and constants"
  {:author "Ian McIntyre"})

(def teams
  [:gold :black])

(def opposites
  {:black :gold
   :gold :black})

(defmacro const
  "Closure that captures x and returns it for any input argument."
  [x]
  `(fn [~(gensym)] ~x))

(defmacro nilsafe
  "Wraps a function f and makes it safe against nil input"
  [f]
  `(fn [x#]
    (if-not (nil? x) (~f x#))))

(defonce ws-url "/chsk")