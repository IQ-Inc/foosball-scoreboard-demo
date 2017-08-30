(ns ^:figwheel-no-load foosball-score.dev
  (:require
    [foosball-score.core :as core]
    [foosball-score.players :as players]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)

(defn four-players
  [])