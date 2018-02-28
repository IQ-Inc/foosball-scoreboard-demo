(ns foosball-score.tcp
  "Serial-like interface, but using TCP as the underlying protocol. Presents the exact
  same interface as the serial module. Intended for dropping-in as a serial interface
  replacement."
  {:author "Ian McIntyre"}
  (:require
    [clj-tcp.client :as tcp]
    [clojure.core.async :as async :refer [go go-loop <! >!]]))

(def ^:private subscribers
  (atom '()))

(defn add-tcp-subscriber
  "Register a subscriber for messages over TCP. Messages will be pushed onto
  the provided channel."
  [chan]
  (swap! subscribers conj chan))

(defn- bytestream->string
  [bs]
  (apply str (butlast (slurp bs))))

(defn- loop-and-read!
  [client]
  (go-loop []
    (let [bs (<! (:read-ch client))
          msg (bytestream->string bs)
          subs @subscribers]
      (doseq [sub subs]
        (go (>! sub msg))))
    (recur)))

(defn listen-on-port
  "Begin listening on the TCP port. If a second argument is provided (a drop-in for baud rate)
  it is entirely ignored."
  ([port baud-rate] (listen-on-port port))
  ([port]
    (let [client (tcp/client "localhost" port {})]
      (loop-and-read! client))))