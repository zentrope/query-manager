(ns query-manager.view.upload-area
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-html! set-text! listen!]]
            [goog.events.FileDropHandler :as dh]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private template
  (node [:div#upload-area
         [:div
          [:p#ua-notice "Events:"]
          [:p "Drop your file here."]
          [:button#text-clear "Clear"]
          [:pre#text-data {:style {:text-align "left"}}]]]))

(defn- show-text
  [data]
  (set-text! (sel1 :#text-data) data))

(defn- clear-text
  []
  (set-text! (sel1 :#text-data) ""))

(defn- on-text-loaded
  [e]
  (show-text (.-result (.-target e))))

(defn- on-drop
  [e]
  (.preventDefault e)
  (.stopPropagation e)
  (let [f (aget (.-files (.-dataTransfer e)) 0)
        desc (str "file: " (.-name f) ", size: " (.-size f))]
    (set-html! (sel1 :#ua-notice) desc)
    (if (.-FileReader js/window)
      (let [rdr (js/FileReader.)]
        (set! (.-onload rdr) on-text-loaded)
        (.readAsText rdr f))
     (.log js/console "no file reader"))))

(defn- on-dragover
  [e]
  (.preventDefault e)
  (.stopPropagation e))

(defn- mk-template
  [broadcast]
  (listen! [template :#text-clear] :click clear-text)
  (listen! [template :#upload-area :div] :dragover on-dragover)
  (listen! [template :#upload-area :div] :drop on-drop)
  template)

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  [broadcast]
  (mk-template broadcast))

(defn events
  []
  [])

(defn recv
  [[topic event]]
  true)
