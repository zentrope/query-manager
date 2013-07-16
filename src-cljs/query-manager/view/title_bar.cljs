(ns query-manager.view.title-bar
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-html!]]
            [query-manager.view :refer [mk-view]]
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
  [mbus]
  (template))

(def ^:private subscriptions
  {:clock (fn [mbus msg] (set-clock! (js/Date. (:value msg))))})

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn mk-view!
  [mbus]
  (mk-view mbus mk-template subscriptions))
