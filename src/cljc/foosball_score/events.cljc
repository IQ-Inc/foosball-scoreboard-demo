(ns foosball-score.events
  "Event multimethod definition, and default event handling"
  {:author "Ian McIntyre"})

(defmulti foosball-event
  "Handle a foosball event"
  (fn [payload] (get-in payload [:?data 1 :event] :default)))

(defmethod foosball-event :default
  [event]
  nil)