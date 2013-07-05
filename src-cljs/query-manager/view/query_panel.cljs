(ns query-manager.view.query-panel
  (:use-macros [dommy.macros :only [sel1 sel node]])
  (:require [dommy.core :refer [attr remove-class! add-class! listen! replace-contents!]]
            [query-manager.utils :refer [flash! listen-all!]]
            [clojure.string :as string]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private template
  (node [:div#queries.panel
         [:h2 "Queries"]
         [:div#queries-table.lister
          [:p "No queries defined."]]]))

(defn- sql-of
  [sql]
  (-> (string/replace sql #"\s+" " ")
      (subs 0 50)
      (string/lower-case)
      (string/trim)
      (str "...")))

(defn- table-of
  [queries]
  (node (list [:table
               [:tr
                [:th {:width "40%"} "desc"]
                [:th {:width "45%"} "sql"]
                [:th {:width "15%"} "actions"]]
               (for [q queries]
                 [:tr {:id (str "qp-row-" (:id q))}
                  [:td (:description q)]
                  [:td (sql-of (:sql q))]
                  [:td {:style {:white-space "nowrap"}}
                   [:button.qp-run {:rid (:id q)} "run"]
                   [:button.qp-del {:did (:id q)} "del"]]])]
              [:button#qp-runall "run all"])))

(defn- on-run-all
  [broadcast]
  (fn [e]
    (js/alert "Running all queries not implemented. Sorry!")))

(defn- on-run
  [broadcast]
  (fn [e]
    (js/alert (str "Running [" (attr (.-target e) :rid) "] not implemented."))))

(defn- on-delete
  [broadcast]
  (fn [e]
    (let [id (attr (.-target e) :did)
          row (keyword (str "#qp-row-" id))]
      (flash! (sel1 row) :flash)
      (broadcast [:query-delete {:value id}]))))

(defn- on-query-change
  [broadcast queries]
  (if (empty? queries)
    (replace-contents! (sel1 :#queries-table) (node [:p "No queries defined."]))
    (let [table (table-of queries)]
      (replace-contents! (sel1 :#queries-table) table)
      (listen-all! (sel :.qp-run) :click (on-run broadcast))
      (listen-all! (sel :.qp-del) :click (on-delete broadcast))
      (listen! (sel1 :#qp-runall) :click (on-run-all broadcast)))))

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
    :query-change (on-query-change broadcast (:value event))
    true))
