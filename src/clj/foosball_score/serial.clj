(ns foosball-score.serial
  "Serial device interface and event mapping"
  {:author "Ian McIntyre"}
  
  (:require
    [clojure.core.async :as async  :refer [<!! >!! chan go go-loop]]
    [serial.core :as serial]))

(defonce events-chan 
  (atom (chan)))

(defonce serial-port
  (atom nil))

(defonce byte-to-event
  (hash-map
    (byte \D) :drop       ; black drop - not specified
    (byte \C) :drop       ; yellow drop - not specified
    (byte \B) :black      ; black goal
    (byte \A) :yellow))   ; yellow goal

(defn get-event!
  "Get the most recent event. Blocks for the serial driver."
  []
  (let [c @events-chan]
    (<!! c)))

(defn byte-to-event-handler
  "Maps a byte into a foosball event.
  Blocks until the event is received on the other end.
  TODO make this handle multi-byte events."
  [input-stream]
  (let [c @events-chan
        b (.read input-stream)]
    (if-let [event (get byte-to-event b)]
      (>!! c event))))
    

(defn listen-forever-on-port
  "Begin listening on the serial device specified by path.
  Optionally, provide a baud rate."
  ([path] (listen-forever-on-port path 115200))
  ([path baud-rate]
  (let [ser (serial/open path :baud-rate baud-rate)]
    (serial/listen! ser byte-to-event-handler)
    (reset! serial-port ser))))
