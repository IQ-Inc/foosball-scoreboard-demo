(ns foosball-score.events
  "Event multimethod definition, and default event handling"
  {:author "Ian McIntyre"})

(defn- event-key
  [payload]
  (get-in payload [:?data 1 :event] :default))

(defmulti foosball-event
  "Handle a foosball event"
  event-key)

(defmethod foosball-event :default
  [event]
  nil)