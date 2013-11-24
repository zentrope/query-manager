(ns query-manager.view.title-bar
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-html!]]
            [query-manager.view :as view]
            [query-manager.utils :refer [das]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#title-bar
         [:span#title-text "Query Manager"]
         [:span#title-version "Vers 2"]
         [:span#title-clock "-"]]))

(defn- set-db-info!
  [{:keys [type host] :as db}]
  (set-html! (sel1 :#title-text) (str host " &mdash; " type)))

(defn- mk-template
  [mbus]
  (template))

(def ^:private subscriptions
  {:db-change (fn [mbus msg] (set-db-info! (:value msg)))})

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn mk-view!
  [mbus]
  (view/mk-view mbus mk-template subscriptions))
