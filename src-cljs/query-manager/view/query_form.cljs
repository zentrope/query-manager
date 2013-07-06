(ns query-manager.view.query-form
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-value! value listen! show! hide!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private template
  (node [:div#query-form-container.form-container {:style {:display "none"}}
         [:div#query-form.form
          [:h2 "Query Definition"]
          [:input#qf-id {:type "hidden"}]
          [:table
           [:tr [:th "Description"] [:td [:input#qf-desc {:type "text"}]]]
           [:tr [:th "Query"] [:td [:textarea#qf-sql {:rows "18"}]]]]
          [:div.form-buttons
           [:button#qf-save "save"]
           [:button#qf-cancel "cancel"]]]]))

(defn- retrieve-query
  [])

;; Local event handlers

(defn- on-cancel
  [broadcast]
  (fn [e]
    (broadcast [:query-form-hide {}])))

(defn- on-save
  [broadcast]
  (fn [e]
    (let [query {:id (value (sel1 :#qf-id))
                 :sql (value (sel1 :#qf-sql))
                 :description (value (sel1 :#qf-desc))}]
      (broadcast [:query-update {:value query}])
      (broadcast [:query-form-hide {}]))))

;; Incoming event handlers

(defn- on-show
  [broadcast]
  (show! (sel1 :#query-form-container)))

(defn- on-hide
  [broadcast]
  (hide! (sel1 :#query-form-container)))

(defn- on-update
  [broadcast {:keys [id sql description]}]
  (set-value! (sel1 :#qf-id) id)
  (set-value! (sel1 :#qf-desc) description)
  (set-value! (sel1 :#qf-sql) sql))

(defn- mk-template
  [broadcast]
  (listen! [template :#qf-save] :click (on-save broadcast))
  (listen! [template :#qf-cancel] :click (on-cancel broadcast))
  template)

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  [broadcast]
  (mk-template broadcast))

(defn events
  []
  [:query-form-show :query-form-hide :query-get])

(defn recv
  [broadcast [topic event]]
  (case topic
    :query-form-show (on-show broadcast)
    :query-form-hide (on-hide broadcast)
    :query-get (on-update broadcast (:value event))
    true))
