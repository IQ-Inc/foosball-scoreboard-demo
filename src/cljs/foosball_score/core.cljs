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

(defn new-game
  "Start a new game"
  []
  (do
    (players/reset-players!)
    (game/new-game)
    (clock/new-game)))

(defmethod events/foosball-event :default
  [event]
  (state/update-state! (:event event)))

;; -------------------------
;; Views

(defn home-page []
  [:div {:tab-index "1" :style {:outline "none"} :on-key-press (fn [_] (new-game))}
   [clock/game-clock new-game]
   [game/scoreboard :black :gold]
   [status/status-msg]
   [players/player-list]])

;; -------------------------
;; Routes

(def page (atom #'home-page))

(defn current-page []
  [:div [@page]])

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
