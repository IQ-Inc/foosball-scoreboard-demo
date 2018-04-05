(ns foosball-score.slack
  "Slack incoming webhook handler creation.
  
  Using the public interface to create a family of functions for interfacing with the Foosball Slack channel."
  {:author "Ian McIntyre"}
  (:require
    [clj-http.client :as client]
    [clojure.data.json :as json]
    [foosball-score.state :as state]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Incoming webhooks interface
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- post-msg!
  "Send a simple text message to Slack"
  [url msg]
  (client/post url {:body (json/write-str {:text msg})} :content-type :json))

(defn build-client
  "Build a Slack incoming webhooks client that interfaces with the provided URL.
  Returns a map with the following functions identified by keyword:
  
  - :post-msg! => accepts msg, a simple text message."
  [url]
  (if (nil? url)
    {:post-msg! (fn [_] (println "Attempt to call slack/post-msg! with a nil URL"))}
    {:post-msg! (partial post-msg! url)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers for message formatting
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn game-outcome
  "Create a message that describes the game outcome"
  [{:keys [scores] :as state}]
  (case (state/who-is-winning state)
    :black (str "Black beat gold " (:black scores) " to " (:gold scores))
    :gold  (str "Gold beat black " (:gold scores) " to " (:black scores))
    nil    (str "Game was tied at " (:gold scores))))