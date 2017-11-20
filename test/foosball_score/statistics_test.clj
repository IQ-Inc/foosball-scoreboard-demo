(ns foosball-score.statistics-test
  "Unit tests for statistics module"
  {:author "Ian McIntyre"}
  (:require
    [clojure.test :refer :all]
    [foosball-score.state :as state]
    [foosball-score.statistics :as statistics]))

;; The game is always over...
(defmethod state/game-over? :stats-test
  [_]
  true)

(def test-state
  (-> state/new-state 
      (assoc :game-mode :stats-test)
      (assoc-in [:teams :black] {:offense {:id "bo"} :defense {:id "bd"}})
      (assoc-in [:teams :gold] {:offense {:id "go"} :defense {:id "gd"}})))

(deftest win-loss-stats-test
  (testing "Identifies winners and losers in separate state entities"
    (let [winids {:gold #{"go" "gd"} :black #{"bo" "bd"}}
          lossids {:gold (:black winids) :black (:gold winids)}]
      (doseq [team [:black :gold]]
        (let [input (-> test-state
                        (assoc-in [:scores team] 1))
              expected (-> input
                          (assoc :winners (team winids))
                          (assoc :losers (team lossids))
                          (assoc :winning-team team))]
          (is (= (statistics/win-loss-stats input) expected))))))
          
  (testing "Has no win / loss identies when game is tied"
    (is (= (statistics/win-loss-stats test-state) test-state))))