(ns foosball-score.systests
  "End-to-end automated tests"
  {:author "Ian McIntyre"}
  (:require
    [foosball-score.handler :as handler :refer [push-event!]]))

(defmacro defsystest
  "Macro to create system tests"
  [name desc args & steps]
  `(defn ~name ~desc ~args 
    (println (str "Running test: " ~desc))
    ~@steps
    (println (str "Finished test: " ~desc))))

(defmacro step
  "Define a test step that will execute, print a message, then wait"
  [action msg wait]
  `(do ~action 
       (println ~msg)
       (Thread/sleep ~wait)))

(defn- seconds [ms] (* ms 1000))

(defsystest game-to
  "Simulate a game to five"
  [score winner]
  (dotimes [n score]
    (step (push-event! :drop) "Droping ball" (seconds 2))
    (step (push-event! winner) 
          (str "Point " (+ 1 n) " for " (name winner))
          (seconds 2))))