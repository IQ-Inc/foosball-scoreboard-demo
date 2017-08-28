(ns foosball-score.server
  "Server and serial startup"
  {:author "Ian McIntyre"}
  (:require 
    [foosball-score.handler :refer [app push-event!]]
    [foosball-score.serial :as serial]
    [foosball-score.events :as events]
    [config.core :refer [env]]
    [org.httpkit.server :refer [run-server]])
  (:gen-class :main true))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "3000"))
        ser (nth args 0)]
    (if-let [baud (read-string (nth args 1))]
      (serial/listen-on-port ser baud)
      (serial/listen-on-port ser))
    (serial/add-serial-subscriber (events/make-event-handler! push-event!))
    (run-server app {:port port :join? false})))
