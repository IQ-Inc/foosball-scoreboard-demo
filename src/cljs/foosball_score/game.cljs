(ns foosball-score.game
  "Defines the game scoring mechanics and the scoreboard behavior"
  {:author "Ian McIntyre"}
  
  (:require 
    [reagent.core :as reagent :refer [atom]]
    [clojure.string :as string]
    [foosball-score.util :refer [teams colors]]
    [foosball-score.state :refer [state]]))

;; --------------------------------
;; Atoms and constants

(defonce scores
  (atom (zipmap teams (cycle [0]))))

(defonce score-times
  (atom '[]))

(defonce max-score 5)

;; --------------------------------
;; Functions
(defn new-game
  "Start a new game"
  []
  (reset! scores
    (zipmap teams (cycle [0])))
  (reset! score-times []))

(defn game-over?
  "Returns true if the game is over, else false"
  []
  (let [ss @scores]
    (boolean (some #(>= % max-score) (vals ss)))))

(defn point-for
  "Increment a point for one of the teams"
  [team time]
  (if (and (some #(= team %) teams) (not (game-over?)))  
    (do 
      (swap! scores update-in [team] inc)
      (swap! score-times conj {:team team :time time}))))

(defn- scorecard-class
  "Change the scorecards class"
  [score]
  (if (>= score max-score)
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
  (let [ss @scores
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