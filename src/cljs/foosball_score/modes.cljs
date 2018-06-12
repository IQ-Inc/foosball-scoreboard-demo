(ns foosball-score.modes
  "Game mode configuration UI"
  {:author "Ian McIntyre"}
  (:require
    [foosball-score.clock :refer [game-time-str]]
    [foosball-score.state :as state]
    [foosball-score.colors :as colors]))

(def mode->str
  {:win-by-two    (fn [_] "Win by two")
   :first-to-max  (fn [{{:keys [max-score]} :scores}] (str "First to " max-score))
   :timed         (fn [_] "Timed game")
   :timed-ot      (fn [_] "Timed game (overtime)")
   :multiball     (fn [{:keys [balls]}] (str "Multiball (" balls ")"))})

;;;;;;;;;;;;;
;; Components
;;;;;;;;;;;;;

(defmulti left-mode-display :game-mode)

(defmethod left-mode-display :timed
  [{:keys [end-time]}]
  [:div (str "Duration: " (game-time-str end-time))])

(defmethod left-mode-display :timed-ot
  [state]
  (left-mode-display (assoc state :game-mode :timed)))

(defmethod left-mode-display :multiball
  [{:keys [max-balls]}]
  [:div (str "Max balls: " max-balls)])

(defmethod left-mode-display :default
  [state]
  [:div (str "Max score: " (get-in state [:scores :max-score]))])

(defn- right-mode-display
  "Show the game mode"
  [{:keys [game-mode] :as state} on-click]
  [:div {:on-click on-click}
    ((mode->str game-mode) state)])

(defn- game-mode-style
  "Returns a style depending on the state"
  [state]
  (if (state/overtime? state)
    {:style {:background-color colors/overtime-accent}}))

(defn game-modes
  [state {:keys [mode up down]}]
  [:div.game-modes (game-mode-style state)
    [:div {:on-click down} "-"]
    [left-mode-display state]
    [:div {:on-click up} "+"]
    [right-mode-display state mode]])