(ns foosball-score.statistics-test
  "Unit tests for statistics module"
  {:author "Ian McIntyre"}
  (:require
    [clojure.test :refer :all]
    [foosball-score.state :as state]
    [foosball-score.statistics :as statistics :refer [anti-team]]))

;; The game is always over...
(defmethod state/game-over? :stats-test
  [_]
  true)

(def simple-stats
  {:wins 0 :losses 0})

(def test-state
  (-> state/new-state 
      (assoc :game-mode :stats-test)
      (assoc-in [:teams :black] {:offense {:id "bo" :stats simple-stats} :defense {:id "bd" :stats simple-stats}})
      (assoc-in [:teams :gold] {:offense {:id "go" :stats simple-stats} :defense {:id "gd" :stats simple-stats}})))

(deftest win-loss-stats-test
  (testing "Updates winners and losers records, and identifies IDs"
    (let [winids {:gold #{"go" "gd"} :black #{"bo" "bd"}}
          lossids {:gold (:black winids) :black (:gold winids)}]
      (doseq [team [:black :gold]]
        (let [input (-> test-state
                        (assoc-in [:scores team] 1))
              expected (-> input
                          (assoc :winners (team winids))
                          (assoc :losers (team lossids))
                          (assoc :winning-team team)
                          (update-in [:teams team :offense :stats :wins] inc)
                          (update-in [:teams team :defense :stats :wins] inc)
                          (update-in [:teams (anti-team team) :offense :stats :losses] inc)
                          (update-in [:teams (anti-team team) :defense :stats :losses] inc))]
          (is (= (statistics/win-loss-stats input) expected))))))
          
  (testing "Has no win / loss stats when game is tied"
    (is (= (statistics/win-loss-stats test-state) test-state)))
    
  (testing "Has no win / loss stats when game is not over"
    (let [input (assoc test-state :game-mode :not-over)]
      (defmethod state/game-over? :not-over [_] false)
      (is (= (statistics/win-loss-stats input) input)))))