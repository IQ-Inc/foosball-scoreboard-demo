(ns foosball-score.state
  "State of all the things"
  {:author "Ian McIntyre"}
  (:require
    #?(:cljs [reagent.core :refer [atom]])
    [clojure.data :refer [diff]]))

;; Application state
;; components are defined below.
(def state (atom (hash-map)))

(defmacro defstate
  "Binds a starting state to a var, and adds it into the state"
  [name starting]
  `(do
    (swap! state merge ~starting)
    (defonce ~name ~starting)))

(defn update-state!
  "Replaces the current state with new-state"
  [new-state]
  (reset! state new-state))

;;;;;;;;;;;;;
;; Game state
;;;;;;;;;;;;;

(defstate new-game-state
  {:status :waiting
   :game-mode :first-to-max})

;;;;;;;;;;;;;;
;; Score state
;;;;;;;;;;;;;;

(defstate new-score-state
  {:scores {:black 0 :gold 0}
   :max-score 5})

;;;;;;;;;;;;;
;; Team state
;;;;;;;;;;;;;

(defstate new-team-state
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

;;;;;;;;;;;;
;; New state
;;;;;;;;;;;;
(def new-state
  (reduce merge {} [new-game-state
                    new-team-state
                    new-score-state]))

;;;;;;;;;;;;;;;;;;
;; State consumers
;;;;;;;;;;;;;;;;;;

(defn game-over?
  "Based on the provided state, returns true if the game is
  over, else false."
  [{:keys [game-mode scores max-score] :as state}]
  (case game-mode
    :first-to-max (or (>= (:gold scores) max-score)
                      (>= (:black scores) max-score))
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

(defn event->state
  "Updates the provided state given an event"
  [state event]
  (if (game-over? state) state
    (case event
      :drop (change-status state :playing)
      (:black :gold) (let [state (point-for state event)]
                       (if (game-over? state)
                         (change-status state :game-over)
                         (change-status state event)))
      (add-player state event))))