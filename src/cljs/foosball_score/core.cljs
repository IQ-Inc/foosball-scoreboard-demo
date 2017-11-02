(ns foosball-score.core
  "Links the components of the frontend, and establishes the websocket handling

  This is the entrypoint for the frontend."
  {:author "Ian McIntyre"}
  (:require
   [reagent.core :as reagent :refer [atom]]
   [secretary.core :as secretary :include-macros true]
   [accountant.core :as accountant]
   [taoensso.sente  :as sente]
   [foosball-score.game :as game]
   [foosball-score.clock :as clock]
   [foosball-score.keypress :refer [keypress-handler]]
   [foosball-score.modes :as modes]
   [foosball-score.status :as status]
   [foosball-score.socket :as socket]
   [foosball-score.players :as players]
   [foosball-score.util :refer [ws-url]]
   [foosball-score.state :as state :refer [state]]))

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

(defn- notify-server
  [state]
  (state/update-state! state)
  (chsk-send! [:foosball/v0 state]))

(defn- swap-team!
  "Accepts the state, then returns a function that will swap the team players
  based on that state"
  [state]
  (fn [team] (notify-server (state/swap-players state team))))

(defmethod socket/foosball-event :default
  [event]
  (state/update-state! (:event event)))

(defn- on-key-press!
  "Maps a character chr to a keypress handler, forwarding through the state."
  [state chr]
  (when-let [handled (keypress-handler state
                                      (js/String.fromCharCode
                                        (.-charCode chr)))]
    (notify-server handled)))

;; -------------------------
;; Main UI manipulation
(defonce flash (atom false))
(defonce flasher-update (js/setInterval #(swap! flash not) 250))

(defn- flash-effect
  "Produce the border flash effect by returning an outline definition"
  [{:keys [status time end-time game-mode] :as state} flash?]
  (if (and flash?
           (not (state/game-over? state))
           (<= (- end-time time) 10)
           (= status :playing)
           (= game-mode :timed))
    "45px solid red"
    "none"))

;; -------------------------
;; Views
(defn home-page [state]
  [:div {:tab-index "1" :style {:outline (flash-effect state @flash)}
         :on-key-press (partial on-key-press! state)}
   [modes/game-modes state]
   [clock/game-clock state]
   [game/scoreboard (game/state-depends state) :black :gold]
   [status/status-msg state]
   [players/player-list (players/state-depends state) (swap-team! state)]])

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
  (sente/start-client-chsk-router! ch-chsk socket/websocket-event))

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
