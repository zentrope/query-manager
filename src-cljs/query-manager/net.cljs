(ns query-manager.net
  (:require [query-manager.ajax :refer [ajax]]
            [cljs.core.async :as async]))

;;-----------------------------------------------------------------------------

(defn- jread
  [json]
  (js->clj json :keywordize-keys true))

(defn- mread
  [json]
  (let [[topic msg] (jread json)]
    [(keyword topic) msg]))

(defn- error-handler
  [queue]
  (fn [err]
    (async/put! queue [:web-error err])))

;;-----------------------------------------------------------------------------

(defn recv-message
  [queue]
  (ajax :uri "/qman/api/messages"
        :method "GET"
        :on-failure (error-handler queue)
        :on-success (fn [msg] (async/put! queue (mread msg)))
        :type :json))

(defn send-message
  [queue msg]
  (ajax :uri "/qman/api/messages"
        :method "POST"
        :on-failure (error-handler queue)
        :on-success (fn [_] )
        :data msg
        :type :json))

;;-----------------------------------------------------------------------------

(defn poke-db
  [queue]
  (send-message queue [:db-get {}]))

(defn test-db
  [queue db]
  (send-message queue [:db-test db]))

(defn save-db
  [queue db]
  (send-message queue [:db-save db]))

(defn poke-jobs
  [queue]
  (send-message queue [:job-list {}]))

(defn poke-job
  [queue job-id]
  (send-message queue [:job-get job-id]))

(defn run-job
  [queue query-id]
  (send-message queue [:job-run query-id]))

(defn delete-job
  [queue job-id]
  (send-message queue [:job-delete job-id]))

(defn poke-queries
  [queue]
  (send-message queue [:query-list {}]))

(defn poke-query
  [queue id]
  (send-message queue [:query-get id]))

(defn save-query
  [queue query]
  (send-message queue [:query-create query]))

(defn update-query
  [queue query]
  (send-message queue [:query-update query]))

(defn delete-query
  [queue query-id]
  (send-message queue [:query-delete query-id]))
