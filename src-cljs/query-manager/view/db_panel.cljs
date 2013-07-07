(ns query-manager.view.db-panel
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace-contents! listen! toggle!]]
            [query-manager.net :as net]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private template
  (node [:div#db-panel.panel
         [:h2 "Database"]
         [:div#database-table.lister]]))

(defn- table-of
  [{:keys [type host port user database]}]
  (node (list [:table
               [:tr
                [:th "type"]
                [:th "host"]
                [:th "port"]
                [:th "user"]
                [:th "database"]]
               [:tr
                [:td type]
                [:td host]
                [:td port]
                [:td user]
                [:td database]]]
              [:button#dbp-edit "edit"])))

(defn- on-click
  [broadcast db e]
  (broadcast [:db-form-show {:value db}]))

(defn- on-update
  [broadcast db]
  (replace-contents! (sel1 :#database-table) (table-of db))
  (listen! (sel1 :#dbp-edit) :click (partial on-click broadcast db)))

(defn- on-visibility-toggle!
  [broadcast]
  (toggle! (sel1 :#db-panel)))

(defn- mk-template
  [delegate]
  template)

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  [delegate]
  (mk-template delegate))

(defn events
  []
  [:db-change :db-panel-toggle])

(defn recv
  [broadcast [topic event]]
  (case topic
    :db-change (on-update broadcast (:value event))
    :db-panel-toggle (on-visibility-toggle! broadcast)
    true))
