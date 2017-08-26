(ns foosball-score.util
  "Shared utilities and constants"
  {:author "Ian McIntyre"})

(defmacro const
  "Closure that captures x and returns it for any input argument."
  [x]
  `(fn [~(gensym)] ~x))

(defonce ws-url "/chsk")

(defonce serial-msg-sentinal \newline)