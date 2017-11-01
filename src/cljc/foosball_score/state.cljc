(ns foosball-score.state
  "State of all the things

  The namespace defines the components representing 'state' and the structure
  of the state. This module is shared across the client and server; it shall
  build in each environment."
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

;;;;;;;;;;;;;
;; Time state
;;;;;;;;;;;;;

(def new-time-state
  {:time 0 :score-times []
   :end-time 120})

;;;;;;;;;;;;
;; New state
;;;;;;;;;;;;

(def new-state
  (merge {} new-game-state
            new-team-state
            new-score-state
            new-time-state))

(defmulti new-game
  "Returns a new game state based on the current state"
  :game-mode)

(defmethod new-game :default
  [state]
  (let [max-score (get-in state [:scores :max-score])
        game-mode (get state :game-mode)
        end-time  (get state :end-time)]
    (-> new-state
        (assoc :game-mode game-mode)
        (assoc-in [:scores :max-score] max-score)
        (assoc :end-time end-time))))

;; Application state
;; components are defined below.
(defonce state (atom new-state))

(defn update-state!
  "Replaces the current state with new-state"
  [new-state]
  (reset! state new-state))

;;;;;;;;;;;;;;;;;;;;;
;; Cyclic transitions
;;;;;;;;;;;;;;;;;;;;;

(defn- next-transition
  "Describes cyclic transitions across a set of possible values"
  [transitions current]
  (if (some #{current} transitions)
      (->> (cycle transitions)
           (drop-while #(not (= % current)))
           next
           first)
      ;; Otherwise, return the first possible transition
      ;; so we can get back into sync
      (first transitions)))

(def next-player-transition
  (partial next-transition [[:black :offense]
                            [:gold  :offense]
                            [:black :defense]
                            [:gold  :defense]]))

(def next-game-mode-transition
  (partial next-transition [:first-to-max :win-by-two :timed]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; State consumers
;;
;; All public functions actions will operate upon the global state.
;; The state will be the first argument of any public function.
;; Private methods may operate upon specific sections of the state.
;;
;; Consider using map destructuring to simplify public methods that
;; only need certain components of the entire state.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn who-is-winning
  "Returns the key of the winning team, or nil if the game is tied"
  [{:keys [scores]}]
  (let [[b g] [(:black scores) (:gold scores)]
        d (- b g)]
    (cond
      (< d 0) :gold
      (> d 0) :black
      (= d 0) nil)))

(defmulti game-over?
  "Based on the provided state, returns true if the game is
  over else false"
  :game-mode)

(defmethod game-over? :first-to-max
  [{:keys [scores]}]
  (or (>= (:gold scores) (:max-score scores))
      (>= (:black scores) (:max-score scores))))

(defmethod game-over? :win-by-two
  [{:keys [scores] :as state}]
  (let [gold (:gold scores)
        black (:black scores)
        difference (- (max black gold) (min black gold))]
    (and (>= difference 2)
         (game-over? (assoc state :game-mode :first-to-max)))))

(defmethod game-over? :timed
  [{:keys [time end-time]}]
  (<= end-time time))

(defn point-for
  "Returns a state with a point added for team, or the current state if
  it is inappropriate to update the team's score"
  [state team]
  (if (game-over? state) 
      state
      (update-in state [:scores team] inc)))

(defn swap-players
  "Swap the players on team"
  [state team]
  (let [offense (-> state :teams team :offense)
        defense (-> state :teams team :defense)]
    (assoc-in state [:teams team] {:offense defense :defense offense})))

(defn change-status
  "Change the status of the state if the status is valid"
  [state status]
  {:pre [(some #{status} [:waiting :playing :black :gold])]}
  (assoc state :status status))

(defn add-player
  "Adds a player to a team, and defines the next player assignment"
  [{:keys [next-player] :as state} player]
  (let [next-next-player (next-player-transition next-player)]
    (-> state
        (assoc :next-player next-next-player)
        (assoc-in (cons :teams next-player) player))))

(defn- update-score-times
  "Adds a score time for the provided team"
  [{:keys [status time] :as state} team]
  (update state :score-times conj {:time time :team team}))

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Config state modifiers
;;;;;;;;;;;;;;;;;;;;;;;;;

(let [limiter (fn [f minimum]
                (fn [v]
                  (if (> (f v) (dec minimum)) (f v) minimum)))]
  (defn- max-score-limiter
    "Defines flooring logic for max-score modifications"
    [func]
    (limiter func 1))
  (defn- end-time-limiter
    "Defines flooring logic for end-time modifications"
    [func]
    (limiter func 15)))

(defn update-max-score
  "Update the max score by up / down increments"
  [state direction]
  {:pre [(some #{direction} [inc dec])]}
  (update-in state [:scores :max-score] (max-score-limiter direction)))

(defn- update-end-time
  "Updates the end time"
  [state direction]
  (update state :end-time (end-time-limiter direction)))

(defn increment-end-time
  "Increment the end time"
  [state]
  (update-end-time state (partial + 15)))

(defn decrement-end-time
  "Decrement the end time"
  [state]
  (update-end-time state #(- % 15)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event -> state transitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti event->state
  "Defines transitions into new states based on events. If the game is over,
  no custom event is dispatched, and the return is nil. Implementers shall
  either return a new state, or nil if there is no update."
  (fn [state event]
    (if (game-over? state) nil event)))

;; Clock ticks
(defmethod event->state :tick
  [{:keys [status game-mode] :as state} _]
  (when (= status :playing)
    (update state :time inc)))

;; Drop ball
(defmethod event->state :drop
  [{:keys [status] :as state} _]
  (if (not (= status :playing)) (change-status state :playing)))

;; Implementation for black / gold goals
(defn- goal->state
  [{:keys [status] :as state} team]
  (if (= status :playing)
    (let [state (point-for state team)]
      (-> state
          (change-status team)
          (update-score-times team)))))

;; Black goal
(defmethod event->state :black
  [state team]
  (goal->state state team))

;; Gold goal
(defmethod event->state :gold
  [state team]
  (goal->state state team))

;; Default event assumes a player sign in
(defmethod event->state :default
  [state player]
  (add-player state player))

;; nil occurs by default when the game is over
(defmethod event->state nil [_ _] nil)
