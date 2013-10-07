(ns query-manager.view.job-viewer
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace-contents! set-html! show! hide! listen!]]
            [query-manager.view :as view]
            [query-manager.protocols :refer [publish!]]))

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
  [mbus]
  (fn [e]
    (publish! mbus :job-view-hide {})))

;;-----------------------------------------------------------------------------
;; Application Events
;;-----------------------------------------------------------------------------

(defn- on-show!
  [mbus]
  (show! (sel1 :#job-view-container)))

(defn- on-hide!
  [mbus]
  (hide! (sel1 :#job-view-container))
  (set-html! (sel1 :#jv-desc) "job viewer")
  (set-html! (sel1 :#job-viewer) "loading..."))

(defn- on-update!
  [mbus job]
  (set-html! (sel1 :#jv-desc) (:description (:query job)))
  (if-not (nil? (:error job))
    (replace-contents! (sel1 :#job-viewer) (error-for job))
    (if (empty? (:results job))
      (replace-contents! (sel1 :#job-viewer) (empty-results))
      (replace-contents! (sel1 :#job-viewer) (table-of (:results job))))))

;;-----------------------------------------------------------------------------
;; Template
;;-----------------------------------------------------------------------------

(defn- mk-template
  [mbus]
  (let [t (template)]
    (listen! [t :#jv-done] :click (on-done-button-clicked mbus))
    t))

(def ^:private subscriptions
  {:job-view-show (fn [mbus msg] (on-show! mbus))
   :job-view-hide (fn [mbus msg] (on-hide! mbus))
   :job-get (fn [mbus msg] (on-update! mbus (:value msg)))})

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn mk-view!
  [mbus]
  (view/mk-view mbus mk-template subscriptions))
