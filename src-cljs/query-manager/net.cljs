(ns query-manager.net
  ;;
  (:require [query-manager.ajax :refer [ajax]]
            [cljs.core.async :as async]))

;;-----------------------------------------------------------------------------

(defn- jread
  [json]
  (js->clj json :keywordize-keys true))

(defn- error-handler
  [queue]
  (fn [err]
    (async/put! queue [:web-error err])))

;;-----------------------------------------------------------------------------
;; Database connection API
;;-----------------------------------------------------------------------------

(defn poke-db
  [queue]
  (ajax :uri "/qman/api/db"
        :method "GET"
        :on-failure (error-handler queue)
        :on-success (fn [db] (async/put! queue [:db-change (jread db)]))
        :type :json))

(defn test-db
  [queue db]
  (ajax :uri "/qman/api/db/test"
        :method "POST"
        :on-failure (error-handler queue)
        :on-success (fn [db] (async/put! queue [:db-test-result (jread db)]))
        :data db
        :type :json))

(defn save-db
  [queue db]
  (ajax :uri "/qman/api/db"
        :method "PUT"
        :on-failure (fn [err] (async/put! queue [:web-error err]))
        :on-success (fn [_] (poke-db queue))
        :data db
        :type :json))

;;-----------------------------------------------------------------------------
;; Jobs API
;;-----------------------------------------------------------------------------

(defn poke-jobs
  [queue]
  (ajax :uri "/qman/api/job"
        :method "GET"
        :type :json
        :on-failure (error-handler queue)
        :on-success (fn [jobs] (async/put! queue [:job-change (jread jobs)]))))

(defn poke-job
  [queue job-id]
  (ajax :uri (str "/qman/api/job/" job-id)
        :method "GET"
        :type :json
        :on-failure (error-handler queue)
        :on-success (fn [job] (async/put! queue [:job-get (jread job)]))))

(defn run-job
  [queue query-id]
  (ajax :uri (str "/qman/api/job/" query-id)
        :method "POST"
        :on-failure (error-handler queue)
        :on-success (fn [_] (poke-jobs queue))))

(defn delete-job
  [queue job-id]
  (ajax :uri (str "/qman/api/job/" job-id)
        :method "DELETE"
        :type :json
        :on-failure (error-handler queue)
        :on-success (fn [_] (poke-jobs queue))))

;;-----------------------------------------------------------------------------
;; Queries API
;;-----------------------------------------------------------------------------

(defn poke-queries
  [queue]
  (ajax :uri "/qman/api/query"
        :method "GET"
        :on-failure (error-handler queue)
        :on-success (fn [data]
                      (async/put! queue [:query-change (jread data)]))
        :type :json))

(defn poke-query
  [queue id]
  (ajax :uri (str "/qman/api/query/" id)
        :method "GET"
        :on-failure (error-handler queue)
        :on-success (fn [data] (async/put! queue [:query-get (jread data)]))
        :type :json))

(defn save-query
  [queue query]
  (ajax :uri "/qman/api/query"
        :method "POST"
        :data query
        :type :json
        :on-failure (error-handler queue)
        :on-success (fn [_] (poke-queries queue))))

(defn update-query
  [queue query]
  (ajax :uri (str "/qman/api/query/" (:id query))
        :method "PUT"
        :data query
        :type :json
        :on-failure (error-handler queue)
        :on-success (fn [_] (poke-queries queue))))

(defn delete-query
  [queue query-id]
  (ajax :uri (str "/qman/api/query/" query-id)
        :method "DELETE"
        :type :json
        :on-failure (error-handler queue)
        :on-success (fn [_] (poke-queries queue))))
