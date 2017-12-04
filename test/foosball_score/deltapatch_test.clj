(ns foosball-score.deltapatch-test
  "Tests for deltapatch"
  {:author "Ian McIntyre"}
  (:require
    [foosball-score.deltapatch :refer [delta patch]]
    [clojure.test :refer :all]))

(deftest delta-test
  (testing "shows no delta for two unique maps"
    (let [from      {:foo 1}
          to        {:bar 2}
          expected  {}]
      (is (= expected (delta from to)))))

  (testing "shows no delta for the same maps"
    (let [from      {:foo 1}
          to        from
          expected  {}]
      (is (= expected (delta from to)))))
      
  (testing "shows the delta only for similar keys"
    (let [from      {:foo 1 :bar 2}
          to        {:foo 3 :baz 9}
          expected  {:foo 3}]
      (is (= expected (delta from to)))))
      
  (testing "shows the delta in nested maps"
    (let [from      {:foo {:bar 2} :baz 9}
          to        {:foo {:bar 3 :quz 8} :baz 9}
          expected  {:foo {:bar 3}}]
      (is (= expected (delta from to)))))

  (testing "shows the delta in deeply nested maps"
    (let [from      {:bang 23 :foo {:bar {:baz 99 :qux 23}}}
          to        {:bag 23 :foo {:bar {:baz 99 :qux 24}}} ; bang => bag ignore
          expected  {:foo {:bar {:qux 24}}}]
      (is (= expected (delta from to)))))
    
  (testing "shows the delta for different collections"
    (let [from      {:foo [1 2] :baz 99 :bar '(4 5)}
          to        {:foo [1 3] :baz 99 :bar '(7 8)}
          expected  {:foo [1 3] :bar '(7 8)}]
      (is (= expected (delta from to))))))

(deftest patch-test
  (testing "patches an empty map with the delta"
    (is (= {:foo 1} (patch {} {:foo 1}))))
    
  (testing "patches an existing map with its delta"
    (is (= {:foo 3 :bar 1} (patch {:foo 1 :bar 1} {:foo 3})))))