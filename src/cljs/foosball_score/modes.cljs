(ns foosball-score.modes
  "Game mode configuration UI"
  {:author "Ian McIntyre"}
  (:require
    [foosball-score.clock :refer [game-time-str]]))

(def mode->str
  {:win-by-two    (fn [_] "Win by two")
   :first-to-max  (fn [n] (str "First to " n))
   :timed         (fn [_] "Timed game")})

;;;;;;;;;;;;;
;; Components
;;;;;;;;;;;;;

(defmulti left-mode-display
  (fn [game-mode max-score end-time] game-mode))

(defmethod left-mode-display :default
  [_ max-score _]
  [:div (str "Max score: " max-score)])

(defmethod left-mode-display :timed
  [_ _ end-time]
  [:div (str "End time: " (game-time-str end-time))])

(defn- right-mode-display
  "Show the game mode"
  [game-mode max-score]
  [:div ((mode->str game-mode) max-score)])

(defn game-modes
  [{:keys [game-mode end-time] {:keys [max-score]} :scores}]
  [:div.game-modes
    [left-mode-display game-mode max-score end-time]
    [right-mode-display game-mode max-score]])