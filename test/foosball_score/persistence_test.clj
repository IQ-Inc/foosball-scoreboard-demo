(ns foosball-score.persistence-test
  "Unit tests for persistence model accessors"
  (:require
    [foosball-score.persistence :as persist]
    [clojure.test :refer :all]))

(defn- make-test-athlete
  []
  (hash-map
    :name "Bobby"
    :stats
      (hash-map
        :wins 5
        :losses 7
        :ties 1)))

(deftest athlete-test
  (testing "Athlete is claimed"
    (is (true? (persist/athlete-claimed? (make-test-athlete)))))

  (testing "Athlete is unclaimed"
    (let [unclaimed (assoc (make-test-athlete) :name nil)]
      (is (false? (persist/athlete-claimed? unclaimed)))))

  (testing "Get athlete name"
    (is (= "Bobby" (persist/athlete-name (make-test-athlete)))))

  (testing "Get athlete wins"
    (is (= 5 (persist/athlete-wins (make-test-athlete)))))

  (testing "Get athlete losses"
    (is (= 7 (persist/athlete-losses (make-test-athlete)))))
  
  (testing "Get athlete ties"
    (is (= 1 (persist/athlete-ties (make-test-athlete)))))

  (testing "Get athlete games"
    (is (= 13 (persist/athlete-games (make-test-athlete))))))