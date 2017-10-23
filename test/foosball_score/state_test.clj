(ns foosball-score.state-test
  (:require
    [foosball-score.state :as state]
    [clojure.test :refer :all]))

(deftest point-for-test
  (let [data {:scores {:black 0 :gold 0 :max-score 5}
              :game-mode :first-to-max}]
    
    (testing "Increments score by one"
      (let [expected (assoc-in data [:scores :black] 1)]
        (is (= (state/point-for data :black) expected)))
      (let [expected (assoc-in data [:scores :gold] 1)]
        (is (= (state/point-for data :gold) expected))))

    (testing "Score does not exceed max-score"
      (let [expected (assoc-in data [:scores :black] 5)
            actual (nth (iterate #(state/point-for % :black) data) 100)]
        (is (= actual expected))))))

(deftest who-is-winning-test
  (testing "Black is winning"
    (is (= :black (state/who-is-winning {:scores {:black 1 :gold 0}}))))

  (testing "Gold is winning"
    (is (= :gold (state/who-is-winning {:scores {:black 3 :gold 4}}))))

  (testing "Tied game"
    (is (nil? (state/who-is-winning {:scores {:black 2 :gold 2}})))))

(deftest first-to-max-test
  (let [data {:scores {:black 0 :gold 0 :max-score 5} 
              :game-mode :first-to-max}]

    (testing "Game is not over for zero scores"
      (is (not (state/game-over? data))))

    (testing "Game is not over for 4v4"
      (let [data (merge data {:scores {:black 4 :gold 4 :max-score 5}})]
        (is (not (state/game-over? data)))))

    (testing "Game is over for 5v4"
      (let [data (merge data {:scores {:black 5 :gold 4 :max-score 5}})]
        (is (state/game-over? data)))
      (let [data (merge data {:scores {:black 4 :gold 5 :max-score 5}})]
        (is (state/game-over? data))))))

(deftest win-by-two-test
  (let [data {:scores {:black 0 :gold 0 :max-score 5}
              :game-mode :win-by-two}]
              
    (testing "Game is not over for zero scoes"
      (is (not (state/game-over? data))))

    (testing "Game is not over for 4v4"
      (let [data (merge data {:scores {:black 4 :gold 4 :max-score 5}})]
        (is (not (state/game-over? data)))))

    (testing "Game is not over for 5v4"
      (let [data (merge data {:scores {:black 5 :gold 4 :max-score 5}})]
        (is (not (state/game-over? data))))
      (let [data (merge data {:scores {:black 4 :gold 5 :max-score 5}})]
        (is (not (state/game-over? data)))))

    (testing "Game is over for 6v4"
      (let [data (merge data {:scores {:black 6 :gold 4 :max-score 5}})]
        (is (state/game-over? data)))
      (let [data (merge data {:scores {:black 4 :gold 6 :max-score 5}})]
        (is (state/game-over? data))))

    (testing "Game is not over for 9v8"
      (let [data (merge data {:scores {:black 9 :gold 8 :max-score 5}})]
        (is (not (state/game-over? data))))
      (let [data (merge data {:scores {:black 8 :gold 9 :max-score 5}})]
        (is (not (state/game-over? data)))))))

(deftest swap-players-test
  (let [data {:teams
                 {:black {:offense "Mike" :defense nil}
                  :gold  {:offense "Frank" :defense "Norb"}}}]
    (testing "Swap players swaps players for specified team"
      (let [expected {:teams
                       {:black {:offense "Mike" :defense nil}
                        :gold  {:offense "Norb" :defense "Frank"}}}]
       (is (= (state/swap-players data :gold) expected)))
      (let [expected {:teams
                       {:black {:offense nil :defense "Mike"}
                        :gold  {:offense "Frank" :defense "Norb"}}}]
        (is (= (state/swap-players data :black) expected))))))

(deftest add-player-test
  (testing "Adds black offense player"
    (let [data {:next-player [:black :offense]}
          expected {:teams {:black {:offense "Ian"}}
                    :next-player [:gold :offense]}]
      (is (= (state/add-player data "Ian") expected))))

  (testing "Adds gold offense player"
    (let [data {:next-player [:gold :offense]
                :teams {:black {:offense "Bobby"}}}
          expected {:teams {:gold {:offense "Ian"} :black {:offense "Bobby"}}
                    :next-player [:black :defense]}]
      (is (= (state/add-player data "Ian") expected))))

  (testing "Adds black defense player"
    (let [data {:next-player [:black :defense]}
          expected {:teams {:black {:defense "Ian"}}
                    :next-player [:gold :defense]}]
      (is (= (state/add-player data "Ian") expected))))

  (testing "Adds gold defense player"
    (let [data {:next-player [:gold :defense]
                :teams {:black {:offense "Ryan" :defense "Mike"}}}
          expected {:teams {:gold {:defense "Ian"}
                           :black {:offense "Ryan" :defense "Mike"}}
                    :next-player [:black :offense]}]
      (is (= (state/add-player data "Ian") expected)))))
