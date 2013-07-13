(ns query-manager.view.job-panel
  (:use-macros [dommy.macros :only [sel sel1 node]])
  (:require [dommy.core :refer [attr replace-contents! listen! toggle!]]
            [query-manager.utils :refer [flash! listen-all!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private template
  (node [:div#jobs.panel
         [:div.panel-header "jobs"]
         [:div.panel-body
          [:div#jobs-table.lister
           [:p "No jobs."]]]]))

(defn- no-jobs
  []
  (node [:p "No job history."]))

(defn- table-of
  [jobs]
  (node (list [:table
               [:tr
                [:th {:width "10%"} "status"]
                [:th {:width "40%"} "query"]
                [:th {:width "15%"} "started"]
                [:th {:width "15%"} "stopped"]
                [:th {:width "10%"} "results"]
                [:th.actions {:width "10%"} "actions"]]
               (for [{:keys [id started stopped query status size]} jobs]
                 [:tr {:id (str "jp-row-" id)}
                  [:td {:class status} status]
                  [:td (:description query)]
                  [:td started]
                  [:td stopped]
                  [:td size]
                  [:td.actions
                   (when-not (zero? size)
                     [:button.jp-view {:jid id} "view"])
                   [:button.jp-del {:jid id} "del"]]])]
              [:button#jp-clear "clear all"])))

;; local events

(defn- on-clear
  [broadcast jids]
  (fn [e]
    (doseq [id jids]
      (broadcast [:job-delete {:value id}]))))

(defn- on-delete
  [broadcast]
  (fn [e]
    (let [id (attr (.-target e) :jid)]
      (flash! (sel1 (keyword (str "#jp-row-" id))) :flash)
      (broadcast [:job-delete {:value id}]))))

(defn- on-view
  [broadcast]
  (fn [e]
    (let [id (attr (.-target e) :jid)]
      (flash! (sel1 (keyword (str "#jp-row-" id))) :flash)
      (broadcast [:job-poke {:value id}]))))

;; incoming events

(defn- on-job-change
  [broadcast jobs]
  (when (empty? jobs)
    (replace-contents! (sel1 :#jobs-table) (no-jobs)))
  (when-not (empty? jobs)
    (replace-contents! (sel1 :#jobs-table) (table-of (sort-by :id jobs)))
    (listen-all! (sel :.jp-del) :click (on-delete broadcast))
    (listen-all! (sel :.jp-view) :click (on-view broadcast))
    (listen! (sel1 :#jp-clear) :click (on-clear broadcast (map :id jobs)))))

(defn- on-visibility-toggle!
  [broadcast]
  (toggle! (sel1 :#jobs)))

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
  [:job-change :job-panel-toggle])

(defn recv
  [broadcast [topic event]]
  (case topic
    :job-change (on-job-change broadcast (:value event))
    :job-panel-toggle (on-visibility-toggle! broadcast)
    true))
