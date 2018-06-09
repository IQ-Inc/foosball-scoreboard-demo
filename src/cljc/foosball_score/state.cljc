(ns foosball-score.state
  "State of all the things

  The namespace defines the components representing 'state' and the structure
  of the state. This module is shared across the client and server; it shall
  build in each environment."
  {:author "Ian McIntyre"}
  (:require
    #?(:cljs [reagent.core :refer [atom]])
    [clojure.data :refer [diff]]
    [foosball-score.util :refer [opposites]]))

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
   :end-time 120
   :overtime 0})

;;;;;;;;;;;;;
;; Ball state
;;;;;;;;;;;;;

(def new-ball-state
  {:balls 0
   :max-balls 3
   :last-drop-team nil})

;;;;;;;;;;;;
;; New state
;;;;;;;;;;;;

(def new-state
  (merge {} new-game-state
            new-team-state
            new-score-state
            new-time-state
            new-ball-state))

(defmulti new-game
  "Returns a new game state based on the current state"
  :game-mode)

(defmethod new-game :default
  [state]
  (let [max-score (get-in state [:scores :max-score])
        game-mode (get state :game-mode)
        end-time  (get state :end-time)
        max-balls (get state :max-balls)]
    (-> new-state
        (assoc-in [:scores :max-score] max-score)
        (assoc :game-mode game-mode)
        (assoc :end-time end-time)
        (assoc :max-balls max-balls))))

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

(def all-positions [[:black :offense]
                    [:gold  :offense]
                    [:black :defense]
                    [:gold  :defense]])

(def next-player-transition
  (partial next-transition all-positions))

(def next-game-mode-transition
  (partial next-transition
    [:first-to-max
     :win-by-two
     :timed
     :timed-ot
     :multiball]))

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

(defmethod game-over? :timed-ot
  [state]
  (and (not (nil? (who-is-winning state)))
       (game-over? (assoc state :game-mode :timed))))

(defmethod game-over? :multiball
  [{:keys [scores] :as state}]
  (let [gold (:gold scores)
        black (:black scores)]
    (>= (+ gold black) (:max-balls state))))

(defmethod game-over? :default
  [state]
  (println "Error handling a game-over? invocation with state of " state))

(defn overtime?
  "Returns true if the game is in overtime, else false"
  [state]
  (and (= (:game-mode state) :timed-ot)
       (game-over? (assoc state :game-mode :timed))
       (nil? (who-is-winning state))))

(defmulti mode-in-progress?
  "Returns true if, given the game mode, the game is in progress."
  :game-mode)

(defmethod mode-in-progress? :default
  [{:keys [time]}]
  (> time 0))

(defn in-progress?
  "Returns true if the game has started, else false. in-progress? is a function of the
  game mode.
  
  'In progress' means that a game has started, but we could be playing or waiting.
  Like some of the other consumers, the value is a function of the provided state
  information, rather than a separate state entity."
  [state]
  (reduce #(and %1 %2) true
          (map #(% state) [(comp not game-over?)
                           (comp not overtime?)
                           mode-in-progress?])))

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

(defn- change-status-dispatch
  [state status]
  {:pre [(some #{status} [:waiting :playing :black :gold])]}
  (:game-mode state))

(defmulti change-status change-status-dispatch)

(defmethod change-status :default
  [state status]
  (assoc state :status status))

(defmethod change-status :multiball
  [{old :status balls :balls max-balls :max-balls :as state} new]
  (let [new-state (assoc state :status new)]
    (case [old new]
      ([:playing :black] [:playing :gold])
        (if (= 0 balls) new-state state)
      [:waiting :playing]
        (if (>= balls max-balls) new-state state)
      new-state)))

(defn- signed-in-players
  "Returns the signed in players in a map of names to positions"
  [{:keys [teams]}]
  (reduce (fn [map position]
            (if-let [player (get-in teams position)]
              (assoc map player position)
              map))
          {} all-positions))

(defn add-player
  "Adds a player to a team, and defines the next player assignment. If the
  player is already in the state, add-player removes the older player assignment
  in favor of the new one. May squash a player assignment if the next player is
  already occupied."
  [{:keys [next-player] :as state} player]
  (let [signed-in (signed-in-players state)
        next-next-player (next-player-transition next-player)]
    (-> (if-let [dup-position (get signed-in player)]
          ;; Cannot dissoc-in, so we use assoc-in and set the player to nil.
          ;; This conforms to a player who is not signed in
          (assoc-in state (cons :teams dup-position) nil)
          state)
        (assoc :next-player next-next-player)
        (assoc-in (cons :teams next-player) player))))

(defn- update-score-times
  "Adds a score time for the provided team"
  [{:keys [status time overtime] :as state} team]
  (update state :score-times conj {:time (+ time overtime) :team team}))

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Config state modifiers
;;;;;;;;;;;;;;;;;;;;;;;;;

(let [limiter (fn [f minimum]
                (fn [v]
                  (if (> (f v) (dec minimum)) (f v) minimum)))]
  (defn- ball-count-limiter
    "Defines ball count limiting"
    [func]
    (limiter func 1))
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

(defn update-max-ball
  "Update the maximum ball count"
  [state direction]
  {:pre [(some #{direction} [inc dec])]}
  (update state :max-balls (ball-count-limiter direction)))

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

(defn play-pause
  "Sets the state to either playing or waiting, if the game is in progress. May be
  used to implement a pause / play button."
  [{:keys [status] :as state}]
  (if (in-progress? state)
    (let [mapping {:playing :waiting :waiting :playing}]
      (assoc state :status (mapping status)))
    state))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event -> state transitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; In a previous implementation, we did not differentiate
;; between gold and black drops. In order to introduce
;; the distinction, we would have to go through and update
;; all the multimethods.
;;
;; The hierarchy instead allows us to define specialized
;; drops that default to normal drops. If we want to
;; specialize an action to a gold / black drop, we can
;; easily do so by adding a new multimethod layer.
;;
;; This is literally so cool. Add new behaviors without
;; changing the callers or callee.
(def drop-hierarchy
  (-> (make-hierarchy)
      (derive :gold-drop :drop)
      (derive :black-drop :drop)))

(def drop->team
  {:gold-drop :gold
   :black-drop :black})

(defmulti event->state
  "Defines transitions into new states based on events. If the game is over,
  no custom event is dispatched, and the return is nil. Implementers shall
  either return a new state, or nil if there is no update."
  (fn [state event]
    (if (game-over? state) nil event))
  :hierarchy #'drop-hierarchy)

(defmulti drop->state
  "Specialization of an event->state transformer for handling ball drops as a function
  of game mode. Implementations shall return a state."
  (fn [state drop] [(:game-mode state) drop])
   :hierarchy #'drop-hierarchy)

;; Clock ticks
(defmethod event->state :tick
  [{:keys [status game-mode] :as state} _]
  (when (= status :playing)
    (if (overtime? state)
        (update state :overtime inc)
        (update state :time inc))))

(defmethod event->state :drop
  [state drop]
  (drop->state state drop))

;; Drop ball
(defn- into-playing
  [{:keys [status] :as state}]
  (when (not (= status :playing)) (change-status state :playing)))

(defmethod drop->state :default
  [state drop]
  (into-playing state))

(defmethod drop->state [:multiball :drop]
  [{:keys [max-balls last-drop-team] :as state} drop]
  (if (or (nil? last-drop-team)
          (= (opposites last-drop-team) (drop->team drop)))
    (let [state (-> state
                    (update :balls #(min (inc %) max-balls))
                    (assoc :last-drop-team (drop->team drop)))]
        (into-playing state))
    state))

;; Implementation for black / gold goals
(defn- goal->state
  [{:keys [status] :as state} team]
  (when (= status :playing)
    (-> state
        (point-for team)
        (update :balls #(max (dec %) 0))
        (change-status team)
        (update-score-times team))))

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
