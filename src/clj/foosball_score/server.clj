(ns foosball-score.server
  "Server and serial startup"
  {:author "Ian McIntyre"}
  (:require 
    [foosball-score.handler :refer [app push-event!]]
    [foosball-score.serial :as serial]
    [foosball-score.events :as events]
    [foosball-score.state :as state]
    [config.core :refer [env]]
    [org.httpkit.server :refer [run-server]])
  (:gen-class :main true))

(defn event-state-handler
  [event]
  (let [state @state/state
        next-state (state/event->state state event)]
    (push-event! next-state)
    (state/update-state! next-state)
    next-state))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "3000"))
        ser (nth args 0)]
    (if-let [baud (read-string (nth args 1))]
      (serial/listen-on-port ser baud)
      (serial/listen-on-port ser))
    (serial/add-serial-subscriber
      (events/make-event-handler!
        event-state-handler))
    (run-server app {:port port :join? false})))
