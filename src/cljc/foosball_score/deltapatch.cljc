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
            (reduce (fn [m [k v]]
                      (cond
                        (map? v) (assoc m k (delta (k from) v))
                        :else (assoc m k v)))
                    {} (select-keys to (keys from))))))

(defn patch
  "Applies the delta d to the map m"
  [m d]
  (merge m d))