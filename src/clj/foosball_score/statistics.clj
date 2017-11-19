(ns foosball-score.statistics
  "Game and player statistics module"
  {:author "Ian McIntyre"}
  (:require
    [foosball-score.state :as state]
    [foosball-score.util :refer [teams]]))

(defn- pronounce-winners-losers
  "Adds winners and losers keys to the state, with a vector of related user IDs"
  [state]
  (let [losing-team (into {} [teams (vec (reverse teams))])
        ids-of (fn [team] (set (map :id (vals (-> state :teams team)))))]
    (if-let [winner (state/who-is-winning state)]
      (-> state
          (assoc :winners (ids-of winner))
          (assoc :losers (ids-of (losing-team winner))))
    state)))

(defn win-loss-stats
  "Accepts the state, and computes relavant statistics. If the statistics should
  be placed into the state, puts the statistics in the state. Otherwise, returns
  the state as-is. Never returns nil."
  [state]
  (if (state/game-over? state)
    (-> state
        pronounce-winners-losers)
    state))