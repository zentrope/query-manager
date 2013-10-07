(ns query-manager.view.query-form
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-value! set-html! value listen! show! hide!]]
            [query-manager.view :as view]
            [query-manager.protocols :refer [publish!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#query-form-container.form-container {:style {:display "none"}}
         [:div#query-form.form
          [:h2 "Query Definition"]
          [:input#qf-id {:type "hidden"}]
          [:table
           [:tr [:th "description"] [:td [:input#qf-desc {:type "text"}]]]
           [:tr [:th "query"] [:td [:textarea#qf-sql {:rows "18"}]]]]
          [:div.form-buttons
           [:button#qf-save "save"]
           [:button#qf-cancel "cancel"]]]]))

;; Local event handlers

(defn- on-cancel
  [mbus]
  (fn [e]
    (publish! mbus :query-form-hide {})))

(defn- on-save
  [mbus]
  (fn [e]
    (let [query {:id (value (sel1 :#qf-id))
                 :sql (value (sel1 :#qf-sql))
                 :description (value (sel1 :#qf-desc))}]
      (if (empty? (:id query))
        (publish! mbus :query-save {:value query})
        (publish! mbus :query-update {:value query}))
      (publish! mbus :query-form-hide {}))))

;; Incoming event handlers

(defn- on-show
  [mbus]
  (let [id (value (sel1 :#qf-id))]
    (set-html! (sel1 :#qf-save) (if (empty? id) "create" "save")))
  (show! (sel1 :#query-form-container)))

(defn- on-hide
  [mbus]
  (hide! (sel1 :#query-form-container))
  (set-value! (sel1 :#qf-id) "")
  (set-value! (sel1 :#qf-desc) "")
  (set-value! (sel1 :#qf-sql) "")
  (set-html! (sel1 :#qf-save) "create"))

(defn- on-update
  [mbus {:keys [id sql description]}]
  (set-value! (sel1 :#qf-id) id)
  (set-value! (sel1 :#qf-desc) description)
  (set-value! (sel1 :#qf-sql) sql)
  (set-html! (sel1 :#qf-save) "save"))

(defn- mk-template
  [mbus]
  (let [t (template)]
    (listen! [t :#qf-save] :click (on-save mbus))
    (listen! [t :#qf-cancel] :click (on-cancel mbus))
    t))

(def ^:private subscriptions
  {:query-form-show (fn [mbus msg] (on-show mbus))
   :query-form-hide (fn [mbus msg] (on-hide mbus))
   :query-get (fn [mbus msg] (on-update mbus (:value msg)))})

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn mk-view!
  [mbus]
  (view/mk-view mbus mk-template subscriptions))
