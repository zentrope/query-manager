(ns query-manager.main
  (:use-macros [dommy.macros :only [sel sel1 node]])
  (:require [dommy.core :refer [set-html! replace! replace-contents!
                                listen! append!]]
            [query-manager.view.status-bar :as status-bar]
            [query-manager.view.title-bar :as title-bar]
            [query-manager.view.dev-area :as dev-area]
            [query-manager.net :refer [dump get-db]]))

;;-----------------------------------------------------------------------------
;; Events
;;-----------------------------------------------------------------------------

(def ^:private subscribers (atom {}))

(defn- add-sub
  [event-type f]
  (swap! subscribers (fn [s]
                       (let [orig (or (event-type s) #{})]
                         (assoc s event-type (conj orig f))))))

(defn- map-subs
  [f event-types]
  (doseq [e event-types]
    (add-sub e f)))

(defn- now
  []
  (.getTime (js/Date.)))

(defn- send-event
  [event]
  (let [topic (first event)
        data (assoc (second event) :timestamp (now))]
    (when-let [subscribers (topic @subscribers)]
      (doseq [s subscribers]
        (s event)))))

;;-----------------------------------------------------------------------------
;; Background Procs
;;-----------------------------------------------------------------------------

(defn- start-clock
  []
  (js/setTimeout (fn []
                   (send-event [:clock {:value (.getTime (js/Date.))}])
                   (start-clock)) 1000))

;;-----------------------------------------------------------------------------

(defn main
  []
  (.log js/console "loading")

  ;; Composite UI components
  (replace-contents! (sel1 :body) (title-bar/dom))
  (append! (sel1 :body) (dev-area/dom send-event))
  (append! (sel1 :body) (status-bar/dom))


  ;; Register UI events
  (map-subs status-bar/recv (status-bar/events))
  (map-subs title-bar/recv (title-bar/events))
  (map-subs dev-area/recv (dev-area/events))

  (listen! (sel1 :body) :mousemove
           (fn [e]
             (let [x (.-clientX e)
                   y (.-clientY e)]
               (send-event [:mousemove {:value [x y]}]))))

  ;; Start background processes
  (start-clock)

  (.log js/console "loaded"))

(set! (.-onload js/window) main)
