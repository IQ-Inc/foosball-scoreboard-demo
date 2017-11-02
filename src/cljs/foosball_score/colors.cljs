(ns foosball-score.colors
  "Team colors and color-based logic"
  {:author "Ian McIntyre"}
  (:require
    [reagent.core :refer [atom]]
    [foosball-score.state :as state]))

(def colors
  (hash-map
    :gold       "#D4AF37"
    :black      "#000000"))

(def overtime-colors
  (hash-map
    :gold  "#CC0000"
    :black "#CC0000"))

(defn get-colors
  "Returns a hash-map of colors based on the game state"
  [state]
  (if (and (state/game-over? state)
           (nil? (state/who-is-winning state)))
    overtime-colors
    colors))