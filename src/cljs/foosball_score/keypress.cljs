(ns foosball-score.keypress
  "Keypress events for the UI. The keypress events accept, modify, and return
  a state. Or, the handler returns nil for no update."
  {:author "Ian McIntyre"}
  (:require
    [foosball-score.state :as state]))

(defmulti keypress-handler
  "Defines handling of keypresses"
  (fn [state chr] chr))

(defmethod keypress-handler \b
  [state _]
  (state/swap-players state :black))

(defmethod keypress-handler \g
  [state _]
  (state/swap-players state :gold))

(defmulti handle-up-down
  "Update the state in the provided direction"
  (fn [{:keys [game-mode]} direction] game-mode))

(defmethod handle-up-down :default
  [state direction]
  (state/update-max-score state direction))

(defmethod handle-up-down :timed
  [state direction]
  (let [lookup {inc state/increment-end-time
                dec state/decrement-end-time}
        func (get lookup direction)]
    (func state)))

(defmethod keypress-handler \j
  [state _]
  (handle-up-down state dec))

(defmethod keypress-handler \k
  [state _]
  (handle-up-down state inc))

(defmethod keypress-handler \m
  [{:keys [game-mode] :as state} _]
  (let [next-game-mode (state/next-game-mode-transition game-mode)]
    (-> state (assoc :game-mode next-game-mode))))

(defmethod keypress-handler \space
  [state chr]
  (state/new-game state))

(defmethod keypress-handler :default
  [_ _]
  nil)