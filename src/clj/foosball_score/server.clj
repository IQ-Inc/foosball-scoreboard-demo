(ns foosball-score.server
  "Server and serial startup"
  {:author "Ian McIntyre"}
  (:require 
    [foosball-score.handler :refer [app push-event!]]
    [foosball-score.serial :as serial]
    [config.core :refer [env]]
    [clojure.core.async :as async :refer [<! chan go-loop]]
    [org.httpkit.server :refer [run-server]])
  (:gen-class :main true))

(def event-lookup
  { "BD" :drop        ; black drop - not specified as a unique event
    "YD" :drop        ; yellow drop - not specified as a unique event
    "BG" :black       ; black goal
    "YG" :yellow })   ; yellow goal

(def ^:private serial-event-chan (chan))

(defn- serial-message-handler
  "Get event from the serial module, and push it onto the
  websocket channel. Runs forever. Invocation does not block"
  [msg-chan]
  (go-loop [msg (<! msg-chan)]
    (if-let [event (get event-lookup msg)]
      (push-event! event))
      (recur (<! msg-chan))))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "3000"))
        ser (nth args 0)]
    (if-let [baud (read-string (nth args 1))]
      (serial/listen-on-port ser baud)
      (serial/listen-on-port ser))
    (serial/add-serial-subscriber serial-event-chan)
    (serial-message-handler serial-event-chan)
    (run-server app {:port port :join? false})))
