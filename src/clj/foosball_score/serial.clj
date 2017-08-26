(ns foosball-score.serial
  "Serial device interface and event mapping"
  {:author "Ian McIntyre"}
  
  (:require
    [clojure.core.async :as async  :refer [<!! >!! chan go go-loop]]
    [serial.core :as serial]
    [foosball-score.util :refer [serial-msg-sentinal]]))

(defonce events-chan 
  (atom (chan)))

(defonce serial-port
  (atom nil))

(defonce max-serial-msg-size 256)

(defonce event-lookup
  (hash-map
    "BD" :drop       ; black drop - not specified as a unique event
    "YD" :drop       ; yellow drop - not specified as a unique event
    "BG" :black      ; black goal
    "YG" :yellow))   ; yellow goal

(defn get-event!
  "Get the most recent event. Blocks for the serial driver."
  []
  (let [c @events-chan]
    (<!! c)))

(defn serial-message-accumulate
  "Accumulates serial bytes into a string until the newline character,
  end of file, or excess of max-serial-msg-size iterations."
  ([input-stream] (serial-message-accumulate input-stream [] 0))
  ([input-stream acc iter]
    (let [eol (byte serial-msg-sentinal)
          b (.read input-stream)]
      (if (or (= b eol) (> iter (- max-serial-msg-size 1)))
        (apply str (map char acc))
        (serial-message-accumulate
          input-stream (conj acc b) (inc iter))))))

(defn serial-to-event-handler
  "Maps a serial message into a foosball event.
  Blocks until the event is received on the other end."
  [input-stream]
  (let [c @events-chan
        k (serial-message-accumulate input-stream)]
    (if-let [event (get event-lookup k)]
      (>!! c event))))
    

(defn listen-forever-on-port
  "Begin listening on the serial device specified by path.
  Optionally, provide a baud rate."
  ([path] (listen-forever-on-port path 115200))
  ([path baud-rate]
  (let [ser (serial/open path :baud-rate baud-rate)]
    (serial/listen! ser serial-to-event-handler false)
    (reset! serial-port ser))))
