(ns foosball-score.server
  "Server and serial startup"
  {:author "Ian McIntyre"}
  (:require 
    [foosball-score.handler :refer [app push-event! listen-for-ws foosball-event]]
    [foosball-score.serial :as serial]
    [foosball-score.events :as events]
    [foosball-score.state :as state]
    [foosball-score.tick :as tick]
    [foosball-score.statistics :as statistics]
    [foosball-score.persistence :as persist]
    [config.core :refer [env]]
    [org.httpkit.server :refer [run-server]])
  (:gen-class :main true))

(defmethod foosball-event :default
  [event]
  (state/update-state! event)
  (push-event! event))

(defn event-state-handler
  [event]
  (let [state @state/state
        next-state (some-> state
                           (state/event->state event)
                           (statistics/win-loss-stats))]
    (if (nil? next-state) state
      (do
        (push-event! next-state)
        (state/update-state! next-state)))
    (if-let [winners (:winners next-state)]
      (doseq [winner winners]
        (persist/win-for! winner)))
    (if-let [losers (:losers next-state)]
      (doseq [loser losers]
        (persist/loss-for! loser)))
    next-state))

(defn every-second
  "Invoked every second to sync the time across clients"
  []
  (event-state-handler :tick))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "3000"))
        ser (nth args 0)]
    (if-let [baud (read-string (nth args 1))]
      (serial/listen-on-port ser baud)
      (serial/listen-on-port ser))
    (serial/add-serial-subscriber
      (events/make-event-handler!
        event-state-handler))
    (run-server app {:port port :join? false})
    (listen-for-ws)
    (tick/call-every-ms every-second 1000)))
