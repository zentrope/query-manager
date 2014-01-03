(ns query-manager.view.job-viewer
  (:use-macros
     [dommy.macros :only [sel1 node]])
  (:require
     [dommy.core :as dom]
     [cljs.core.async :refer [put!]]))

(defn- template
  []
  (node [:div#job-view-container
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

(defn- on-done-button-clicked
  [queue]
  (fn [e]
    (put! queue [:job-view-hide {}])))

(defn- on-show!
  []
  (dom/show! (sel1 :#job-view-container)))

(defn- on-hide!
  []
  (dom/hide! (sel1 :#job-view-container))
  (dom/set-html! (sel1 :#jv-desc) "job viewer")
  (dom/set-html! (sel1 :#job-viewer) "loading..."))

(defn- on-update!
  [job]
  (dom/set-html! (sel1 :#jv-desc) (:description (:query job)))
  (if-not (nil? (:error job))
    (dom/replace-contents! (sel1 :#job-viewer) (error-for job))
    (if (empty? (:results job))
      (dom/replace-contents! (sel1 :#job-viewer) (empty-results))
      (dom/replace-contents! (sel1 :#job-viewer) (table-of (:results job))))))

(defn- mk-template
  [queue]
  (let [body (template)]
    (dom/listen! [body :#jv-done] :click (on-done-button-clicked queue))
    body))

(def ^:private interceptor (atom nil))

(defn- register-key-handler!
  [queue]
  (let [keyhandler (fn [e]
                     (let [keycode (.-keyCode e)]
                       (when (or (= keycode 27)
                                 (= keycode 13))
                         (put! queue [:job-view-hide {}]))
                       (.stopPropagation e)
                       (.preventDefault e)))]
    (reset! interceptor keyhandler)
    (dom/listen! js/document :keydown keyhandler)))

(defn- unregister-key-handler!
  []
  (when-let [handler @interceptor]
    (dom/unlisten! js/document :keydown handler)
    (reset! interceptor nil)))

;;-----------------------------------------------------------------------------

(defn show!
  [queue]
  (let [body (mk-template queue)]
    (dom/append! (sel1 :body) body)
    (.focus (sel1 :#jv-done))
    (register-key-handler! queue)))

(defn hide!
  []
  (unregister-key-handler!)
  (when-let [place (sel1 :#job-view-container)]
    (dom/remove! place)))

(defn fill!
  [job]
  (on-update! job))
