(ns foosball-score.serial
  "Serial device interface and event mapping"
  {:author "Ian McIntyre"}
  
  (:require
    [clojure.core.async :as async  :refer [<!! >!! chan go go-loop]]
    [clojure.string :refer [trim]]
    [serial.core :as serial]
    [foosball-score.util :refer [serial-msg-sentinal]]))

(defonce events-chan 
  (atom (chan)))

(defonce serial-port
  (atom nil))

(def event-lookup
  { "BD" :drop        ; black drop - not specified as a unique event
    "YD" :drop        ; yellow drop - not specified as a unique event
    "BG" :black       ; black goal
    "YG" :yellow })   ; yellow goal

(defn get-event!
  "Get the most recent event. Blocks for the serial driver."
  []
  (let [c @events-chan]
    (<!! c)))

(defn serial-message-accumulate
  "Accumulates serial bytes into a string until the newline character. The
  function accounts for carriage returns by trimming excess whitespace."
  [in-stream]
  (loop [value (.read in-stream)
         acc []]
    (if (or (= value (byte \newline)) (= -1 value))
      (trim (apply str (map char acc)))
      (recur (.read in-stream) (conj acc value)))))

(defn- serial-to-event-handler
  "Maps a serial message into a foosball event.
  Blocks until the event is received on the other end."
  [input-stream]
  (let [c @events-chan
        event-key (serial-message-accumulate input-stream)]
    (if-let [event (get event-lookup event-key)]
      (>!! c event))))
    

(defn listen-on-port
  "Begin listening on the serial device specified by path.
  Optionally, provide a baud rate."
  ([path] (listen-on-port path 115200))
  ([path baud-rate]
  (let [ser (serial/open path :baud-rate baud-rate)]
    (serial/listen! ser serial-to-event-handler false)
    (reset! serial-port ser))))
