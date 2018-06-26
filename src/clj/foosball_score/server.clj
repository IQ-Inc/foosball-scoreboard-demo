(ns foosball-score.server
  "Server and serial startup"
  {:author "Ian McIntyre"}
  (:require 
    [foosball-score.events :as events]
    [foosball-score.deltapatch :refer [delta patch]]
    [foosball-score.handler :refer [app push-event! listen-for-ws foosball-event]]
    [foosball-score.serial :as serial]
    [foosball-score.state :as state]
    [foosball-score.tcp :as tcp]
    [foosball-score.tick :as tick]
    [foosball-score.slack :as slack]
    [foosball-score.statistics :as statistics]
    [foosball-score.persistence :as persist]
    [config.core :refer [env]]
    [clojure.core.async :as async :refer [go go-loop chan <! put!]]
    [org.httpkit.server :refer [run-server]])
  (:gen-class :main true))

;;;;;;;;;;;;;;;;;
;; Slack handling
;;;;;;;;;;;;;;;;;
(let [{:keys [post-msg!]} (slack/build-client (env :slack))]
  (def post-slack-msg! post-msg!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Foosball event responses
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod foosball-event :default
  [event _]
  (state/update-state! (patch @state/state event))
  (push-event! event))

(defmethod foosball-event :reset
  [event ?reply-fn]
  (when ?reply-fn
    (?reply-fn @state/state)))

;; Ignore client -> server debug events
(defmethod foosball-event :debug
  [_ _]
  nil)

(defn- persist-using!
  "Handles persistence of winners or losers using the provided persistence
  method. Returns the state with the winners / losers stripped out of the state"
  [state k fpersist]
  (if-let [wls (k state)]
    (do
      (doseq [wl wls] (fpersist wl))
      (dissoc state k))
    state))

(defn- event-state-handler
  "Pipe the current state and event through a variety of state-transforming functions. If
  one of them every returns nil, the operation is aborted, and the return is nil. Otherwise,
  the return is a new state."
  [state event]
  (some-> state
          (state/event->state event)
          (statistics/win-loss-stats)
          (persist-using! :winners persist/win-for!)
          (persist-using! :losers persist/loss-for!)
          (persist-using! :tiers persist/tie-for!)))

(defn event-state-task!
  "Spawns a task to process events that change the Foosball state. Events are pushed onto
  a channel, and the event with a new state is pushed onto an output channel. The return
  is a tuple of the input and output channels. The output channel pushes messages of
  [event new-state]."
  []
  (let [in  (chan)
        out (chan)]
    (go-loop [event   (<! in)       ; Get event from the input channel, waiting if necessary...
              state @state/state]   ; ...then immediately grab the current state, which may have changed
      (when-let [next-state (event-state-handler state event)]
        (if (state/game-over? next-state)
            (go (post-slack-msg! (slack/game-outcome next-state))))
        (push-event! (delta state next-state))
        (state/update-state! next-state)
        (put! out [event next-state]))
      (recur (<! in)              ; Get the event from the input channel, waiting if necessary...
             @state/state))       ; ...then immediately grab the current state, which may have changed
    [in out]))

(defn every-second
  "Invoked every second to sync the time across clients. Pushes an event on the provided channel."
  [event-chan]
  (put! event-chan :tick))

(defn- io-configuration
  "Returns a 'port listener' and 'subscriber' mechanism, depending on the
  specified interface. intf is one of 'serial' or 'tcp'"
  [intf arg]
  {:pre [(or (= intf "tcp") (= intf "serial"))]}
  (case intf
    "tcp"    [tcp/listen-on-port tcp/add-tcp-subscriber (read-string arg)]
    "serial" [serial/listen-on-port tcp/add-tcp-subscriber arg]
    "unreachable due to precondition checks"))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "3000"))
        [listen-on-port add-subscriber parsed-arg] (io-configuration (nth args 0) (nth args 1))]
    (listen-on-port parsed-arg)
    (let [[event-chan _] (event-state-task!)]      
      (add-subscriber
        (events/make-event-handler!
          event-chan #(push-event! {:debug %})))
      (run-server app {:port port :join? false})
      (listen-for-ws)
      (tick/call-every-ms #(every-second event-chan) 1000))))
