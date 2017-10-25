(ns foosball-score.serial
  "Serial device interface and event mapping"
  {:author "Ian McIntyre"}
  
  (:require
    [clojure.core.async :as async :refer [>! go]]
    [clojure.string :refer [trim]]
    [serial.core :as serial]))

(def ^:private subscribers
  (atom '()))

(defonce ^:private serial-port
  (atom nil))

(defn add-serial-subscriber
  "Register a subscriber for serial messages. Messages will be pushed onto the
  provided channel."
  [chan]
  (swap! subscribers conj chan))

(defn- notify-subscribers
  "Notify subscribers to the serial event"
  [event]
  (let [chans @subscribers]
    (doseq [chan chans]
      (go (>! chan event)))))

(defn serial-message-accumulate
  "Accumulates serial bytes into a string until the newline character. The
  function accounts for carriage returns by trimming excess whitespace."
  [in-stream]
  (loop [value (.read in-stream)
         acc []]
    (if (or (= value (byte \newline)) (= -1 value))
      (trim (apply str (map char acc)))
      (recur (.read in-stream) (conj acc value)))))

(defn- serial-byte-handler
  "Handle a serial input stream, and notify subscribers"
  [input-stream]
  (let [serial-msg (serial-message-accumulate input-stream)]
    (notify-subscribers serial-msg)))

(defn listen-on-port
  "Begin listening on the serial device specified by path.
  Optionally, provide a baud rate."
  ([path] (listen-on-port path 115200))
  ([path baud-rate]
  (let [ser (serial/open path :baud-rate baud-rate)]
    (serial/listen! ser serial-byte-handler false)
    (reset! serial-port ser))))
