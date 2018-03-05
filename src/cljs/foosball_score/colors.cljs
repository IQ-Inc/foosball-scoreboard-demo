(ns foosball-score.colors
  "Team colors and color-based logic"
  {:author "Ian McIntyre"}
  (:require
    [reagent.core :refer [atom]]
    [foosball-score.state :as state]))

(def overtime-accent "#CC0000")

(def colors
  (hash-map
    :gold       "#D4AF37"
    :black      "#000000"))

(def overtime-colors
  (hash-map
    :gold  overtime-accent
    :black overtime-accent
    nil    overtime-accent))

(defn get-colors
  "Returns a hash-map of colors based on the game state"
  [state]
  (if (state/overtime? state)
    overtime-colors
    colors))