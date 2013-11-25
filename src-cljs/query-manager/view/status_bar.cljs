(ns query-manager.view.status-bar
  ;;
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace! listen! set-html! toggle-class!
                                add-class! remove-class!]]
            [cljs.core.async :as async :refer [chan]]
            [query-manager.view :as view]
            [query-manager.protocols :refer [publish!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#status-bar
         [:div.status-buttons
          [:button#sb-db {:class "not-showing"} "db"]
          [:button#sb-err "log"]]
         [:div#mouse-coords
          "(" [:span#sb-mouse-x "0"] ":" [:span#sb-mouse-y "0"] ")"]]))

(defn- on-toggle!
  [mbus event-channel topic]
  (fn [e]
    (publish! mbus topic {})
    (async/put! event-channel [topic {}])))

(defn- mk-template
  [event-channel mbus]
  (let [t (template)]
    (listen! [t :#sb-db] :click (on-toggle! mbus event-channel :db-form-show))
    (listen! [t :#sb-err] :click (on-toggle! mbus event-channel :error-panel-toggle))
    t))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(def ^:private subscriptions
  {:db-form-hide (fn [mbus msg] (add-class! (sel1 :#sb-db) "not-showing"))
   :db-form-show (fn [mbus msg] (remove-class! (sel1 :#sb-db) "not-showing"))
   :error-panel-toggle (fn [mbus msg] (toggle-class! (sel1 :#sb-err) "not-showing"))})

;; Just for now. Eventually, a "view" will be a data structure
;; containing channels and so on. This just lets me figure out
;; of a "channel-per-view" is workable.

(def ^:private event-channel (async/chan))

(defn get-channel
  []
  event-channel)

(defn mk-view!
  [mbus]
  (view/mk-view mbus (partial mk-template event-channel) subscriptions))
