(ns foosball-score.socket
  "Socket input handling from the server"
  {:author "Ian McIntyre"})

(defonce websocket-connected? (atom false))

;; --------------------------------
;; Websocket configuration handling
(defmulti websocket-cfg
  (fn [payload] (get-in payload [:event 0])))

(defmethod websocket-cfg :chsk/state
  [payload]
  (let [[last now] (get-in payload [:event 1])
        connected? (:open? now)]
    (compare-and-set! websocket-connected? (not connected?) connected?)))

(defmethod websocket-cfg :default
  [_]
  nil)

;; --------------------------------

(defn- foosball-version-tag
  [payload]
  (get-in payload [:?data 0]))

(defmulti websocket-event
  "Handle a foosball event"
  foosball-version-tag)

(defmulti foosball-event :event)

(defmethod websocket-event :foosball/v0
  [event]
  (let [event (get-in event [:?data 1])]
    (if-let [debug-msg (get-in event [:event :debug])]
      (foosball-event {:event :debug :debug debug-msg})
      (foosball-event event))))

(defmethod websocket-event :default
  [event]
  (websocket-cfg event))