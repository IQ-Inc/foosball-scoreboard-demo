(ns foosball-score.game
  "Defines the game scoring mechanics and the scoreboard behavior"
  {:author "Ian McIntyre"}
  
  (:require 
    [reagent.core :as reagent :refer [atom]]
    [clojure.string :as string]))

;; --------------------------------
;; Atoms and constants
(defonce teams
  [:gold :black])

(defonce colors
  (hash-map :gold "#D4AF37" :black "#000000"))

(defonce scores
  (atom (zipmap teams (cycle [0]))))

(defonce max-score 5)

;; --------------------------------
;; Functions
(defn new-game
  "Start a new game"
  []
  (reset! scores
    (zipmap teams (cycle [0]))))

(defn game-over?
  "Returns true if the game is over, else false"
  []
  (let [ss @scores]
    (boolean (some #(>= % max-score) (vals ss)))))

(defn point-for
  "Increment a point for one of the teams"
  [team]
  (if (and (some #(= team %) teams) (not (game-over?)))  
    (swap! scores update-in [team] inc))
    @scores)

(defn- scorecard-class
  "Change the scorecards class"
  [score]
  (if (>= score max-score)
    "blink"))

;; --------------------------------
;; Components

(defn scoreboard-content
  "A team's scoreboard content"
  [team]
  (let [ss @scores
        color (team colors)]
    [:div.scorecard {:class (scorecard-class (team ss))}
      [:h4 {:style {:color color}} (string/upper-case (name team))]
      [:h1 {:style {:color color}} (team ss)]]))

(defn scoreboard
  "Create the game's scoreboard"
  [left right]
  [:div.scoreboard
    [scoreboard-content left]
    [scoreboard-content right]])