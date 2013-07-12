(ns query-manager.view.upload-panel
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [show! hide! add-class! set-html! listen! listen-once! hidden?]]
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
  [channel]
  (fn [e]
    (let [source (.-result (.-target e))]
      (try
        (let [queries (reader/read-string source)]
          (doseq [q queries]
            (channel [:query-save {:value q}])))
        (catch js/Error e
          (.log js/console "ERROR:" e))
        (finally
          (hide-container!))))))

(defn- on-drop
  [channel]
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (let [f (aget (.-files (.-dataTransfer e)) 0)]
      (if (.-FileReader js/window)
        (let [rdr (js/FileReader.)]
          (set! (.-onload rdr) (on-text-loaded channel))
          (.readAsText rdr f))
        (.log js/console "no file reader")))
    ;;(hide-container!)
    ))

(defn- target-class
  [e]
  (.-className (.-target e)))

(defn- on-dragover
  [channel]
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
  [channel]
  (prep-initial-drag!)
  (listen! (sel1 :html) :dragover (on-dragover channel))
  (listen! (sel1 :html) :dragleave (on-drag-leave))
  (listen! (sel1 :html) :drop (fn [e]
                                (if (= (target-class e) "import-container")
                                  ((on-drop channel) e))))
  (template))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  [channel]
  (mk-template channel))

(defn events
  []
  [])

(defn recv
  [channel [topic event]]
  true)
