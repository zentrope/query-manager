(ns query-manager.view.db-form
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-value! value listen! show! hide!]]
            [query-manager.view :refer [mk-view]]
            [query-manager.protocols :refer [publish!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
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
  [mbus]
  (fn [e]
    (publish! mbus :db-save {:value (mk-db)})
    (publish! mbus :db-form-hide {})))

(defn- on-cancel
  [mbus]
  (fn [e]
    (publish! mbus :db-form-hide {})))

(defn- mk-template
  [mbus]
  (let [t (template)]
    (listen! [t :#dbf-save] :click (on-save mbus))
    (listen! [t :#dbf-test] :click (not-implemented))
    (listen! [t :#dbf-cancel] :click (on-cancel mbus))
    t))

(defn- on-update
  [mbus {:keys [type host port user database password]}]
  (set-value! (sel1 :#dbf-type) type)
  (set-value! (sel1 :#dbf-host) host)
  (set-value! (sel1 :#dbf-user) user)
  (set-value! (sel1 :#dbf-pass) password)
  (set-value! (sel1 :#dbf-port) port)
  (set-value! (sel1 :#dbf-database) database))

(defn- on-show
  [mbus db-info]
  (show! (sel1 :#db-form-container)))

(defn- on-hide
  [mbus db-info]
  (hide! (sel1 :#db-form-container)))

(def ^:private subscriptions
  {:db-change (fn [mbus msg] (on-update mbus (:value msg)))
   :db-form-show (fn [mbus msg] (on-show mbus (:value msg)))
   :db-form-hide (fn [mbus msg] (on-hide mbus (:value msg)))})

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn mk-view!
  [mbus]
  (mk-view mbus mk-template subscriptions))
