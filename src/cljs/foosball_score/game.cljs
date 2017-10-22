(ns foosball-score.game
  "Defines the game scoring mechanics and the scoreboard behavior"
  {:author "Ian McIntyre"}
  
  (:require 
    [reagent.core :as reagent :refer [atom]]
    [clojure.string :as string]
    [foosball-score.util :refer [teams colors]]
    [foosball-score.state :refer [game-over? who-is-winning]]))

(defonce score-times
  (atom '[]))

;; --------------------------------
;; Functions
(defn state-depends
  [state]
  (select-keys state [:scores :game-mode]))

(defn new-game
  "Start a new game"
  []
  (reset! score-times []))

(defn- scorecard-class
  "Change the scorecards class"
  [state team]
  (if (and (game-over? state)
           (= team (who-is-winning state)))
    "blink"))

;; --------------------------------
;; Components

(defn score-time-list
  "Show the time of each score"
  []
  [:div.scorelist
    (for [item @score-times] ^{:key item}
      (let [time (item :time)
            team (item :team)
            color (team colors)]
        [:div {:style {:color color}} time]))])

(defn scoreboard-content
  "A team's scoreboard content"
  [state team align]
  (let [color (team colors)
        scores (:scores state)]
    [:div.scorecard {:class (scorecard-class state team)}
      [:h6 {:style {:color color :text-align align}}
           (string/upper-case (name team))]
      [:h1 {:style {:color color :text-align align}} (team scores)]]))

(defn scoreboard
  "Create the game's scoreboard"
  [state left right]
  [:div.scoreboard
    [scoreboard-content state left :right]
    [score-time-list]
    [scoreboard-content state right :left]])