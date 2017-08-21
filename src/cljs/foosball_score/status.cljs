(ns foosball-score.status
  "Defines the status messages and the status component"
  {:author "Ian McIntyre"}
  
  (:require-macros [foosball-score.util :refer [const]])
  (:require
    [reagent.core :as reagent :refer [atom]]
    [foosball-score.game :refer [colors]]
    [clojure.string :as string]))

;; --------------------------------
;; Atoms
(defonce status
  (atom :waiting))

;; --------------------------------
;; Status message implementations

(defn- possible-team-messages
  "Puts a team name somewhere in a message"
  [team]
  [(str team " brought out messi for that goal")
   (str team " scores a goal")
   (str team " just destroyed that goalie")
   (str team " can't stop, won't stop")
   (str "****, " team ", that was clutch")
   (str "*" team " player takes off shirt and runs a lap*")
   "your parents would be so proud of that goal"
   "gggooooooaaaaalllll"])

(defn- pick-team-message
  "Choose a message for the team"
  [team]
  (let [n (name team)]
    (rand-nth (possible-team-messages n))))

(def status-messages
  (hash-map 
    :waiting (const "waiting for ball drop...")
    :playing (const "playing")
    :game-over (const "game over")
    :yellow pick-team-message
    :black pick-team-message))

;; --------------------------------
;; Functions

(defn- get-status
  "Get the current status message"
  []
  (let [stat @status
        msg-fn (stat status-messages)]
    (string/upper-case (msg-fn stat))))

(defn change-status
  "Change the status to s"
  [stat]
  (if (contains? status-messages stat)
    (reset! status stat)))

(defn- status-style
  "Get the corresponding status style"
  []
  (let [stat @status]
    (hash-map :color (stat colors))))

(defn status?
  "Get the status"
  []
  (let [stat @status]
    stat))

;; --------------------------------
;; Components

(defn status-msg
  "The status message component"
  []
  [:div.scoreboard.status {:style (status-style)}
    [:p (get-status)]])