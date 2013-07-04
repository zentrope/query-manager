(ns query-manager.view.db-panel
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace-contents! listen!]]
            [query-manager.net :as net]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private template
  (node [:div#container.lister
         [:h2 "Database"]
         [:div#database-table
          [:button#db-create "connect"]]]))

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
              [:button#db-edit "edit"])))

(defn- on-update
  [broadcast db]
  (replace-contents! (sel1 :#database-table) (table-of db))
  (listen! (sel1 :#db-edit) :click (fn [e] (broadcast [:db-form-show {:value db}]))))

(defn- on-error
  [err]
  (replace-contents! (sel1 :#database-table)
                     (node (list [:p "Unable to connect."]
                                 [:button#db-create "connect"]))))

(defn- on-success
  [delegate]
  (fn [db-info]
    (delegate [:loading {:value false}])
    (delegate [:db-change {:value (js->clj db-info :keywordize-keys true)}])))

(defn- on-failure
  [delegate]
  (fn [error]
    (delegate [:loading {:value false}])
    (delegate [:net-error {:value error}])))

(defn- on-click
  [delegate]
  (delegate [:loading {:value true}])
  (net/get-db (on-success delegate)
              (on-failure delegate)))

(defn- mk-template
  [delegate]
  (listen! [template :#create-connection] :click (fn [] (on-click delegate)))
  template)

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  [delegate]
  (mk-template delegate))

(defn events
  []
  [:db-change :net-error])

(defn recv
  [broadcast [topic event]]
  (case topic
    :db-change (on-update broadcast (:value event))
    :net-error (on-error (:value event))
    true))
