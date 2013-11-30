(ns query-manager.view.query-panel
  (:use-macros [dommy.macros :only [sel1 sel node]])
  (:require [dommy.core :as dom]
            [cljs.core.async :as async]
            [query-manager.utils :as utils]))

;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#queries.side-panel
         [:div.side-panel-header "queries"]
         [:div.side-panel-body
          [:div#queries-table.lister
           [:p "No queries defined."]
           [:button#qp-new "new"]]]]))

(defn- table-of
  [queries]
  (node (list [:table
               (for [q queries]
                 [:tr {:id (str "qp-row-" (:id q))}
                  [:td (:description q)]
                  [:td.actions
                   [:button.qp-run {:qid (:id q)} "run"]
                   [:button.qp-edit {:qid (:id q)} "edit"]
                   [:button.qp-del {:qid (:id q)} "del"]]])]
              [:button#qp-new "new"]
              [:button#qp-runall "run all"]
              [:button#qp-export "export"])))

(defn- on-run-all
  [queue qids]
  (fn [e]
    (doseq [qid qids]
      (async/put! queue [:query-run qid]))))

(defn- on-run
  [queue]
  (fn [e]
    (let [id (dom/attr (.-target e) :qid)]
      (utils/flash! (sel1 (keyword (str "#qp-row-" id))) :flash)
      (async/put! queue [:query-run id]))))

(defn- on-delete
  [queue]
  (fn [e]
    (let [id (dom/attr (.-target e) :qid)
          row (keyword (str "#qp-row-" id))]
      (utils/flash! (sel1 row) :flash)
      (async/put! queue [:query-delete id]))))

(defn- on-new
  [queue]
  (fn [e]
    (async/put! queue [:query-form-show {}])))

(defn- on-edit
  [queue]
  (fn [e]
    (let [id (dom/attr (.-target e) :qid)]
      (utils/flash! (sel1 (keyword (str "#qp-row-" id))) :flash)
      (async/put! queue [:query-poke id])
      (async/put! queue [:query-form-show {}]))))

(defn- on-query-change
  [queue queries]
  (if (empty? queries)
    (do (dom/replace-contents! (sel1 :#queries-table)
                               (node (list [:p "No queries defined."]
                                           [:button#qp-new "new"])))
        (dom/listen! (sel1 :#qp-new) :click (on-new queue)))

    (let [table (table-of (sort-by :id queries))]
      (dom/replace-contents! (sel1 :#queries-table) table)
      (utils/listen-all! (sel :.qp-run) :click (on-run queue))
      (utils/listen-all! (sel :.qp-edit) :click (on-edit queue))
      (utils/listen-all! (sel :.qp-del) :click (on-delete queue))
      (dom/listen! (sel1 :#qp-new) :click (on-new queue))
      (dom/listen! (sel1 :#qp-runall) :click (on-run-all queue (map :id queries)))
      (dom/listen! (sel1 :#qp-export) :click (fn [e]
                                               (async/put! queue [:export-queries {}]))))))

(defn- mk-template
  [queue]
  (let [body (template)]
    ;;(dom/listen! [body :#qp-new] :click (on-new queue))
    body))

;;-----------------------------------------------------------------------------

(defn show!
  [queue where]
  (let [doc (mk-template queue)]
    (dom/append! (sel1 where) doc)))

(defn fill-queries!
  [queue queries]
  (on-query-change queue queries))
