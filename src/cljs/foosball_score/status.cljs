(ns foosball-score.status
  "Defines the status messages and the status component"
  {:author "Ian McIntyre"}
  (:require-macros [foosball-score.util :refer [const]])
  (:require
    [foosball-score.state :as state]
    [foosball-score.colors :refer [get-colors]]
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
   "cool"
   "neat"
   "wow, what a shot"
   (str "neat shot, " team)
   (str "Team " team " is bringing chaos to this match!")])

(defn- pick-team-message
  "Choose a message for the team"
  [team]
  (let [teamname (name team)]
    (rand-nth (possible-team-messages teamname))))

(def status-messages
  (hash-map 
    :waiting (const "waiting for ball drop...")
    :playing (const "playing")
    :gold pick-team-message
    :black pick-team-message))

;; --------------------------------
;; Functions

(defn- game-over-msg
  "Generates the game over message based on a winner"
  [winner]
  (let [lookup {nil "TIED GAME" :black "BLACK WINS" :gold "GOLD WINS"}]
    (str "GAME OVER: " (get lookup winner))))

(defn- get-status
  "Get the current status message"
  [{:keys [status] :as state}]
  (cond
    (state/game-over? state) (game-over-msg (state/who-is-winning state))
    (state/overtime? state) "OVERTIME: NEXT GOAL WINS"
    :else (let [msg-fn (status status-messages)]
            (string/upper-case (msg-fn status)))))

(defn- status-style
  "Get the corresponding status style"
  [{:keys [status] :as state}]
  (if (or (state/game-over? state) (state/overtime? state))
    (hash-map :color ((get-colors state) (state/who-is-winning state)))
    (hash-map :color (status (get-colors state)))))

(defn- status-class
  "Generates a map to apply a style class"
  [state]
  (if (state/overtime? state)
    "blink"))

;; --------------------------------
;; Components

(defn status-msg
  "The status message component"
  [{:keys [status] :as state}]
  [:div.scoreboard.status {:style (status-style state) :class (status-class state)}
    [:p (get-status state)]])
