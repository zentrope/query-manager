(ns query-manager.view.job-panel
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace-contents! listen!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private job-state (atom []))

(def ^:private template
  (node [:div#jobs.panel
         [:h2 "Jobs"]
         [:div#jobs-table.lister
          [:p "No jobs."]]]))

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
                [:th {:width "10%"} "actions"]]
               (for [{:keys [id started stopped query status size]} jobs]
                 [:tr
                  [:td {:class status} status]
                  [:td (:description query)]
                  [:td started]
                  [:td stopped]
                  [:td size]
                  [:td
                   [:button "noop"]]])]
              [:button#jp-clear "clear all"])))

(defn- on-clear
  [broadcast]
  (fn [e]
    (doseq [{:keys [id]} @job-state]
      (broadcast [:job-delete {:value id}]))))

(defn- on-job-change
  [broadcast jobs]
  (reset! job-state jobs)
  (when (empty? jobs)
    (replace-contents! (sel1 :#jobs-table) (no-jobs)))
  (when-not (empty? jobs)
    (replace-contents! (sel1 :#jobs-table) (table-of (sort-by :id jobs)))
    (listen! (sel1 :#jp-clear) :click (on-clear broadcast))))

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
  [:job-change])

(defn recv
  [broadcast [topic event]]
  (case topic
    :job-change (on-job-change broadcast (:value event))
    true))
