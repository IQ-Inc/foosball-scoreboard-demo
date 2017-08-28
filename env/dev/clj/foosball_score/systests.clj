(ns foosball-score.systests
  "End-to-end automated tests"
  {:author "Ian McIntyre"}
  (:require
    [foosball-score.repl :refer [server-running?]]
    [foosball-score.handler :as handler :refer [push-event!]]
    [foosball-score.util :refer [teams]]))

(def ^:private banner
  (apply str (take 78 (cycle [\-]))))

(defmacro defsystest
  "Macro to create system tests"
  [name desc args & steps]
  `(defn ~name ~desc ~args
    (if (not (server-running?))
      (println "Error: server not running. Start server with (start-server)")
      (do
        (println banner)
        (println (str "Running test: " ~desc))
        ~@steps
        (println (str "Finished test: " ~desc))
        (println banner)))))

(defmacro step
  "Define a test step that will execute, print a message, then wait"
  ([action msg wait]
  `(do ~action 
       (println ~msg)
       (Thread/sleep ~wait)))
  ([action msg] `(step ~action ~msg 0)))

(defn- seconds [sec] (* sec 1000))

(defn- team-valid? [team] (some #(= % team) teams))

(defsystest game-to
  "Play a game to score by pushing events through websockets"
  [score winner]
  (if (not (team-valid? winner))
    (println (str "Not a valid team: " (name winner)))
    (dotimes [n score]
      (step (push-event! :drop) "Droping ball" (seconds 2))
      (step (push-event! winner) 
            (str "Point " (+ 1 n) " for " (name winner))
            (seconds 2)))))

(defsystest ignore-double-drop
  "The UI ignores double-drop events"
  []
  (println "Test assumes a 'Waiting for ball drop' state...")
  (step (push-event! :drop)
        "Sent first drop..."
        (seconds 2))
  (step (push-event! :drop)
        "Sent second drop")
  (println "Observe that the clock is still ticking, and the scores are both 0"))

(defsystest ignore-double-goal
  "The UI ignores double-goal events"
  [scorer]
  (if (not (team-valid? scorer))
    (println (str "Not a valid team: " (name scorer)))
    (do
      (println "Test assumes a 'Waiting for ball drop' state...")
      (step (push-event! :drop)
            "Drop ball..."
            (seconds 2))
      (step (push-event! scorer)
            (str "Point for " (name scorer))
            (seconds 2))
      (step (push-event! scorer)
            (str "Another point for " (name scorer)))
      (println "Observe that the score for " (name scorer) " is still 1"))))