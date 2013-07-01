(ns query-manager.view.status-bar
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace! set-html!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private template
  (node [:div#status-bar
         [:div#db-info "conn: none"]
         [:div#loading ""]
         [:div#mouse-coords
          "{:x " [:span#mouse-x "0"] " :y " [:span#mouse-y "0"] "}"]]))

(defn- set-mouse-coords!
  [[x y]]
  (set-html! (sel1 :#mouse-x) x)
  (set-html! (sel1 :#mouse-y) y))

(defn- set-db-info!
  [{:keys [type host] :as db}]
  (replace! (sel1 :#db-info) (node [:div#db-info
                                    "conn: "
                                    [:span#db-type type]
                                    " on "
                                    [:span#db-host host]])))

(defn- set-loading!
  [toggle]
  (if toggle
    (set-html! (sel1 :#loading) "loading...")
    (set-html! (sel1 :#loading) "")))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  []
  template)

(defn events
  []
  [:loading :db-change :mousemove])

(defn recv
  [[type event]]
  (case type
    :loading (set-loading! (:value event))
    :db-change (set-db-info! (:value event))
    :mousemove (set-mouse-coords! (:value event))
    nil))
