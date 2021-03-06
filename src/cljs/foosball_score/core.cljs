(ns foosball-score.core
  "Links the components of the frontend, and establishes the websocket handling

  This is the entrypoint for the frontend."
  {:author "Ian McIntyre"}
  (:require
   [reagent.core :as reagent :refer [atom]]
   [secretary.core :as secretary :include-macros true]
   [accountant.core :as accountant]
   [taoensso.sente  :as sente]
   [foosball-score.colors :as colors]
   [foosball-score.deltapatch :refer [delta patch]]
   [foosball-score.game :as game]
   [foosball-score.click :as click]
   [foosball-score.clock :as clock]
   [foosball-score.keypress :refer [keypress-handler]]
   [foosball-score.modes :as modes]
   [foosball-score.status :as status]
   [foosball-score.socket :as socket]
   [foosball-score.players :as players]
   [foosball-score.util :refer [ws-url]]
   [foosball-score.state :as state]))

;; -------------------------
;; Debugging support
(defonce debug-msg (atom ""))

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
  (chsk-send! [:foosball/v0 (delta @state/state state)]))

(defn- reset-state!
  []
  (chsk-send! [:foosball/v0 {:event :reset}] 750
    (fn [state] (state/update-state! state))))

(defn- swap-team!
  "Accepts the state, then returns a function that will swap the team players
  based on that state"
  [state]
  (fn [team] (notify-server (state/swap-players state team))))

(defmethod socket/foosball-event :default
  [event]
  (state/update-state! (patch @state/state (:event event))))

(defmethod socket/foosball-event :debug
  [event]
  (reset! debug-msg (:debug event)))

(defn- on-key-press!
  "Maps a character chr to a keypress handler, forwarding through the state."
  [state chr]
  (when-let [handled (keypress-handler state
                                      (js/String.fromCharCode
                                        (.-charCode chr)))]
    (notify-server handled)))

(defn- mode-click-handlers
  [state]
  {:up   #(notify-server (click/increase-setting state))
   :down #(notify-server (click/decrease-setting state))
   :mode #(notify-server (click/change-mode state))})

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
           (some #{game-mode} [:timed :timed-ot]))
    {:outline-width "45px" :outline-color colors/overtime-accent :outline-style "solid"}
    {:outline "none"}))

;; -------------------------
;; Views
(defn home-page [state]
  [:div {:tab-index "1" :style (flash-effect state @flash)
         :on-key-press (partial on-key-press! state)}
   [modes/game-modes state (mode-click-handlers state)]
   [clock/game-clock state #(notify-server (click/new-game state))]
   [game/scoreboard state :black :gold #(notify-server (click/play-pause state))]
   [status/status-msg state]
   [:div.scoreboard.debug {:on-click #(reset! debug-msg "")} @debug-msg]
   [players/player-list (players/state-depends state) (swap-team! state)]])

(defn error-page [_]
  [:div {:class "ws-error"}
    [:h1 ":("]
    [:div "Your Foosball table ran into a problem and needs to restart. We're just collecting some error info,
    and then we'll restart for you."]])

;; -------------------------
;; Routes

(def page (atom #'home-page))

(defn current-page []
  [:div [@page @state/state]])

(secretary/defroute "/" []
  (reset! page #'home-page))

(secretary/defroute "/err" []
  (reset! page #'error-page))

;; -------------------------
;; Initialize app

(defn- ws-connection-callback
  [_ _ _ connected?]
  (if connected?
    (do (secretary/dispatch! "/") (reset-state!))
    (secretary/dispatch! "/err")))

(defn mount-root []
  (do
    (reagent/render [current-page] (.getElementById js/document "app"))
    (sente/start-client-chsk-router! ch-chsk socket/websocket-event)
    (add-watch socket/websocket-connected? :wserr ws-connection-callback)))

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
