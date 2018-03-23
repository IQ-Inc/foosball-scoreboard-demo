(ns foosball-score.click
  "Click handlers for buttons and clickable UI elements
  
  The click handlers are defined in terms of keypress handlers."
  {:author "Ian McIntyre"}
  (:require
    [foosball-score.keypress :refer [keypress-handler]]))

(defn change-mode
  "Change the game mode"
  [state]
  (keypress-handler state \m))

(defn increase-setting
  "Increase the game mode setting (score, time, etc)"
  [state]
  (keypress-handler state \k))

(defn decrease-setting
  "Decrease the game mode setting (score, time, etc)"
  [state]
  (keypress-handler state \j))

(defn new-game
  "Start a new game"
  [state]
  (keypress-handler state \space))