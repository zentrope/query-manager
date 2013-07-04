(ns query-manager.view.query-panel
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [listen! replace-contents!]]
            [clojure.string :as string]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private template
  (node [:div#queries.lister
         [:h2 "Queries"]
         [:div#queries-table "Not implemented."]]))

(defn- sql-of
  [sql]
  (-> (string/replace sql #"\s+" " ")
      (subs 0 50)
      (string/lower-case)
      (string/trim)
      (str "...")))

(defn- table-of
  [queries]
  (node [:table
         [:tr
          [:th {:width "45%"} "desc"]
          [:th {:width "45%"} "sql"]
          [:th {:width "10%"} "actions"]]
         (for [q queries]
           [:tr
            [:td (:description q)]
            [:td (sql-of (:sql q))]
            [:td [:button {:id (:id q)} "run"]]])]))

(defn- on-run-button-click
  [e]
  (let [id (.-id (.-target e))]
    (js/alert (str "Running [" id "] not implemented."))))

(defn- on-query-change
  [queries]
  (let [table (table-of queries)]
    (listen! [table :button] :click on-run-button-click)
    (replace-contents! (sel1 :#queries-table) table)))

(defn- mk-template
  [broadcast]
  template)

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  [broadcast]
  (mk-template broadcast))

(defn events
  []
  [:query-change])

(defn recv
  [broadcast [topic event]]
  (case topic
    :query-change (on-query-change (:value event))
    true))
