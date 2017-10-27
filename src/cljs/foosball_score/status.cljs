(ns foosball-score.status
  "Defines the status messages and the status component"
  {:author "Ian McIntyre"}
  (:require-macros [foosball-score.util :refer [const]])
  (:require
    [foosball-score.state :refer [game-over?]]
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
   "gggooooooaaaaalllll"
   (str "Team " team " is bringing chaos to this match!")])

(defn- pick-team-message
  "Choose a message for the team"
  [team]
  (let [n (name team)]
    (rand-nth (possible-team-messages n))))

(def status-messages
  (hash-map 
    :waiting (const "waiting for ball drop...")
    :playing (const "playing")
    :gold pick-team-message
    :black pick-team-message))

;; --------------------------------
;; Functions

(defn- get-status
  "Get the current status message"
  [{:keys [status] :as state}]
  (if (game-over? state) "GAME OVER"
    (let [msg-fn (status status-messages)]
      (string/upper-case (msg-fn status)))))

(defn- status-style
  "Get the corresponding status style"
  [stat]
  (hash-map :color (stat colors)))

;; --------------------------------
;; Components

(defn status-msg
  "The status message component"
  [{:keys [status] :as state}]
  [:div.scoreboard.status {:style (status-style status)}
    [:p (get-status state)]])