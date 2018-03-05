(ns foosball-score.persistence-int
  "Integration tests for persistence."
  {:author "Ian McIntyre"}
  (:require
    [foosball-score.persistence :as persist :refer [*in-mem-db*]]
    [clojure.test :refer :all]))

(def athlete-ids #{"ABC" "ZYX" "123" "987" "CLAIMED"})
(def unclaimed-athlete-ids
  (set (filter (complement #{"CLAIMED"}) athlete-ids)))

(defn- create-db
  "Create the database"
  []
  (doseq [athlete (vec athlete-ids)]
    (persist/create-athlete! athlete))
  (persist/claim-athlete! "CLAIMED" "Norb"))

(defn- destroy-db
  "Destroy the database"
  []
  (doseq [athlete (vec athlete-ids)]
    (persist/delete-athlete! athlete)))

(defn- with-database
  "Fixture for running a test with the database. Rebinds the dynamic database"
  [run-test]
  (binding [*in-mem-db* (atom {})]
    (create-db)
    (run-test)
    (destroy-db)))

(deftest ^:integration test-filter-unclaimed-athletes
  "Ensure correct athletes are unclaimed"
  (let [unclaimed (persist/athletes-who? persist/are-unclaimed :as-map)
        ids     (set (keys unclaimed))]
    (is (= ids unclaimed-athlete-ids))))

(deftest ^:integration test-filter-claimed-athletes
  "Ensure correct athlets are claimed"
  (let [claimed (persist/athletes-who? persist/are-claimed :as-map)
        id      (set (keys claimed))]
    (is (= id #{"CLAIMED"}))))

(defn- make-wl-test
  "Creates a win/loss test"
  [wl check id]
  (let [score-is  (fn [n]
                    (let [athlete (persist/lookup-athlete id)]
                      (is (= n (check athlete)))))]
    (score-is 0)
    (wl id)
    (score-is 1)))

(deftest ^:integration test-win-for
  "Ensure player gets a win"
  (testing "Unclaimed" (make-wl-test persist/win-for! persist/athlete-wins "ABC"))
  (testing "Claimed" (make-wl-test persist/win-for! persist/athlete-wins "CLAIMED")))

(deftest ^:integration test-loss-for
  "Ensure player gets a win"
  (testing "Unclaimed" (make-wl-test persist/loss-for! persist/athlete-losses "987"))
  (testing "Claimed" (make-wl-test persist/loss-for! persist/athlete-losses "CLAIMED")))

(use-fixtures :each with-database)