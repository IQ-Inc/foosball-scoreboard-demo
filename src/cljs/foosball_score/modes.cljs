(ns foosball-score.modes
  "Game mode configuration UI"
  {:author "Ian McIntyre"}
  (:require
    [foosball-score.clock :refer [game-time-str]]
    [foosball-score.state :as state]
    [foosball-score.colors :as colors]))

(def mode->str
  {:win-by-two    (fn [_] "Win by two")
   :first-to-max  (fn [n] (str "First to " n))
   :timed         (fn [_] "Timed game")
   :timed-ot      (fn [_] "Timed game (overtime)")})

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
  [:div (str "Duration: " (game-time-str end-time))])

(defmethod left-mode-display :timed-ot
  [game-mode max-score end-time]
  (left-mode-display :timed max-score end-time))

(defn- right-mode-display
  "Show the game mode"
  [game-mode max-score on-click]
  [:div {:on-click on-click}
    ((mode->str game-mode) max-score)])

(defn- game-mode-style
  "Returns a style depending on the state"
  [state]
  (if (state/overtime? state)
    {:style {:background-color colors/overtime-accent}}))

(defn game-modes
  [{:keys [game-mode end-time] {:keys [max-score]} :scores :as state} {:keys [mode up down]}]
  [:div.game-modes (game-mode-style state)
    [:div {:on-click down} "-"]
    [left-mode-display game-mode max-score end-time]
    [:div {:on-click up} "+"]
    [right-mode-display game-mode max-score mode]])