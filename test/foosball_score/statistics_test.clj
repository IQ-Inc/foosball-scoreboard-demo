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
    (let [input (-> test-state
                    (assoc-in [:scores :black] 1))
          expected (-> input
                       (assoc :winners #{"bo" "bd"})
                       (assoc :losers #{"go" "gd"}))]
      (is (= (statistics/win-loss-stats input) expected)))))