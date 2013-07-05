(ns query-manager.view.db-form
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-value! value listen! show! hide!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private template
  (node [:div#db-form-container.form-container {:style {:display "none"}}
         [:div#db-form.form
          [:h2 "Database Connection Info"]
          [:table
           [:tr [:th "type"] [:td [:select#dbf-type
                                   [:option {:value "h2"} "h2"]
                                   [:option {:value "mysql"} "mysql"]
                                   [:option {:value "oracle"} "oracle"]
                                   [:option {:value "sqlserver"} "sqlserver"]
                                   [:option {:value "postgresql"} "postgresql"]]]]
           [:tr [:th "database"] [:td [:input#dbf-database {:type "text"}]]]
           [:tr [:th "user"] [:td [:input#dbf-user {:type "text"}]]]
           [:tr [:th "password"] [:td [:input#dbf-pass {:type "password"}]]]
           [:tr [:th "host"] [:td [:input#dbf-host {:type "text"}]]]
           [:tr [:th "port"] [:td [:input#dbf-port {:type "text"}]]]]
          [:div.form-buttons
           [:button#dbf-save "save"]
           [:button#dbf-test "test"]
           [:button#dbf-cancel "cancel"]]]]))

(defn- mk-db
  []
  {:type (value (sel1 :#dbf-type))
   :host (value (sel1 :#dbf-host))
   :port (value (sel1 :#dbf-port))
   :user (value (sel1 :#dbf-user))
   :password (value (sel1 :#dbf-pass))
   :database (value (sel1 :#dbf-database))})

(defn- not-implemented
  []
  (fn [e]
    (js/alert "Not implemented.")))

(defn- on-save
  [broadcast]
  (fn [e]
    (broadcast [:db-save {:value (mk-db)}])
    (broadcast [:db-form-hide {}])))

(defn- on-cancel
  [broadcast]
  (fn [e]
    (broadcast [:db-form-hide {}])))

(defn- mk-template
  [broadcast]
  (listen! [template :#dbf-save] :click (on-save broadcast))
  (listen! [template :#dbf-test] :click (not-implemented))
  (listen! [template :#dbf-cancel] :click (on-cancel broadcast))
  template)

(defn- on-update
  [broadcast {:keys [type host port user database password]}]
  (set-value! (sel1 :#dbf-type) type)
  (set-value! (sel1 :#dbf-host) host)
  (set-value! (sel1 :#dbf-user) user)
  (set-value! (sel1 :#dbf-pass) password)
  (set-value! (sel1 :#dbf-port) port)
  (set-value! (sel1 :#dbf-database) database))

(defn- on-show
  [broadcast db-info]
  (show! (sel1 :#db-form-container)))

(defn- on-hide
  [broadcast db-info]
  (hide! (sel1 :#db-form-container)))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  [broadcast]
  (mk-template broadcast))

(defn events
  []
  [:db-change :db-form-hide :db-form-show])

(defn recv
  [broadcast [topic event]]
  (case topic
    :db-change (on-update broadcast (:value event))
    :db-form-show (on-show broadcast (:value event))
    :db-form-hide (on-hide broadcast (:value event))
    true))
