(ns foosball-score.status
  "Defines the status messages and the status component"
  {:author "Ian McIntyre"}
  (:require-macros [foosball-score.util :refer [const]])
  (:require
    [foosball-score.util :refer [colors]]
    [clojure.string :as string]))

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
    :gold pick-team-message
    :black pick-team-message))

;; --------------------------------
;; Functions

(defn- get-status
  "Get the current status message"
  [stat]
  (let [msg-fn (stat status-messages)]
    (string/upper-case (msg-fn stat))))

(defn- status-style
  "Get the corresponding status style"
  [stat]
  (hash-map :color (stat colors)))

;; --------------------------------
;; Components

(defn status-msg
  "The status message component"
  [stat]
  [:div.scoreboard.status {:style (status-style stat)}
    [:p (get-status stat)]])