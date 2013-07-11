(ns query-manager.view.export-panel
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace-contents! html set-html! listen! toggle!]]))

;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#export-panel.panel
         [:div#ep-data
          [:button [:a {:href "#"} "download"]]]]))

(defn- on-visibility-toggle!
  [broadcast]
  (toggle! (sel1 :#export-panel)))

(defn- on-data-update!
  [broadcast queries]
  (replace-contents!
   (sel1 :#ep-data)
   (node [:a {:href (str "data:application/edn;charset=utf-8," (str (js->clj queries)))
              :download "queries.clj"} "download"])))

;; (defn- on-download
;;   [broadcast]
;;   (fn [e]
;;     ;;
;;     ;;
;;     (let [data (str "data:text/plain;charset=utf-8,"
;;                     (html (sel1 :#ep-data)) #"\n" "\n")]
;;       (.log js/console data)
;;       (.open js/window data))))

(defn- mk-template
  [broadcast]
  (let [t (template)]
    ;;(listen! [t :#ep-download] :click (on-download broadcast))
    t))

;;-----------------------------------------------------------------------------
;; Public
;;-----------------------------------------------------------------------------

(defn dom
  [broadcast]
  (mk-template broadcast))

(defn events
  []
  [:query-change
   :export-panel-toggle])

(defn recv
  [broadcast [topic event]]
  (case topic
    :query-change (on-data-update! broadcast (:value event))
    :export-panel-toggle (on-visibility-toggle! broadcast)
    true))
