(ns query-manager.view.job-viewer
  ;;
  ;; Presents query (job) results.
  ;;
  (:use-macros [dommy.macros :only [sel1 node]]
               [cljs.core.async.macros :only [go-loop]])
  (:require [dommy.core :as dom]
            [cljs.core.async :as async]
            [query-manager.utils :as utils]))

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
  [output-ch]
  (fn [e]
    (async/put! output-ch [:job-view-hide {}])))

;;-----------------------------------------------------------------------------
;; Application Events
;;-----------------------------------------------------------------------------

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

;;-----------------------------------------------------------------------------
;; Template
;;-----------------------------------------------------------------------------

(defn- mk-template
  [output-ch]
  (let [body (template)]
    (dom/listen! [body :#jv-done] :click (on-done-button-clicked output-ch))
    body))

(defn- process
  [output-ch [topic msg]]
  (case topic
    :job-view-show (on-show!)
    :job-view-hide (on-hide!)
    :job-get (on-update! (:value msg))
    :noop))


(defn- block-loop
  [input-ch output-ch]
  (go-loop []
    (when-let [msg (async/<! input-ch)]
      (process output-ch msg)
      (recur))))

;;-----------------------------------------------------------------------------

(defn instance
  []
  (let [recv-ch (async/chan)
        send-ch (utils/subscriber-ch :job-view-show :job-view-hide :job-get)
        block (block-loop send-ch recv-ch)]
    {:recv recv-ch
     :send send-ch
     :view (mk-template recv-ch)
     :block block}))
