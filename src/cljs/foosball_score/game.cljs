(ns foosball-score.game
  "Defines the game scoring mechanics and the scoreboard behavior"
  {:author "Ian McIntyre"}
  
  (:require 
    [reagent.core :as reagent :refer [atom]]
    [clojure.string :as string]
    [foosball-score.util :refer [teams colors]]
    [foosball-score.state :refer [state]]))

(defonce score-times
  (atom '[]))

;; --------------------------------
;; Functions
(defn new-game
  "Start a new game"
  []
  (reset! score-times []))

(defn- scorecard-class
  "Change the scorecards class"
  [score]
  (if (>= score (:max-score @state))
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
  [team align]
  (let [ss (:scores @state)
        color (team colors)]
    [:div.scorecard {:class (scorecard-class (team ss))}
      [:h6 {:style {:color color :text-align align}} (string/upper-case (name team))]
      [:h1 {:style {:color color :text-align align}} (team ss)]]))

(defn scoreboard
  "Create the game's scoreboard"
  [left right]
  [:div.scoreboard
    [scoreboard-content left :right]
    [score-time-list]
    [scoreboard-content right :left]])