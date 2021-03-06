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
      (is (= expected (delta from to)))))
      
  (testing "shows no change for similar submaps"
    (let [from      {:foo {:bar 1 :baz 2} :qux 43}
          to        {:foo {:bar 1 :baz 2} :qux 42}
          expected  {:qux 42}]
      (is (= expected (delta from to)))))
      
  (testing "shows a player sign in"
    (let [from      {:teams {:black {:offense nil :defense nil}
                             :gold  {:offense nil :defense nil}}}
          addition  {:name "Ian" :id "ABC123" :stats {:wins 1 :loses 2}}
          to        (assoc-in from [:teams :gold :offense] addition)
          expected  {:teams {:gold {:offense addition}}}]
      (is (= expected (delta from to)))))
      
  (testing "shows a player sign out"
    (let [player    {:name "Ian" :id "ABC123" :stats {:wins 1 :loses 2}}
          from      {:teams {:black {:offense player :defense nil}
                             :gold  {:offense nil    :defense nil}}}
          to        (assoc-in from [:teams :black :offense] nil)
          expected  {:teams {:black {:offense nil}}}]
      (is (= expected (delta from to))))))

(deftest patch-test
  (testing "patches an empty map with the delta"
    (is (= {:foo 1} (patch {} {:foo 1}))))
    
  (testing "patches an existing map with its delta"
    (is (= {:foo 3 :bar 1} (patch {:foo 1 :bar 1} {:foo 3}))))
    
  (testing "patches a nested map without losing keys"
    (is (= {:foo {:bar 1 :baz 2}}
           (patch {:foo {:bar 1 :baz 1}} {:foo {:baz 2}})))))