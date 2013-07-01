(ns query-manager.view.title-bar
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-html!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private template
  (node [:div#title-bar
         [:span#title-text "Query Manager"]
         [:span#title-version "Vers 0.1"]
         [:span#title-clock "-"]]))

(defn- as
  [part date]
  (let [num (case part
              :month (inc (.getMonth date))
              :day (.getDate date)
              :hour (.getHours date)
              :minute (.getMinutes date)
              :second (.getSeconds date)
              :year (.getFullYear date)
              0)]
    (if (< num 10)
      (str "0" num)
      (str num))))

(defn- date-str
  [date]
  (str (as :hour date) ":" (as :minute date) ":" (as :second date)))

(defn- set-clock!
  [date]
  (set-html! (sel1 :#title-clock) (date-str date)))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  []
  template)

(defn events
  []
  [:clock])

(defn recv
  [[type event]]
  (case type
    :clock (set-clock! (js/Date. (:value event)))
    true))
