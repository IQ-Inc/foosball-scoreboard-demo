(ns ^:figwheel-no-load foosball-score.dev
  (:require
    [foosball-score.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
