(ns query-manager.view.job-viewer
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace-contents! set-html! show! hide! listen!]]))

;;-----------------------------------------------------------------------------
;; DOM
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#job-view-container {:style {:display "none"}}
         [:div.job-view
          [:h2#jv-desc "-"]
          [:p#jv-title.sub-title "Job Results Viewer"]
          [:div#job-viewer.job-view-area
            [:p "Nothing here yet."]]
          [:div.viewer-controls
           [:button#jv-done "done"]]]]))

(defn- error-for
  [job]
  (node [:div [:p (:error job)]]))

(defn- empty-results
  []
  (node [:div [:p "No rows returned for this query."]]))

(defn- table-of
  [results]
  (let [cols (keys (first results))]
    (node [:div.lister
           [:table
            [:tr (for [c cols] [:th (name c)])]
            (for [row results]
              [:tr (for [v (vals row)]
                     [:td (str v)])])]])))

;;-----------------------------------------------------------------------------
;; DOM Events
;;-----------------------------------------------------------------------------

(defn- on-done-button-clicked
  [broadcast]
  (fn [e]
    (broadcast [:job-view-hide {}])))

;;-----------------------------------------------------------------------------
;; Application Events
;;-----------------------------------------------------------------------------

(defn- on-show!
  [broadcast]
  (show! (sel1 :#job-view-container)))

(defn- on-hide!
  [broadcast]
  (hide! (sel1 :#job-view-container)))

(defn- on-update!
  [broadcast job]
  (set-html! (sel1 :#jv-desc) (:description (:query job)))
  ;;
  ;; TODO:
  ;; The button click in job_panel should first send a job-view-show,
  ;; then a job get. This pops up quick, the populates later.
  ;;
  (on-show! broadcast)
  ;;
  (if-not (nil? (:error job))
    (replace-contents! (sel1 :#job-viewer) (error-for job))
    (if (empty? (:results job))
      (replace-contents! (sel1 :#job-viewer) (empty-results))
      (replace-contents! (sel1 :#job-viewer) (table-of (:results job))))))

;;-----------------------------------------------------------------------------
;; Template
;;-----------------------------------------------------------------------------

(defn- mk-template
  [broadcast]
  (let [template (template)]
    (listen! [template :#jv-done] :click (on-done-button-clicked broadcast))
    template))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn events
  []
  [:job-get :job-view-show :job-view-hide])

(defn dom
  [broadcast]
  (mk-template broadcast))

(defn recv
  [broadcast [topic event]]
  (case topic
    :job-view-show (on-show! broadcast)
    :job-view-hide (on-hide! broadcast)
    :job-get (on-update! broadcast (:value event))
    true))
