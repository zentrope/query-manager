(ns query-manager.view.job-panel
  (:use-macros [dommy.macros :only [sel sel1 node]])
  (:require [dommy.core :refer [attr replace-contents! listen! toggle!]]
            [query-manager.protocols :refer [publish!]]
            [query-manager.view :refer [mk-view]]
            [query-manager.utils :refer [flash! listen-all! das]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#jobs.panel
         [:div.panel-header "jobs"]
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
      (str (das :hour d)
           ":"
           (das :minute d)
           ":"
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
               (for [{:keys [id started stopped query status size]} jobs]
                 [:tr {:id (str "jp-row-" id)}
                  [:td {:class status} status]
                  [:td (:description query)]
                  [:td (ftimestamp started)]
                  [:td (ftimestamp stopped)]
                  [:td.num (duration started stopped)]
                  [:td.num size]
                  [:td.actions
                   (when-not (zero? size)
                     [:button.jp-view {:jid id} "view"])
                   [:button.jp-del {:jid id} "del"]]])]
              [:button#jp-clear "clear all"])))

;; local events

(defn- on-clear
  [mbus jids]
  (fn [e]
    (doseq [id jids]
      (publish! mbus :job-delete {:value id}))))

(defn- on-delete
  [mbus]
  (fn [e]
    (let [id (attr (.-target e) :jid)]
      (flash! (sel1 (keyword (str "#jp-row-" id))) :flash)
      (publish! mbus :job-delete {:value id}))))

(defn- on-view
  [mbus]
  (fn [e]
    (let [id (attr (.-target e) :jid)]
      (flash! (sel1 (keyword (str "#jp-row-" id))) :flash)
      (publish! mbus :job-poke {:value id}))))

;; incoming events

(defn- on-job-change
  [mbus jobs]
  (when (empty? jobs)
    (replace-contents! (sel1 :#jobs-table) (no-jobs)))
  (when-not (empty? jobs)
    (replace-contents! (sel1 :#jobs-table) (table-of (sort-by :id jobs)))
    (listen-all! (sel :.jp-del) :click (on-delete mbus))
    (listen-all! (sel :.jp-view) :click (on-view mbus))
    (listen! (sel1 :#jp-clear) :click (on-clear mbus (map :id jobs)))))

(defn- on-visibility-toggle!
  [mbus]
  (toggle! (sel1 :#jobs)))

(defn- mk-template
  [mbus]
  (template))

(def ^:private subscriptions
  {:job-change (fn [mbus msg] (on-job-change mbus (:value msg)))
   :job-panel-toggle (fn [mbus msg] (on-visibility-toggle! mbus))})

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn mk-view!
  [mbus]
  (mk-view mbus mk-template subscriptions))
