(ns foosball-score.players
  "Foosball player display"
  (:require
    [reagent.core :as reagent :refer [atom]]
    [foosball-score.util :refer [teams colors]]
    [foosball-score.state :refer [swap-players]]
    [goog.string :as gstring]))

;;;;;;;;;;;;;;;
;; Players n'at
;;;;;;;;;;;;;;;

(defn- position-icon
  "Returns a font awesome icon based on the player's tactical position"
  [pos color]
  (let [icon (if (= pos :defense) "fa fa-shield" "fa fa-bolt")]
    [:i {:class icon}]))

(defn- player
  [team position players]
  (let [display (if (position players) (position players) "???")]
  [:div
    [:div [position-icon position (team colors)] (gstring/unescapeEntities " &middot; ") display]]))

(defn- team-player-list
  "The player list component"
  [team players swapper]
  (let [[offense defense] [(:offense players) (:defense players)]]
    [:div.player {:style {:color (team colors)}}
      [player team :offense players]
      [:i {:class "fa fa-refresh" :on-click swapper}]
      [player team :defense players]]))

(defn player-list
  [{:keys [teams] :as state} notify]
  [:div.playerlist
    [team-player-list :black (:black teams) (partial notify (swap-players state :black))]
    [team-player-list :gold (:gold teams) (partial notify (swap-players state :gold))]])