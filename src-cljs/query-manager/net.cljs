(ns query-manager.net
  ;;
  (:require [query-manager.ajax :refer [ajax]]
            [cljs.core.async :as async]))

;;-----------------------------------------------------------------------------

(defn- jread
  [json]
  (js->clj json :keywordize-keys true))

(defn- error-handler
  [output-ch]
  (fn [err]
    (async/put! output-ch [:web-error {:value err}])))

;;-----------------------------------------------------------------------------
;; Database connection API
;;-----------------------------------------------------------------------------

(defn poke-db
  [output-ch]
  (ajax :uri "/qman/api/db"
        :method "GET"
        :on-failure (error-handler output-ch)
        :on-success (fn [db] (async/put! output-ch [:db-change {:value (jread db)}]))
        :type :json))

(defn test-db
  [output-ch db]
  (ajax :uri "/qman/api/db/test"
        :method "POST"
        :on-failure (error-handler output-ch)
        :on-success (fn [db] (async/put! output-ch [:db-test-result {:value (jread db)}]))
        :data db
        :type :json))

(defn save-db
  [output-ch db]
  (ajax :uri "/qman/api/db"
        :method "PUT"
        :on-failure (fn [err] (async/put! output-ch [:web-error {:value err}]))
        :on-success (fn [_] (poke-db output-ch))
        :data db
        :type :json))

;;-----------------------------------------------------------------------------
;; Jobs API
;;-----------------------------------------------------------------------------

(defn poke-jobs
  [output-ch]
  (ajax :uri "/qman/api/job"
        :method "GET"
        :type :json
        :on-failure (error-handler output-ch)
        :on-success (fn [jobs] (async/put! output-ch [:job-change {:value (jread jobs)}]))))

(defn poke-job
  [output-ch job-id]
  (ajax :uri (str "/qman/api/job/" job-id)
        :method "GET"
        :type :json
        :on-failure (error-handler output-ch)
        :on-success (fn [job] (async/put! output-ch [:job-get {:value (jread job)}]))))

(defn run-job
  [output-ch query-id]
  (ajax :uri (str "/qman/api/job/" query-id)
        :method "POST"
        :on-failure (error-handler output-ch)
        :on-success (fn [_] (poke-jobs output-ch))))

(defn delete-job
  [output-ch job-id]
  (ajax :uri (str "/qman/api/job/" job-id)
        :method "DELETE"
        :type :json
        :on-failure (error-handler output-ch)
        :on-success (fn [_] (poke-jobs output-ch))))

;;-----------------------------------------------------------------------------
;; Queries API
;;-----------------------------------------------------------------------------

(defn poke-queries
  [output-ch]
  (ajax :uri "/qman/api/query"
        :method "GET"
        :on-failure (error-handler output-ch)
        :on-success (fn [data]
                      (async/put! output-ch [:query-change {:value (jread data)}]))
        :type :json))

(defn poke-query
  [output-ch id]
  (ajax :uri (str "/qman/api/query/" id)
        :method "GET"
        :on-failure (error-handler output-ch)
        :on-success (fn [data] (async/put! output-ch [:query-get {:value (jread data)}]))
        :type :json))

(defn save-query
  [output-ch query]
  (ajax :uri "/qman/api/query"
        :method "POST"
        :data query
        :type :json
        :on-failure (error-handler output-ch)
        :on-success (fn [_] (poke-queries output-ch))))

(defn update-query
  [output-ch query]
  (ajax :uri (str "/qman/api/query/" (:id query))
        :method "PUT"
        :data query
        :type :json
        :on-failure (error-handler output-ch)
        :on-success (fn [_] (poke-queries output-ch))))

(defn delete-query
  [output-ch query-id]
  (ajax :uri (str "/qman/api/query/" query-id)
        :method "DELETE"
        :type :json
        :on-failure (error-handler output-ch)
        :on-success (fn [_] (poke-queries output-ch))))
