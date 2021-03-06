(ns query-manager.view.job-panel
  (:use-macros [dommy.macros :only [sel sel1 node]])
  (:require [dommy.core :as dom]
            [cljs.core.async :as async]
            [query-manager.utils :refer [flash! listen-all! das]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#jobs.panel
         [:div.panel-header "query job results"]
         [:div.panel-body
          [:div#jobs-table.lister
           [:p "No jobs."]]]]))

(defn- no-jobs
  []
  (node [:p "No job history."]))

(defn- ftimestamp
  [ts]
  (if (< ts 1)
    "-"
    (let [d (js/Date. ts)]
      (str (das :hour d) ":"
           (das :minute d) ":"
           (das :second d)))))

(defn- duration
  [started stopped]
  (if (< stopped 1)
    (node [:span.spin])
    (.toFixed (/ (- stopped started) 1000) 2)))

(defn- table-of
  [jobs]
  (node (list [:table
               [:tr
                [:th {:width "10%"} "status"]
                [:th {:width "40%"} "query"]
                [:th {:width "10%"} "started"]
                [:th {:width "10%"} "stopped"]
                [:th.num {:width "10%"} "duration (s)"]
                [:th.num {:width "10%"} "results"]
                [:th.actions {:width "10%"} "actions"]]
               (for [{:keys [id started stopped query status size count-col] :as job} jobs]
                 [:tr {:id (str "jp-row-" id)}
                  [:td {:class status} status]
                  [:td (:description query)]
                  [:td (ftimestamp started)]
                  [:td (ftimestamp stopped)]
                  [:td.num (duration started stopped)]
                  [:td.num (if-let [c count-col] c size)]
                  [:td.actions
                   (when-not (zero? size)
                     [:button.jp-view {:jid id} "view"])
                   [:button.jp-del {:jid id} "del"]]])]
              [:button#jp-clear "clear all"])))

;; local events

(defn- on-clear
  [output-ch]
  (fn [e]
    (async/put! output-ch [:job-delete-all {}])))

(defn- on-delete
  [output-ch]
  (fn [e]
    (let [id (dom/attr (.-target e) :jid)]
      (flash! (sel1 (keyword (str "#jp-row-" id))) :flash)
      (async/put! output-ch [:job-delete id]))))

(defn- on-view
  [output-ch]
  (fn [e]
    (let [id (dom/attr (.-target e) :jid)]
      (flash! (sel1 (keyword (str "#jp-row-" id))) :flash)
      (async/put! output-ch [:job-view-show {}])
      (async/put! output-ch [:job-poke id]))))

;; incoming events

(defn- on-job-change
  [output-ch jobs]
  (when (empty? jobs)
    (dom/replace-contents! (sel1 :#jobs-table) (no-jobs)))
  (when-not (empty? jobs)
    (dom/replace-contents! (sel1 :#jobs-table) (table-of (sort-by :id jobs)))
    (listen-all! (sel :.jp-del) :click (on-delete output-ch))
    (listen-all! (sel :.jp-view) :click (on-view output-ch))
    (dom/listen! (sel1 :#jp-clear) :click (on-clear output-ch))))

(defn- mk-template
  []
  (template))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn show!
  [queue place]
  (dom/append! (sel1 place) (mk-template)))

(defn fill-jobs!
  [queue jobs]
  (on-job-change queue jobs))
