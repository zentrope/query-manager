(ns query-manager.view.query-panel
  (:use-macros [dommy.macros :only [sel1 sel node]])
  (:require [dommy.core :refer [toggle! attr listen! replace-contents!]]
            [query-manager.view :refer [mk-view]]
            [query-manager.protocols :refer [publish!]]
            [query-manager.utils :refer [flash! listen-all!]]
            [clojure.string :as string]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#queries.panel
         [:div.panel-header "queries"]
         [:div.panel-body
          [:div#queries-table.lister
           [:p "No queries defined."]
           [:button#qp-new "new"]]]]))

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
                [:th {:width "40%"} "query"]
                [:th {:width "45%"} "sql"]
                [:th.actions {:width "15%"} "actions"]]
               (for [q queries]
                 [:tr {:id (str "qp-row-" (:id q))}
                  [:td (:description q)]
                  [:td (sql-of (:sql q))]
                  [:td.actions
                   [:button.qp-run {:qid (:id q)} "run"]
                   [:button.qp-edit {:qid (:id q)} "edit"]
                   [:button.qp-del {:qid (:id q)} "del"]]])]
              [:button#qp-new "new"]
              [:button#qp-runall "run all"]
              [:button#qp-export "export"])))

(defn- on-run-all
  [mbus qids]
  (fn [e]
    (doseq [qid qids]
      (publish! mbus :query-run {:value qid}))))

(defn- on-run
  [mbus]
  (fn [e]
    (let [id (attr (.-target e) :qid)]
      (flash! (sel1 (keyword (str "#qp-row-" id))) :flash)
      (publish! mbus :query-run {:value id}))))

(defn- on-delete
  [mbus]
  (fn [e]
    (let [id (attr (.-target e) :qid)
          row (keyword (str "#qp-row-" id))]
      (flash! (sel1 row) :flash)
      (publish! mbus :query-delete {:value id}))))

(defn- on-new
  [mbus]
  (fn [e]
    (publish! mbus :query-form-show {})))

(defn- on-edit
  [mbus]
  (fn [e]
    (let [id (attr (.-target e) :qid)]
      (flash! (sel1 (keyword (str "#qp-row-" id))) :flash)
      (publish! mbus :query-poke {:value id})
      (publish! mbus :query-form-show {}))))

(defn- on-query-change
  [mbus queries]
  (if (empty? queries)

    (do (replace-contents! (sel1 :#queries-table)
                           (node (list [:p "No queries defined."]
                                       [:button#qp-new "new"])))
        (listen! (sel1 :#qp-new) :click (on-new mbus)))

    (let [table (table-of (sort-by :id queries))]
      (replace-contents! (sel1 :#queries-table) table)
      (listen-all! (sel :.qp-run) :click (on-run mbus))
      (listen-all! (sel :.qp-edit) :click (on-edit mbus))
      (listen-all! (sel :.qp-del) :click (on-delete mbus))
      (listen! (sel1 :#qp-new) :click (on-new mbus))
      (listen! (sel1 :#qp-runall) :click (on-run-all mbus (map :id queries)))
      (listen! (sel1 :#qp-export) :click (fn [e]
                                           (publish! mbus :export-queries {}))))))

(defn- on-visibility-toggle!
  [mbus]
  (toggle! (sel1 :#queries)))

(defn- mk-template
  [mbus]
  (template))

(def ^:private subscriptions
  {:query-change (fn [mbus msg]
                   (on-query-change mbus (:value msg)))
   :query-panel-toggle (fn [mbus msg]
                         (on-visibility-toggle! mbus))})


;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn mk-view!
  [mbus]
  (mk-view mbus mk-template subscriptions))
