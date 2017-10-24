(ns foosball-score.core
  "Links the components of the frontend, and establishes the websocket handling"
  {:author "Ian McIntyre"}
  (:require
   [reagent.core :as reagent :refer [atom]]
   [secretary.core :as secretary :include-macros true]
   [accountant.core :as accountant]
   [taoensso.sente  :as sente]
   [foosball-score.game :as game]
   [foosball-score.clock :as clock]
   [foosball-score.status :as status]
   [foosball-score.events :as events]
   [foosball-score.players :as players]
   [foosball-score.state :refer [state]]
   [foosball-score.util :refer [ws-url]]
   [foosball-score.state :as state]))

;; -------------------------
;; Websocket setup
(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! ws-url ; Path must match the server's path
                                  {:type :auto})] ; e/o #{:auto :ajax :ws}
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state))  ; Watchable, read-only atom

;; -------------------------
;; Functions

(defmethod events/foosball-event :default
  [event]
  (state/update-state! (:event event)))

(defn- notify-server
  [state]
  (state/update-state! state)
  (chsk-send! [:foosball/v0 state]))

(defmulti keypress-handler
  "Defines handling of keypresses"
  (fn [state chr] chr))

(defmethod keypress-handler \b
  [state _]
  (notify-server (state/swap-players state :black)))

(defmethod keypress-handler \g
  [state _]
  (notify-server (state/swap-players state :gold)))

(defmethod keypress-handler :default
  [state chr]
  (println chr)
  (notify-server state/new-state))

;; -------------------------
;; Views

(defn home-page [state]
  [:div {:tab-index "1" :style {:outline "none"}
        :on-key-press (fn [c] (keypress-handler state (js/String.fromCharCode (.-charCode c))))}
   [clock/game-clock (clock/state-depends state) (partial notify-server state/new-state)]
   [game/scoreboard (game/state-depends state) :black :gold]
   [status/status-msg (:status state)]
   [players/player-list state notify-server]])

;; -------------------------
;; Routes

(def page (atom #'home-page))

(defn current-page []
  [:div [@page @state]])

(secretary/defroute "/" []
  (reset! page #'home-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (do
    (reagent/render [current-page] (.getElementById js/document "app")))
  (sente/start-client-chsk-router! ch-chsk events/websocket-event))

(defn init! []
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (secretary/dispatch! path))
    :path-exists?
    (fn [path]
      (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
