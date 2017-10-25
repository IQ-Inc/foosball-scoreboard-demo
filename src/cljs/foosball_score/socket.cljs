(ns foosball-score.socket
  "Socket input handling from the server"
  {:author "Ian McIntyre"})

(defn- foosball-version-tag
  [payload]
  (get-in payload [:?data 0]))

(defmulti websocket-event
  "Handle a foosball event"
  foosball-version-tag)

(defmulti foosball-event :event)

(defmethod websocket-event :foosball/v0
  [event]
  (foosball-event (get-in event [:?data 1])))

(defmethod websocket-event :default
  [event]
  nil)