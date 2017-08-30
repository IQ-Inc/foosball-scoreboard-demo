(ns foosball-score.players
  "Foosball player display"
  (:require
    [reagent.core :as reagent :refer [atom]]
    [foosball-score.util :refer [teams colors]]))

(defonce players (atom []))

(defonce max-players 4)

(defn- append-player
  [players player]
    (if (< (count players) max-players)
      (conj players player)
      players))

(defn- next-team
  []
  (if (even? (count @players))
    :black
    :gold))

(defn add-player!
  "Add a player"
  [player]
  (let [team (next-team)]
    (swap! players append-player {:player player :team team})))

(defn reset-players!
  "Reset the player list"
  []
  (reset! players []))

(defn- player-color
  [{:keys [team]}]
  (team colors))

;; Components
(defn player-list
  "The player list component"
  []
  [:div.playerlist
    (let [sorted-players (sort-by (comp name :team) @players)]
      (for [player sorted-players] ^{:key player}
        [:div.player 
          [:div {:style {:color (player-color player) }} (player :player)]
          [:img.avatar {:src "/img/avatar-face-icon.png"}]
        ]))])