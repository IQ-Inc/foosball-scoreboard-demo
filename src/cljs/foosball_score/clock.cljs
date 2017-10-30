(ns foosball-score.clock
  "Defines the state and actions for the game clock"
  {:author "Ian McIntyre"}
  (:require
    [foosball-score.state :refer [game-over?]]
    [goog.string :as gstring]
    [goog.string.format]))

(defonce max-game-time (+ (* 60 99) 59))

;; --------------------------------
;; Functions

(defn game-time-str
  "Show the game time as minutes and seconds (example: 72:55)"
  [t]
  (let [mins (int (/ t 60))
        secs (mod t 60)]
    (str 
      (gstring/format "%02d" mins)
      ":"
      (gstring/format "%02d" secs))))

(defn- game-clock-class
  "Apply a class to the game clock"
  [{:keys [status time] :as state}]
  (if (or (game-over? state)
          (and (some #{status} [:black :gold])
               (> time 0)))
    "blink"))

;; --------------------------------
;; Components
(defn game-clock
  "The game clock"
  [{:keys [status time] :as state} new-game-callback]
  [:div.gameclock.scoreboard {:class (game-clock-class state)
                              :on-click new-game-callback}
    [:h2 (game-time-str time)]])
  