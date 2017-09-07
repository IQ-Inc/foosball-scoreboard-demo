(ns foosball-score.players
  "Foosball player display"
  (:require
    [reagent.core :as reagent :refer [atom]]
    [foosball-score.util :refer [teams colors]]))

;;;;;;;;;;
;; Avatars
;;;;;;;;;;

;; Avatar icon names will follow the convention
;;    avatar-face-icon-#.png
;; where # is a number starting at 1 and a sequential increment from
;; the previous avatar icon name. When you add a new avatar, increment
;; number-of-avatars appropriately.
(def all-avatar-imgs
  (let [number-of-avatars 4]  ;; Change me if there are more avatars
    (take number-of-avatars
      (map #(str "/img/avatar-face-icon-" % ".png")
        (iterate inc 1)))))

(defonce avatars
  (atom (shuffle all-avatar-imgs)))

(defn- reset-avatars!
  "Reset the available avatars randomly, and store them in the atom."
  []
  (reset! avatars (shuffle all-avatar-imgs)))

(defn- pop-random-avatar!
  "Returns the next available avatar, or nil if they're all gone"
  []
  (let [avatar (first @avatars)]
    (swap! avatars rest)
    avatar))

;;;;;;;;;;;;;;;
;; Players n'at
;;;;;;;;;;;;;;;

(defonce players (atom []))
(defonce max-players 4)

(defn- append-player
  "Append player to players if there are fewer than max-players"
  [players player]
    (if (< (count players) max-players)
      (conj players player)
      players))

(defn- next-team
  "Get the team that will receive the next player.
  Players are first assigned to black, then to gold, then black, then gold."
  []
  (if (even? (count @players))
    :black
    :gold))

(defn add-player!
  "Add a player"
  [player]
  (let [team    (next-team)
        avatar  (pop-random-avatar!)]
    (swap! players append-player {:player player :team team
                                  :avatar avatar})))

(defn reset-players!
  "Reset the player list"
  []
  (do
    (reset! players [])
    (reset-avatars!)))

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
          [:img.avatar {:src (player :avatar)}]
        ]))])