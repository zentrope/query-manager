(ns query-manager.import
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :as dom]
            [cljs.core.async :as async]
            [cljs.reader :as reader]
            [clojure.string :as string]))

;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div.import-container {:style {:display "none"}}]))

(defn- prep-initial-drag!
  []
  (dom/listen-once! (sel1 :html)
                    :dragenter (fn [e] (dom/show! (sel1 :.import-container)))))

(defn- hide-container!
  []
  (dom/hide! (sel1 :.import-container))
  (prep-initial-drag!))

(defn- on-text-loaded
  [output-ch]
  (fn [e]
    (let [source (.-result (.-target e))]
      (try
        (let [queries (reader/read-string source)]
          (doseq [q queries]
            (async/put! output-ch [:query-save {:value q}])))
        (catch js/Error e
          (.log js/console "ERROR:" e))
        (finally
          (hide-container!))))))

(defn- on-drop
  [output-ch]
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (let [f (aget (.-files (.-dataTransfer e)) 0)]
      (if (.-FileReader js/window)
        (let [rdr (js/FileReader.)]
          (set! (.-onload rdr) (on-text-loaded output-ch))
          (.readAsText rdr f))
        (.log js/console "no file reader")))))

(defn- target-class
  [e]
  (.-className (.-target e)))

(defn- on-dragover
  []
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (let [class (target-class e)]
      (when-not (= class "import-container")
        (when-not (dom/hidden? (sel1 :.import-container))
          ;; (.log js/console "class" class)
          (hide-container!))))))

(defn- on-drag-leave
  []
  (fn [e]
    (if (= (target-class e) "import-container")
      (hide-container!))))

(defn- mk-template
  [output-ch]
  (prep-initial-drag!)
  (dom/listen! (sel1 :html) :dragover (on-dragover))
  (dom/listen! (sel1 :html) :dragleave (on-drag-leave))
  (dom/listen! (sel1 :html) :drop (fn [e]
                                (if (= (target-class e) "import-container")
                                  ((on-drop output-ch) e))))
  (template))

;;-----------------------------------------------------------------------------

(defn show!
  [queue place]
  (dom/append! (sel1 place) (mk-template queue)))
