(ns foosball-score.deltapatch-test
  "Tests for deltapatch"
  {:author "Ian McIntyre"}
  (:require
    [foosball-score.deltapatch :refer [delta]]
    [clojure.test :refer :all]))

(deftest delta-test
  (testing "shows no delta for two unique maps"
    (let [from      {:foo 1}
          to        {:bar 2}
          expected  {}]
      (is (= expected (delta from to)))))

  (testing "shows no delta for the same maps"
    (let [from      {:foo 1}
          expected  {}]
      (is (= expected (delta from from)))))
      
  (testing "shows the delta for similar keys"
    (let [from      {:foo 1 :bar 2}
          to        {:foo 3 :baz 9}
          expected  {:foo 3}]
      (is (= expected (delta from to)))))
      
  (testing "shows the delta in nested maps"
    (let [from      {:foo {:bar 2} :baz 9}
          to        {:foo {:bar 3 :quz 8} :baz 9}
          expected  {:foo {:bar 3}}]
      (is (= expected (delta from to))))))