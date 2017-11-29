(ns foosball-score.deltapatch
  "Identify deltas and patch a map by a delta"
  {:author "Ian McIntyre"}
  (:require
    [clojure.set :as set]))

(defn delta
  "Returns the delta from from to to for keys relevant to both"
  [from to]
  (into {}
    (filter (fn [[k v]] (not (= (k from) v)))
            (select-keys to (keys from)))))