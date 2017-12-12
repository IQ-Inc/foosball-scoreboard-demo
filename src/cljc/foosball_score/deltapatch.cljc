(ns foosball-score.deltapatch
  "Delta and patching for maps"
  {:author "Ian McIntyre"}
  (:require
    [clojure.set :as set]))

(defn delta
  "Returns the delta from from to to for keys relevant to both"
  [from to]
  (cond
    (nil? from) to
    (nil? to) nil
    :else
      (let [ks   (set/intersection (set (keys from)) (set (keys to)))
            dks  (filter #(not (= (% from) (% to))) ks)]
        (reduce (fn [m k]
                  (if (or (map? (k from)) (map? (k to)))
                      (assoc m k (delta (k from) (k to)))
                      (assoc m k (k to))))
                {} dks))))

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