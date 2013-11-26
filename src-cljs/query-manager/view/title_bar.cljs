(ns query-manager.view.title-bar
  (:use-macros [dommy.macros :only [sel1 node]]
               [cljs.core.async.macros :only [go-loop]])
  (:require [dommy.core :as dom]
            [cljs.core.async :as async]))

;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#title-bar
         [:span#title-text "Query Manager"]
         [:span#title-version "Vers 2"]
         [:span#title-clock "-"]]))

(defn- set-db-info!
  [{:keys [type host] :as db}]
  (dom/set-html! (sel1 :#title-text) (str host " &mdash; " type)))

(defn- mk-template
  []
  (template))

(defn process
  [[topic msg]]
  (case topic
    :db-change (set-db-info! (:value msg))
    :noop))

(defn- block-loop
  [ch]
  (go-loop []
      (when-let [msg (async/<! ch)]
        (process msg)
        (recur))))

;;-----------------------------------------------------------------------------

(defn instance
  []
  (let [recv-ch (async/chan)
        block (block-loop recv-ch)]
    {:recv recv-ch
     :send nil
     :view (mk-template)
     :block block}))
