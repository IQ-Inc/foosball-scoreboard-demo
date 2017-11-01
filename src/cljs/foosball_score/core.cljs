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

(defmethod socket/foosball-event :default
  [event]
  (state/update-state! (:event event)))

(defn- notify-server
  [state]
  (state/update-state! state)
  (chsk-send! [:foosball/v0 state]))

(defmulti keypress-handler
  "Defines handling of keypresses"
  (fn [state chr] chr))

(defn- swap-team
  "Accepts the state, then returns a function that will swap the team players
  based on that state"
  [state]
  (fn [team] (notify-server (state/swap-players state team))))

(defmethod keypress-handler \b
  [state _]
  ((swap-team state) :black))

(defmethod keypress-handler \g
  [state _]
  ((swap-team state) :gold))

(defmulti handle-up-down
  "Update the state in the provided direction"
  (fn [{:keys [game-mode]} direction] game-mode))

(defmethod handle-up-down :default
  [state direction]
  (notify-server (state/update-max-score state direction)))

(defmethod handle-up-down :timed
  [state direction]
  (let [lookup {inc state/increment-end-time
                dec state/decrement-end-time}
        func (get lookup direction)]
    (notify-server (func state))))

(defmethod keypress-handler \j
  [state _]
  (handle-up-down state dec))

(defmethod keypress-handler \k
  [state _]
  (handle-up-down state inc))

(defmethod keypress-handler \m
  [{:keys [game-mode] :as state} _]
  (let [next-game-mode (state/next-game-mode-transition game-mode)]
    (notify-server (-> state (assoc :game-mode next-game-mode)))))

(defmethod keypress-handler \space
  [state chr]
  (notify-server (state/new-game state)))

(defmethod keypress-handler :default
  [_ _]
  nil)

;; -------------------------
;; Views

(defn home-page [state]
  [:div {:tab-index "1" :style {:outline "none"}
        :on-key-press (fn [c] (keypress-handler state (js/String.fromCharCode (.-charCode c))))}
   [modes/game-modes state]
   [clock/game-clock state (partial notify-server (state/new-game state))]
   [game/scoreboard (game/state-depends state) :black :gold]
   [status/status-msg state]
   [players/player-list (players/state-depends state) (swap-team state)]])

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
