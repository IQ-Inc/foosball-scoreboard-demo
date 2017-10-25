(ns foosball-score.tick
  "Provides means of dispatching function calls with millisecond resolution"
  {:author "Ian McIntyre"}
  (:require
    [clojure.core.async :as async :refer [timeout >! <! go-loop chan]]))

(defn tick
  "Start a clock that puts true onto a channel every t milliseconds. Returns
  that channel"
  [t]
  (let [ch (chan)]
    (go-loop []
      (<! (timeout t))
      (when (>! ch true)
        (recur)))
    ch))

(defn call-every-ms
  "Invokes function f every t ms"
  [f t]
  (let [tock (tick t)]
    (go-loop [_ (<! tock)]
      (f)
      (recur (<! tock)))))