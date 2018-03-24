(ns foosball-score.handler
  "Route and websocket handlers"
  {:author "Ian McIntyre"}
  (:require
    [compojure.core :refer [GET POST defroutes]]
    [compojure.route :refer [not-found resources]]
    [hiccup.page :refer [include-js include-css html5]]
    [foosball-score.middleware :refer [wrap-middleware]]
    [foosball-score.util :refer [ws-url]]
    [config.core :refer [env]]
    [taoensso.sente :as sente]
    [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/dseg.css" "/css/dseg.min.css"))
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids)) ; Watchable, read-only atom

(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/err" [] (loading-page))
  (GET  ws-url req (ring-ajax-get-or-ws-handshake req))
  (POST ws-url req (ring-ajax-post                req))
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))

(defn push-event!
  "Send an event to all connected clients"
  [event]
  (if-let [uids (:any @connected-uids)]
    (doseq [uid uids]
      (chsk-send! uid [:foosball/v0 {:event event}]))))

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Websocket multimethods
;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti websocket-event
  "Handle a websocket event"
  :id)

(defmulti foosball-event
  "Handle foosball-specific events"
  :event)

(defmethod websocket-event :foosball/v0
  [event]
  (foosball-event (get-in event [:event 1])))

(defmethod websocket-event :default
  [event]
  nil)

(defn listen-for-ws
  "Enable websocket listening"
  []
  (sente/start-server-chsk-router! ch-chsk websocket-event))