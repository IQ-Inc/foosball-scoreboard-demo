(ns foosball-score.state-test
  (:require
    [foosball-score.state :as state]
    [clojure.test :refer :all]))

(deftest point-for-test
  (let [data {:scores {:black 0 :gold 0 :max-score 5}
              :game-mode :first-to-max
              :time 0
              :end-time 60}]
    
    (testing "Increments score by one"
      (let [expected (assoc-in data [:scores :black] 1)]
        (is (= (state/point-for data :black) expected)))
      (let [expected (assoc-in data [:scores :gold] 1)]
        (is (= (state/point-for data :gold) expected))))

    (testing "Score does not exceed max-score"
      (let [expected (assoc-in data [:scores :black] 5)
            actual (nth (iterate #(state/point-for % :black) data) 100)]
        (is (= actual expected))))

    (testing "Score may exceed max-score in timed game mode"
      (let [input (-> data (assoc :game-mode :timed) (update :time inc))
            expected (assoc-in input [:scores :black] 100)
            actual (nth (iterate #(state/point-for % :black) input) 100)]
        (is (= actual expected))))

    (testing "Score does not increment when time equals end-time in timed mode"
      (let [input (-> data (assoc :game-mode :timed) (assoc :time 60))]
        (is (= input (state/point-for input :black)))))))

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

(deftest timed-game-test
  (let [data {:scores {:black 0 :gold 0 :max-score 2}
              :game-mode :timed
              :time 60
              :end-time 61}]

    (testing "Game is not over when time does not equal end-time"
      (is (not (state/game-over? data))))

    (testing "Game is over when time equals end-time"
      (let [input (assoc data :time 61)]
        (is (state/game-over? input))))))

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
      (is (= (state/add-player data "Ian") expected))))
      
  (testing "Replaces duplicated player in new position"
    (let [data {:next-player [:gold :defense]
                :teams {:black {:offense "Ryan" :defense "Mike"}}}
          expected {:teams {:gold {:defense "Ryan"}
                           :black {:offense nil :defense "Mike"}}
                    :next-player [:black :offense]}]
      (is (= (state/add-player data "Ryan") expected))))

  (testing "Squashes existing player for next player assignment"
    (let [data {:next-player [:black :defense]
                :teams {:black {:offense "Ryan" :defense "Mike"}}}
          expected {:teams {:black {:offense "Ryan" :defense "Ian"}}
                    :next-player [:gold :defense]}]
      (is (= (state/add-player data "Ian") expected))))
      
  (testing "Squashes existing player to remove a duplicate"
    (let [data {:next-player [:black :defense]
                :teams {:black {:offense "Ryan" :defense "Mike"}}}
          expected {:teams {:black {:offense nil :defense "Ryan"}}
                    :next-player [:gold :defense]}]
      (is (= (state/add-player data "Ryan") expected)))))

(deftest event-state-transition-test
  (testing "Drop ball transitions to playing"
    (let [expected (assoc state/new-state :status :playing)]
      (is (= expected (state/event->state state/new-state :drop)))))

  (testing "Double-drop does nothing and returns nil for no update"
    (let [actual (-> state/new-state
                     (state/event->state :drop)
                     (state/event->state :drop))]
      (is (nil? actual))))

  (testing "No goals when not in playing state"
    (doseq [team [:black :gold]]
      (is (nil? (state/event->state state/new-state team)))))

  (testing "Goals occur when in playing state"
    (doseq [team [:black :gold]]
      (let [input (-> state/new-state 
                      (assoc :status :playing)
                      (assoc :time 42))
            expected (-> state/new-state
                         (update-in [:scores team] inc)
                         (assoc :time 42)
                         (assoc :status team)
                         (update :score-times conj {:time 42 :team team}))]
        (is (= expected (state/event->state input team))))))

  (testing "Ignores double goals and returns nil for no update"
    (doseq [team [:black :gold]]
      (let [input (assoc state/new-state :status :playing)
            actual (-> input
                       (state/event->state team)
                       (state/event->state team))]
        (is (nil? actual)))))

  (testing "Clock ticks increment time by one when playing"
    (let [input (assoc state/new-state :status :playing)
          expected (update input :time inc)]
      (is (= expected (state/event->state input :tick)))))

  (testing "Time does not increment for any other status"
    (doseq [status [:waiting :drop :black :gold]]
      (let [input (assoc state/new-state :status status)]
        (is (nil? (state/event->state input :tick))))))

  (testing "Point increments for identified team"
    (doseq [team [:black :gold]]
      (let [input    (-> state/new-state
                         (assoc :scores {:black 4 :gold 4 :max-score 5})
                         (assoc :status :playing)
                         (assoc :time 42)
                         (assoc :game-mode :first-to-max))
            expected (-> input
                         (update-in [:scores team] inc)
                         (assoc :status team)
                         (update :score-times conj {:time 42 :team team}))]
        (is (= expected (state/event->state input team))))))

  (testing "Game over state has no update"
    (is (nil? (state/event->state
                (assoc-in state/new-state [:scores :black] 5) :drop))))

  (testing "Time increments when in a timed game"
    (let [input (-> state/new-state
                    (assoc :game-mode :timed)
                    (assoc :time 60)
                    (assoc :end-time 120)
                    (state/change-status :playing))
          expected (-> input
                       (update :time inc))]
      (is (= expected (state/event->state input :tick))))))

(deftest new-game-test
  (testing "Resets everything but max-score, game-mode, and end-time"
    (let [input (-> state/new-state
                    (assoc-in [:scores :black] 4)
                    (assoc-in [:scores :gold] 3)
                    (assoc :game-mode :win-by-two)
                    (assoc-in [:scores :max-score] 2)
                    (assoc :time 999)
                    (assoc :end-time 89))
          expected (-> state/new-state
                       (assoc :game-mode :win-by-two)
                       (assoc-in [:scores :max-score] 2)
                       (assoc :end-time 89))]
      (is (= expected (state/new-game input))))))

(deftest update-max-score-test
  (let [data {:scores {:max-score 5}}]

    (testing "Increment max-score"
      (is (= {:scores {:max-score 6}} (state/update-max-score data inc))))

    (testing "Decrement max-score"
      (is (= {:scores {:max-score 4}} (state/update-max-score data dec))))

    (testing "Rejects any other updates with an AssertionError"
      (is (thrown? AssertionError
                   (state/update-max-score data (partial + 99)))))

    (testing "Does not decrement below 1"
      (let [input (assoc-in data [:scores :max-score] 1)]
        (is (= input (state/update-max-score input dec)))))

    (testing "May increment indefinitely"
      (let [actual (nth (iterate #(state/update-max-score % inc) data) 200)]
        (is (= {:scores {:max-score 205}} actual))))))

(deftest update-end-time-test
  (testing "Increment end time by 15"
    (is (= {:end-time 45} (state/increment-end-time {:end-time 30}))))

  (testing "Decrement end time by 15"
    (is (= {:end-time 15} (state/decrement-end-time {:end-time 30}))))

  (testing "Does not decrement below 15"
    (is (= {:end-time 15} (state/decrement-end-time {:end-time 15}))))

  (testing "Increments end-time indefinitely by multiples of 15"
    (let [actual (nth (iterate state/increment-end-time {:end-time 30}) 100)]
      (is (= {:end-time 1530} actual)))))