(ns foosball-score.events
  "Server serial <-> websocket event handling"
  {:author "Ian McIntyre"}
  (:require
    [clojure.core.async :refer [go go-loop chan <! >!]]))

(def event-lookup
  { "BD" :drop        ; black drop - not specified as a unique event
    "GD" :drop        ; gold drop - not specified as a unique event
    "BG" :black       ; black goal
    "GG" :gold })     ; gold goal

(defn make-event-handler!
  "Create an event handler, and returns a channel that will be awaited on
  for serial events. Accepts a callback, a function to receive translated
  events."
  [callback]
  (let [msg-chan (chan)]
    (go-loop [msg (<! msg-chan)]
      (if-let [event (get event-lookup msg)]
        (callback event))
      (recur (<! msg-chan)))
    msg-chan))