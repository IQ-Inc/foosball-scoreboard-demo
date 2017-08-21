(ns foosball-score.prod
  (:require [foosball-score.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
