(ns query-manager.view.error-panel
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace-contents! listen!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private errors (atom []))

(def ^:private template
  (node [:div#error-panel.panel
         [h2 "Error Log"]
         [:div#ep-list.lister
          [:p "No errors!"]]]))

(defn- table-of
  [errors]
  (node (list [:table
               [:tr
                [:th "status"]
                [:th "error"]]
               (for [{:keys [status reason]} errors]
                 [:tr
                  [:td status]
                  [:td reason]])]
              [:button#ep-clear "clear"])))

(defn- on-clear
  [broadcast]
  (fn [e]
    (reset! errors [])
    (replace-contents! (sel1 :#ep-list) (node [:p "No errors!"]))))

(defn- on-error
  [broadcast error-event]
  (let [errors (swap! errors conj error-event)]
    (replace-contents! (sel1 :#ep-list) (table-of errors))
    (listen! (sel1 :#ep-clear) :click (on-clear broadcast))))

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
  [:web-error])

(defn recv
  [broadcast [topic event]]
  (case topic
    :web-error (on-error broadcast (:value event))
    true))
