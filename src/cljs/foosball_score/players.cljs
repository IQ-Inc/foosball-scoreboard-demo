(ns foosball-score.players
  "Foosball team and player display"
  (:require
    [foosball-score.util :refer [teams colors]]
    [foosball-score.state :refer [swap-players]]
    [goog.string :as gstring]))

;;;;;;;;;;;;;;;
;; Players n'at
;;;;;;;;;;;;;;;

(defn state-depends
  "State filtering for player components"
  [state]
  (select-keys state [:next-player :teams]))

(defn- position-icon
  "Returns a font awesome icon based on the player's tactical position"
  [pos color]
  (let [icon (if (= pos :defense) "fa fa-shield" "fa fa-bolt")]
    [:i {:class icon}]))

(defn- player-display
  "Returns a string representing the player name and w/l counts"
  [{:keys [name stats]}]
  (str (if (nil? name) "UNKNOWN" name) " (" (:wins stats) "-" (:losses stats) ")"))

(defn- player
  "Component that shows the player position icon and name"
  [team position next-player players]
  (let [display-name (if (position players) (player-display (position players)) "???")
        outline (if (= next-player [team position]) "dotted" nil)]
  [:div {:style {:outline outline :margin 5}}
    [:div {:style {:padding 5}}
      [position-icon position (team colors)]
      (gstring/unescapeEntities " &middot; ") display-name]]))

(defn- team-player-list
  "The one to two players on a team"
  [{:keys [teams next-player] :as state} team swapper]
  (let [[offense defense] [(-> teams team :offense) (-> teams team :defense)]]
    [:div.player {:style {:color (team colors)}}
      [player team :offense next-player (team teams)]
      [:i {:class "fa fa-refresh" :on-click swapper}]
      [player team :defense next-player (team teams)]]))

(defn player-list
  "The two to four players in the game"
  [state swapper]
  [:div.playerlist
    [team-player-list state :black (partial swapper :black)]
    [team-player-list state :gold (partial swapper :gold)]])