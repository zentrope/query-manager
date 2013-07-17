(ns query-manager.view.status-bar
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace! listen! set-html! toggle-class!]]
            [query-manager.view :refer [mk-view]]
            [query-manager.protocols :refer [publish!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#status-bar
         [:div#db-info "conn: none"]
         [:div.status-buttons
          [:button#sb-db.not-showing "db"]
          [:button#sb-err "err"]]
         [:div#mouse-coords
          "(" [:span#sb-mouse-x "0"] ":" [:span#sb-mouse-y "0"] ")"]]))

(defn- db-info-node
  [type host]
  (node [:div#db-info
         "conn: " [:span#db-type type]
         " on " [:span#db-host host]]))

(defn- set-mouse-coords!
  [[x y]]
  (set-html! (sel1 :#sb-mouse-x) x)
  (set-html! (sel1 :#sb-mouse-y) y))

(defn- set-db-info!
  [{:keys [type host] :as db}]
  (replace! (sel1 :#db-info) (db-info-node type host)))

(defn- on-toggle!
  [mbus topic]
  (fn [e]
    (publish! mbus topic {})))

(defn- mk-template
  [mbus]
  (let [t (template)]
    (listen! [t :#sb-db] :click (on-toggle! mbus :db-form-show))
    (listen! [t :#sb-err] :click (on-toggle! mbus :error-panel-toggle))
    t))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(def ^:private subscriptions
  {:mousemove (fn [mbus msg] (set-mouse-coords! (:value msg)))
   :db-change (fn [mbus msg] (set-db-info! (:value msg)))
   :db-form-hide (fn [mbus msg] (toggle-class! (sel1 :#sb-db) "not-showing"))
   :db-form-show (fn [mbus msg] (toggle-class! (sel1 :#sb-db) "not-showing"))
   :error-panel-toggle (fn [mbus msg] (toggle-class! (sel1 :#sb-err) "not-showing"))})

(defn mk-view!
  [mbus]
  (mk-view mbus mk-template subscriptions))
