(ns foosball-score.events
  "Raw event translation

  The module was originally developed to handle events dispatched over serial.
  The module may also serve as a translation interface for anything dispatching
  multi-character messages."
  {:author "Ian McIntyre"}
  (:require
    [clojure.core.async :refer [go go-loop chan put! <! >!]]
    [foosball-score.persistence :as persist]))

(def ^:private debug-callback! (atom (fn [_] nil)))

(def event-lookup
  { "BD" :black-drop  ; black drop
    "GD" :gold-drop   ; gold drop
    "BG" :black       ; black goal
    "GG" :gold })     ; gold goal

(defmulti on-msg-size
  "Handle messages by raw message size"
  count)

;; Handle simple serial messages, like
;; those expected from the Arduino
(defmethod on-msg-size 2
  [msg]
  (if-let [game-event (get event-lookup msg)]
    game-event))

;; These are expected to be sign-in events
;; from an ID card. Example ID: 18A632
(defmethod on-msg-size (count "18A632")
  [msg]
  (if-let [claimed-user (persist/lookup-athlete msg)]
    claimed-user
    (persist/create-athlete! msg)))

;; Anything else is not handled
(defmethod on-msg-size :default
  [msg]
  (let [debug! @debug-callback!]
    (debug! (str "Received and dropped unexpected message of " msg))))

(defn make-event-handler!
  "Create an event handler, and returns a channel that will be awaited on
  for serial events. Accepts a callback, a function to receive translated
  events."
  ([c] (make-event-handler! c (fn [_] nil)))
  ([c debug-cb]
  (reset! debug-callback! debug-cb)
  (let [msg-chan (chan)]
    (go-loop [msg (<! msg-chan)]
      (if-let [event (on-msg-size msg)]
        (put! c event))
      (recur (<! msg-chan)))
    msg-chan)))