(ns query-manager.view
  (:require [query-manager.view.db-form :as db-form]
            [query-manager.view.query-panel :as query-panel]
            [query-manager.view.job-panel :as job-panel]
            [query-manager.view.query-form :as query-form]
            [query-manager.view.job-viewer :as job-viewer]
            [query-manager.view.import :as import]
            [query-manager.view.frame :as frame]))

(defn show-app-frame!
  [queue]
  (frame/show! queue)
  (query-panel/show! queue :#left)
  (job-panel/show! queue :#right)
  (import/show! queue :body))

(defn set-frame-db!
  [db]
  (frame/set-db! db))

(defn fill-queries!
  [queue queries]
  (query-panel/fill-queries! queue queries))

(defn fill-jobs!
  [queue jobs]
  (job-panel/fill-jobs! queue jobs))

(defn show-job-viewer!
  [queue]
  (job-viewer/show! queue))

(defn hide-job-viewer!
  []
  (job-viewer/hide!))

(defn fill-job-viewer!
  [job]
  (job-viewer/fill! job))

(defn show-query-form!
  [queue]
  (query-form/show! queue))

(defn hide-query-form!
  []
  (query-form/hide!))

(defn fill-query-form!
  [query]
  (query-form/fill! query))

(defn show-db-form!
  [queue]
  (db-form/show! queue))

(defn fill-db-form!
  [db]
  (db-form/set-values! db))

(defn test-db-form!
  [result]
  (db-form/set-test-result! result))

(defn hide-db-form!
  []
  (db-form/hide!))
