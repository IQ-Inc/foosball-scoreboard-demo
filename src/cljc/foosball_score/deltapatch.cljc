(ns foosball-score.deltapatch
  "Delta and patching for maps"
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
                        (map? v) (let [vs (delta (k from) v)]
                                   (if (not (empty? vs))
                                       (assoc m k vs)
                                       m))
                        :else (assoc m k v)))
                    {} (select-keys to (keys from))))))

(defn patch
  "Applies the delta d to the map m"
  [from d]
  (reduce (fn [n [k v]]
            (cond
              (map? v) (let [vs (patch (k from) v)]
                         (if (not (empty? vs))
                             (assoc n k vs)
                             n))
              :else (assoc n k v)))
          from d))