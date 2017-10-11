(ns foosball-score.state
  (:require
    #?(:cljs [reagent.core :refer [atom]])
    [clojure.data :refer [diff]]))

;; Application state
;; components are defined below.
(def state (atom (hash-map)))

(defn updates-between
  "Takes an old and a new state, and returns the values in the
  new state that are not in old state."
  [old new]
  (let [[_ change _] (diff old new)]
    change))

(defn update!
  "Replaces the current state with new-state"
  [new-state]
  (swap! state merge new-state))

;;;;;;;;;;;;;
;; Game state
;;;;;;;;;;;;;

(def add-game-state
  (swap! state merge
    {:status :waiting
     :game-mode :first-to-max}))

;;;;;;;;;;;;;;
;; Score state
;;;;;;;;;;;;;;

(def add-score-state
  (swap! state merge
    {:scores {:black 0 :gold 0}
     :max-score 5}))

;;;;;;;;;;;;;
;; Team state
;;;;;;;;;;;;;

(def add-team-state
  (swap! state merge
    {:teams
      {:black [nil nil]
       :gold  [nil nil]}}))

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
                  (and (>= d 2) (game-over? (assoc state :game-mode :first-to-max))))
    true)) ;; Default, game is over for invalid modes

(defn point-for
  "Returns a state with a point added for team, or the current state if
  it is inappropriate to update the team's score"
  [state team]
  (let [next-state (update-in state [:scores team] inc)]
    (if (game-over? next-state) state next-state)))