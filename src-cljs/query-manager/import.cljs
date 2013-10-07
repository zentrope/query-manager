(ns query-manager.import
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [show! hide! listen! listen-once! hidden?]]
            [query-manager.protocols :as proto]
            [query-manager.view :as view]
            [cljs.reader :as reader]
            [clojure.string :as string]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div.import-container {:style {:display "none"}}]))

(defn- prep-initial-drag!
  []
  (listen-once! (sel1 :html) :dragenter (fn [e] (show! (sel1 :.import-container)))))

(defn- hide-container!
  []
  (hide! (sel1 :.import-container))
  (prep-initial-drag!))

(defn- on-text-loaded
  [mbus]
  (fn [e]
    (let [source (.-result (.-target e))]
      (try
        (let [queries (reader/read-string source)]
          (doseq [q queries]
            (proto/publish! mbus :query-save {:value q})))
        (catch js/Error e
          (.log js/console "ERROR:" e))
        (finally
          (hide-container!))))))

(defn- on-drop
  [mbus]
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (let [f (aget (.-files (.-dataTransfer e)) 0)]
      (if (.-FileReader js/window)
        (let [rdr (js/FileReader.)]
          (set! (.-onload rdr) (on-text-loaded mbus))
          (.readAsText rdr f))
        (.log js/console "no file reader")))))

(defn- target-class
  [e]
  (.-className (.-target e)))

(defn- on-dragover
  [mbus]
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (let [class (target-class e)]
      (when-not (= class "import-container")
        (when-not (hidden? (sel1 :.import-container))
          ;; (.log js/console "class" class)
          (hide-container!))))))

(defn- on-drag-leave
  []
  (fn [e]
    (if (= (target-class e) "import-container")
      (hide-container!))))

(defn- mk-template
  [mbus]
  (prep-initial-drag!)
  (listen! (sel1 :html) :dragover (on-dragover mbus))
  (listen! (sel1 :html) :dragleave (on-drag-leave))
  (listen! (sel1 :html) :drop (fn [e]
                                (if (= (target-class e) "import-container")
                                  ((on-drop mbus) e))))
  (template))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn mk-view!
  [mbus]
  (view/mk-view mbus mk-template {}))
