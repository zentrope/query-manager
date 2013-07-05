(ns query-manager.view.upload-panel
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-html! set-text! listen! replace-contents!]]
            [cljs.reader :as reader]
            [clojure.string :as string]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private template
  (node [:div#upload-area
         [:div#upload-lz
          [:p#upload-doc "Drop your file here."]]]))

(defn- on-text-loaded
  [broadcast e]
  (let [source (.-result (.-target e))]
    (try
      (let [queries (reader/read-string source)]
        (doseq [q queries]
          (broadcast [:query-save {:value q}])))
      (catch js/Error e
        (.log js/console "ERROR:" e)
        (set-html! (sel1 :#upload-doc) "Invalid file format. Sorry!")))))

(defn- on-drop
  [broadcast e]
  (.preventDefault e)
  (.stopPropagation e)
  (let [f (aget (.-files (.-dataTransfer e)) 0)]
    (if (.-FileReader js/window)
      (let [rdr (js/FileReader.)]
        (set! (.-onload rdr) (partial on-text-loaded broadcast))
        (.readAsText rdr f))
     (.log js/console "no file reader"))))

(defn- on-dragover
  [e]
  (.preventDefault e)
  (.stopPropagation e))

(defn- mk-template
  [broadcast]
  (listen! [template :#upload-area :div] :dragover on-dragover)
  (listen! [template :#upload-area :div] :drop (partial on-drop broadcast))
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
  [broadcast [topic event]]
  true)
