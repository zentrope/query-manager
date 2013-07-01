(ns query-manager.view.dev-area
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-html! listen!]]
            [query-manager.net :as net]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private template
  (node [:div#container
         [:p "Client not yet implemented."]
         [:p "This is just a place to invoke events while under development."]
         [:p [:button#test-button "Test"]]
         [:p.flash "~"]]))

(defn- on-success
  [delegate]
  (fn [db-info]
    (delegate [:loading {:value false}])
    (delegate [:db-change {:value (js->clj db-info :keywordize-keys true)}])))

(defn- on-failure
  [delegate]
  (fn [error]
    (delegate [:loading {:value false}])
    (delegate [:net-error {:value error}])))

(defn- on-click
  [delegate]
  (delegate [:loading {:value true}])
  (net/get-db (on-success delegate)
              (on-failure delegate)))

(defn- mk-template
  [delegate]
  (listen! [template :#test-button] :click (fn [] (on-click delegate)))
  template)

(defn- set-flash!
  [db-info]
  (set-html! (sel1 :.flash) db-info))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  [delegate]
  (mk-template delegate))

(defn events
  []
  [:db-change :net-error])

(defn recv
  [[topic event]]
  (case topic
    :db-change (set-flash! (:value event))
    :net-error (let [e (:value event)]
                 (set-flash! (str "failed: " (:status e) " -> " (:reason e))))
    true))
