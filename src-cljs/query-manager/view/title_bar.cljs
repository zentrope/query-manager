(ns query-manager.view.title-bar
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-html!]]
            [query-manager.utils :refer [das]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#title-bar
         [:span#title-text "Query Manager"]
         [:span#title-version "Vers 0.1"]
         [:span#title-clock "-"]]))

(defn- date-str
  [date]
  (str (das :hour date) ":" (das :minute date) ":" (das :second date)))

(defn- set-clock!
  [date]
  (set-html! (sel1 :#title-clock) (date-str date)))

(defn- mk-template
  [channel]
  (template))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  [channel]
  (mk-template channel))

(defn topics
  []
  [:clock])

(defn recv
  [broadcast [type event]]
  (case type
    :clock (set-clock! (js/Date. (:value event)))
    true))
