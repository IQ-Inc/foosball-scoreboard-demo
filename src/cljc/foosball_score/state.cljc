(ns foosball-score.state
  "State of all the things"
  {:author "Ian McIntyre"}
  (:require
    #?(:cljs [reagent.core :refer [atom]])
    [clojure.data :refer [diff]]))

;;;;;;;;;;;;;
;; Game state
;;;;;;;;;;;;;

(def new-game-state
  {:status :waiting
   :game-mode :first-to-max})

;;;;;;;;;;;;;;
;; Score state
;;;;;;;;;;;;;;

(def new-score-state
  {:scores {:black 0 :gold 0 :max-score 5}})

;;;;;;;;;;;;;
;; Team state
;;;;;;;;;;;;;

(def new-team-state
  {:teams {:black {:offense nil :defense nil}
           :gold  {:offense nil :defense nil}}
   :next-player [:black :offense]})

(defn next-player-transition
  "Returns the next player state"
  [next-player]
  (let [transitions [[:black :offense]
                     [:gold  :offense]
                     [:black :defense]
                     [:gold  :defense]]]
    ;; Guard for invaid inputs
    (if (some #{next-player} transitions)
      (->> (cycle transitions)
           (drop-while #(not (= % next-player)))
           next
           first)
      (first transitions))))

;;;;;;;;;;;;;
;; Time state
;;;;;;;;;;;;;

(def new-time-state
  {:time 0 :score-times []})

;;;;;;;;;;;;
;; New state
;;;;;;;;;;;;
(def new-state
  (reduce merge {} [new-game-state
                    new-team-state
                    new-score-state
                    new-time-state]))

;; Application state
;; components are defined below.
(defonce state (atom new-state))

(defn update-state!
  "Replaces the current state with new-state"
  [new-state]
  (reset! state new-state))

;;;;;;;;;;;;;;;;;;
;; State consumers
;;;;;;;;;;;;;;;;;;

(defn who-is-winning
  "Returns the key of the winning team, or nil if the game is tied"
  [{:keys [scores]}]
  (let [[b g] [(:black scores) (:gold scores)]
        d (- b g)]
    (cond
      (< d 0) :gold
      (> d 0) :black
      (= d 0) nil)))

(defn game-over?
  "Based on the provided state, returns true if the game is
  over, else false."
  [{:keys [game-mode scores] :as state}]
  (case game-mode
    :first-to-max (or (>= (:gold scores) (:max-score scores))
                      (>= (:black scores) (:max-score scores)))
    :win-by-two (let [g (:gold scores)
                      b (:black scores)
                      d (- (max b g) (min b g))]
                  (and (>= d 2)
                       (game-over? (assoc state :game-mode :first-to-max))))
    true)) ;; Default, game is over for invalid modes

(defn point-for
  "Returns a state with a point added for team, or the current state if
  it is inappropriate to update the team's score"
  [state team]
  (if (game-over? state) 
      state
      (update-in state [:scores team] inc)))

;; Describe positional associations for the team state
(let [player (fn [which]
               (fn [{:keys [teams]} team]
                 (which (team teams))))]
  (def offense (player :offense))
  (def defense (player :defense)))

(defn swap-players
  "Swap the players on team"
  [state team]
  (let [offense (offense state team)
        defense (defense state team)]
    (assoc-in state [:teams team] {:offense defense :defense offense})))

(defn change-status
  "Change the status of the state"
  [state status]
  (assoc state :status status))

(defn add-player
  "Adds a player to a team, and defines the next player assignment"
  [{:keys [next-player] :as state} player]
  (let [next-next-player (next-player-transition next-player)
        next-state (assoc state :next-player next-next-player)]
    (assoc-in next-state (cons :teams next-player) player)))

(defn- update-score-times
  [{:keys [status time] :as state} team]
  (update state :score-times conj {:time time :team team}))

(defn event->state
  "Updates the provided state given an event. Returns the next state, or nil
  if there is no update."
  [{:keys [status] :as state} event]
  (if (game-over? state) state
    (case event
      :tick (if (= status :playing) (update state :time inc) state)
      :drop (change-status state :playing)
      (:black :gold) (let [state (point-for state event)]
                       (if (game-over? state)
                         (-> state 
                             (#(change-status % :game-over))
                             (update-score-times (who-is-winning state)))
                         (-> state
                             (#(change-status % event))
                             (update-score-times event))))
      (add-player state event))))