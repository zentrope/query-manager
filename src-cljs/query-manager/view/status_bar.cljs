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
         [:div.status-buttons
          [:button#sb-db.not-showing "db"]
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

(defn- on-toggle!
  [channel topic]
  (fn [e]
    (channel [topic {}])))

(defn- mk-template
  [channel]
  (let [t (template)]
    (listen! [t :#sb-db] :click (on-toggle! channel :db-form-show))
    (listen! [t :#sb-jobs] :click (on-toggle! channel :job-panel-toggle))
    (listen! [t :#sb-sql] :click (on-toggle! channel :query-panel-toggle))
    (listen! [t :#sb-err] :click (on-toggle! channel :error-panel-toggle))
    t))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  [channel]
  (mk-template channel))

(defn events
  []
  [:db-change
   :mousemove
   :db-form-hide
   :db-form-show
   :job-panel-toggle
   :query-panel-toggle
   :error-panel-toggle])

(defn recv
  [channel [topic event]]
  (case topic
    :db-change (set-db-info! (:value event))
    :mousemove (set-mouse-coords! (:value event))
    :db-form-hide (toggle-class! (sel1 :#sb-db) "not-showing")
    :db-form-show (toggle-class! (sel1 :#sb-db) "not-showing")
    :job-panel-toggle (toggle-class! (sel1 :#sb-jobs) "not-showing")
    :query-panel-toggle (toggle-class! (sel1 :#sb-sql) "not-showing")
    :error-panel-toggle (toggle-class! (sel1 :#sb-err) "not-showing")
    true))
