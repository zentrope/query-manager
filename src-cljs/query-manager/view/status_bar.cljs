(ns query-manager.view.status-bar
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace! listen! set-html! toggle-class!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#status-bar
         [:div#db-info "conn: none"]
         [:div.panel-buttons
          [:button#sb-db "db"]
          [:button#sb-jobs "jobs"]
          [:button#sb-sql "queries"]
          [:button#sb-err "err"]]
         [:div#mouse-coords
          "(" [:span#mouse-x "0"] ":" [:span#mouse-y "0"] ")"]]))

(defn- set-mouse-coords!
  [[x y]]
  (set-html! (sel1 :#mouse-x) x)
  (set-html! (sel1 :#mouse-y) y))

(defn- set-db-info!
  [{:keys [type host] :as db}]
  (replace! (sel1 :#db-info) (node [:div#db-info
                                    "conn: "
                                    [:span#db-type type]
                                    " on "
                                    [:span#db-host host]])))

(defn- on-toggle
  [broadcast topic]
  (fn [e]
    (broadcast [topic {}])))

(defn- mk-template
  [broadcast]
  (let [t (template)]
    (listen! [t :#sb-db] :click (on-toggle broadcast :db-panel-toggle))
    (listen! [t :#sb-jobs] :click (on-toggle broadcast :job-panel-toggle))
    (listen! [t :#sb-sql] :click (on-toggle broadcast :query-panel-toggle))
    (listen! [t :#sb-err] :click (on-toggle broadcast :error-panel-toggle))
    t))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  [broadcast]
  (mk-template broadcast))

(defn events
  []
  [:db-change :mousemove
   :db-panel-toggle :job-panel-toggle :query-panel-toggle :error-panel-toggle])

(defn recv
  [broadcast [type event]]
  (case type
    :db-change (set-db-info! (:value event))
    :mousemove (set-mouse-coords! (:value event))
    :db-panel-toggle (toggle-class! (sel1 :#sb-db) "not-showing")
    :job-panel-toggle (toggle-class! (sel1 :#sb-jobs) "not-showing")
    :query-panel-toggle (toggle-class! (sel1 :#sb-sql) "not-showing")
    :error-panel-toggle (toggle-class! (sel1 :#sb-err) "not-showing")
    true))
