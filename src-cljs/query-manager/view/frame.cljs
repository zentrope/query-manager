(ns query-manager.view.frame
  (:use-macros [dommy.macros :only [node sel1]])
  (:require [cljs.core.async :as async :refer [put!]]
            [dommy.core :as dom]))

(defn- container-frame
  []
  (node [:div#container
         [:div#left]
         [:div#right
          [:div#title-bar
           [:span#title-text "Query Manager"]
           [:span#title-version "Vers 2"]]
          [:div#status-bar
           [:div.status-buttons
            [:button#sb-ar {:class "not-showing"} "archive"]
            [:button#sb-db {:class "not-showing"} "db"]]]]]))

(defn- on-db-button-click!
  [queue]
  (fn [e] (put! queue [:db-form-show {}])))

(defn- on-archive-button-click!
  [queue]
  (fn [e] (put! queue [:archive {}])))

(defn- make-template
  [queue]
  (let [body (container-frame)]
    (dom/listen! [body :#sb-db] :click (on-db-button-click! queue))
    (dom/listen! [body :#sb-ar] :click (on-archive-button-click! queue))
    body))

;;-----------------------------------------------------------------------------

(defn set-db!
  [{:keys [type host] :as db}]
  (dom/set-html! (sel1 :#title-text) (str host " &mdash; " type)))

(defn show!
  [queue]
  (dom/replace-contents! (sel1 :body) (make-template queue)))
