(ns foosball-score.repl
  (:use foosball-score.handler
        figwheel-sidecar.repl-api
        foosball-score.events
        foosball-score.server
        foosball-score.tick
        [ring.middleware file-info file]
        [org.httpkit.server :refer [run-server]]
        [clojure.core.async :as async :refer [go go-loop chan >! <! close! put!]]))

(defonce server (atom nil))
(defonce event-chan (atom nil))

(defn serial-msg!
  "Routes msg through the event handler. Does nothing if the channel is nil."
  [msg]
  (if-let [chan @event-chan]
    (go (>! chan msg)))
  nil)

(defn- emit-event!
  "Passed into the event handler to print out the translation"
  [event]
  (println  "-- Event translation -- " event))

(defn- debug-event!
  "An auxilary debug handler for the event module"
  [event]
  (println "-- Debug msg -- " event))

(defn get-handler []
  ;; #'app expands to (var app) so that when we reload our code,
  ;; the server is forced to re-resolve the symbol in the var
  ;; rather than having its own copy. When the root binding
  ;; changes, the server picks it up without having to restart.
  (-> #'app
      ; Makes static assets in $PROJECT_DIR/resources/public/ available.
      (wrap-file "resources")
      ; Content-Type, Content-Length, and Last Modified headers for files in body
      (wrap-file-info)))

(defn start-server
  "used for starting the server in development mode from REPL"
  [& [port]]
  (let [port (if port (Integer/parseInt port) 3000)]
    (reset! server
            (run-server (get-handler)
                   {:port port
                    :auto-reload? true
                    :join? false}))
    (let [[in-chan out-chan] (event-state-task!)
          filt-chan (chan)]
      (async/pipeline 1 filt-chan (filter (comp not (partial = :tick) first)) out-chan)
      (go-loop [state (<! filt-chan)] (emit-event! state) (recur (<! filt-chan)))
      (reset! event-chan
              (make-event-handler! #(put! in-chan %) debug-event!))
      (listen-for-ws)
      (call-every-ms #(every-second in-chan) 1000))
    (println (str "You can view the site at http://localhost:" port))))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)
    (close! @event-chan)
    (reset! event-chan nil)))

(defn server-running?
  "Check if the server is running"
  []
  (not (nil? @server)))