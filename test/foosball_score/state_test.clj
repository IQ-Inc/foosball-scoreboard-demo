(ns foosball-score.state-test
  (:require
    [foosball-score.state :as state]
    [clojure.test :refer :all]))

(deftest first-to-max-test
  (let [data {:scores {:black 0 :gold 0}
              :max-score 5 
              :game-mode :first-to-max}]

    (testing "Game is not over for zero scores"
      (is (not (state/game-over? data))))

    (testing "Game is not over for 4v4"
      (let [data (merge data {:scores {:black 4 :gold 4}})]
        (is (not (state/game-over? data)))))

    (testing "Game is over for 5v4"
      (let [data (merge data {:scores {:black 5 :gold 4}})]
        (is (state/game-over? data)))
      (let [data (merge data {:scores {:black 4 :gold 5}})]
        (is (state/game-over? data))))))

(deftest win-by-two-test
  (let [data {:scores {:black 0 :gold 0}
              :max-score 5
              :game-mode :win-by-two}]
              
    (testing "Game is not over for zero scoes"
      (is (not (state/game-over? data))))))

