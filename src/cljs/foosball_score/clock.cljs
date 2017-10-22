(ns foosball-score.clock
  "Defines the state and actions for the game clock"
  {:author "Ian McIntyre"}
  (:require
    [goog.string :as gstring]
    [goog.string.format]))

(defonce max-game-time (+ (* 60 99) 59))

;; --------------------------------
;; Functions

(defn state-depends
  [state]
  (select-keys state [:time :status]))

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
  [status time]
  (if (and (some #{status} [:black :gold :game-over]) (> time 0))
    "blink"))

;; --------------------------------
;; Components
(defn game-clock
  "The game clock"
  [{:keys [status time] :as state} new-game-callback]
  [:div.gameclock.scoreboard {:class (game-clock-class status time)
                              :on-click new-game-callback}
    [:h2 (game-time-str time)]])
  