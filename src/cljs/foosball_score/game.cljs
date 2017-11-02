(ns foosball-score.game
  "Defines the scoreboard"
  {:author "Ian McIntyre"}
  (:require
    [clojure.string :as string]
    [foosball-score.clock :refer [game-time-str]]
    [foosball-score.util :refer [teams]]
    [foosball-score.colors :refer [colors]]
    [foosball-score.state :refer [game-over? who-is-winning]]))

;; --------------------------------
;; Functions

(defn state-depends
  "Describes the filtering of the state specific for this component"
  [state]
  (select-keys state [:scores :game-mode :score-times :time :end-time]))

(defn- scorecard-class
  "Change the scorecards class"
  [state team]
  (if (and (game-over? state)
           (= team (who-is-winning state)))
    "blink"))

;; --------------------------------
;; Components

(defn score-time-list
  "Show the time of the most recent 9 scores"
  [all-score-times]
  (let [total (count all-score-times)
        score-times (if (> total 9)
                        (drop (- total 9) all-score-times)
                        all-score-times)]
    [:div.scorelist
      (for [item score-times] ^{:key item}
        (let [time (game-time-str (item :time))
              team (item :team)
              color (team colors)]
          [:div {:style {:color color}} time]))]))

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
    [score-time-list (:score-times state)]
    [scoreboard-content state right :left]])