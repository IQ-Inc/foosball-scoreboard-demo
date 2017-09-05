(ns foosball-score.events
  "Server serial <-> websocket event handling"
  {:author "Ian McIntyre"}
  (:require
    [clojure.core.async :refer [go go-loop chan <! >!]]
    [foosball-score.persistence :as persist]))

(def event-lookup
  { "BD" :drop        ; black drop - not specified as a unique event
    "GD" :drop        ; gold drop - not specified as a unique event
    "BG" :black       ; black goal
    "GG" :gold })     ; gold goal

(defmulti on-msg-size
  "Handle messages by size"
  count)

(defmethod on-msg-size 2
  [msg]
  (if-let [game-event (get event-lookup msg)]
    game-event))

(defmethod on-msg-size :default
  [msg]
  (if-let [claimed-user (persist/athlete-name (persist/lookup-athlete msg))]
    claimed-user
    msg))

(defn make-event-handler!
  "Create an event handler, and returns a channel that will be awaited on
  for serial events. Accepts a callback, a function to receive translated
  events."
  [callback]
  (let [msg-chan (chan)]
    (go-loop [msg (<! msg-chan)]
      (if-let [event (on-msg-size msg)]
        (callback event))
      (recur (<! msg-chan)))
    msg-chan))