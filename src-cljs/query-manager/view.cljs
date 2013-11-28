(ns query-manager.view
  (:require [query-manager.view.status-bar :as status-bar]
            [query-manager.view.title-bar :as title-bar]
            [query-manager.view.job-panel :as job-panel]
            [query-manager.view.db-form :as db-form]
            [query-manager.view.error-panel :as error-panel]
            [query-manager.view.query-form :as query-form]
            [query-manager.view.job-viewer :as job-viewer]
;;            [query-manager.view.query-panel :as query-panel]
            [query-manager.import :as import]
            ))

;;-----------------------------------------------------------------------------

(defn- parts-of
  [state pred]
  (filter (fn [v] (not (nil? v))) (map pred (:views state))))

;;-----------------------------------------------------------------------------

(defn receivers
  [state]
  (parts-of state :recv))

(defn senders
  [state]
  (parts-of state :send))

(defn views
  [state]
  (parts-of state :view))

(defn instance
  []
  {:views [(status-bar/instance)
           (title-bar/instance)
           (job-panel/instance)
           (db-form/instance)
           (error-panel/instance)
           (query-form/instance)
           (job-viewer/instance)
           (import/instance)]})
