(ns query-manager.view
  (:require [query-manager.protocols :as proto]
            ;;
            ;;
            [query-manager.view.status-bar :as status-bar]
            [query-manager.view.title-bar :as title-bar]
            [query-manager.view.job-panel :as job-panel]
            [query-manager.view.db-form :as db-form]))

;;-----------------------------------------------------------------------------
;; Deprecated

(defrecord View [mbus template-fn]
  proto/IView
  (dom [this]
    (template-fn mbus)))

(defn mk-view
  [mbus template-fn subscriptions]
  (doseq [[topic handler] subscriptions]
    (proto/subscribe! mbus topic handler))
  (View. mbus template-fn))

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
           (db-form/instance)]})
