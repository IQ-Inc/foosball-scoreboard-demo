(ns foosball-score.clock
  "Defines the state and actions for the game clock"
  {:author "Ian McIntyre"}
  
  (:require
    [reagent.core :as reagent :refer [atom]]
    [goog.string :as gstring]
    [goog.string.format]))

;; --------------------------------
;; Atoms and constants
(defonce game-time (atom 0))

(defonce game-interval
  (atom nil))

(defonce max-game-time (+ (* 60 99) 59))

;; --------------------------------
;; Functions
(defn- game-time-str
  "Show the game time as minutes and seconds (example: 72:55)"
  []
  (let [t @game-time
        mins (int (/ t 60))
        secs (mod t 60)]
    (str 
      (gstring/format "%02d" mins)
      "."
      (gstring/format "%02d" secs))))

(defn- game-time-tick
  "Increment the game time by one second"
  []
  (let [t @game-time]
    (if (> max-game-time t)
      (swap! game-time inc)
      t)))

(defn start-game
  "Start the game clock"
  []
  (let [interval @game-interval]
    (if (nil? interval)
      (reset! game-interval (js/setInterval game-time-tick 1000)))))

(defn pause-game
  "Pause the game clock"
  []
  (swap! game-interval js/clearInterval))

(defn new-game
  "Reset the game clock"
  []
  (pause-game)
  (reset! game-time 0))

(defn- paused?
  "Returns true if the clock is paused, else false"
  []
  (let [interval @game-interval]
    (nil? interval)))

(defn- game-clock-class
  "Apply a class to the game clock"
  []
  (let [t @game-time]
    (if (and (paused?) (> t 0))
      "blink")))

;; --------------------------------
;; Components
(defn game-clock
  "The game clock"
  []
  [:div.gameclock.scoreboard {:class (game-clock-class)}
    [:h2 (game-time-str)]])