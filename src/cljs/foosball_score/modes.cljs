(ns foosball-score.modes
  "Game mode configuration UI"
  {:author "Ian McIntyre"})

(def mode->str
  {:win-by-two    (fn [_] "Win by two")
   :first-to-max  (fn [n] (str "First to " n))})

;;;;;;;;;;;;;
;; Components
;;;;;;;;;;;;;

(defn- max-score-display
  "Show the max score"
  [max-score]
  [:div (str "Max score: " max-score)])

(defn- game-mode-display
  "Show the game mode"
  [game-mode max-score]
  [:div ((mode->str game-mode) max-score)])

(defn game-modes
  [{:keys [game-mode] {:keys [max-score]} :scores}]
  [:div
    [max-score-display max-score]
    [game-mode-display game-mode max-score]])