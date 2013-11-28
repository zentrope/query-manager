(ns query-manager.export
  (:use-macros [cljs.core.async.macros :only [go-loop]])
  (:require [cljs.core.async :as async]
            [query-manager.utils :as utils]))

(defn- export!
  []
  (set! (.-location js/window) "/qman/queries/download"))

(defn- block-loop
  [input-ch]
  (go-loop []
    (when-let [msg (async/<! input-ch)]
      (case (first msg)
        :export-queries (export!)
        :noop)
      (recur))))

;;-----------------------------------------------------------------------------

(defn instance
  []
  (let [send-ch (utils/subscriber-ch :export-queries)]
    {:send send-ch :block (block-loop send-ch)}))
