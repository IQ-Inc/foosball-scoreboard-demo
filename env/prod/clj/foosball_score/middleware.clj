(ns foosball-score.middleware
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults site-defaults)))