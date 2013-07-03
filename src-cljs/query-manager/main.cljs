(ns query-manager.main
  (:use-macros [dommy.macros :only [sel1]])
  (:require [dommy.core :refer [replace-contents! listen! append!]]
            [query-manager.view.status-bar :as status-bar]
            [query-manager.view.title-bar :as title-bar]
            [query-manager.view.dev-area :as dev-area]
            [query-manager.view.upload-area :as upload-area]
            [query-manager.view.query-panel :as query-panel]
            [query-manager.proc.clock :as clock]
            [query-manager.net :as net]))

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
;; Main
;;-----------------------------------------------------------------------------

(defn main
  []
  (.log js/console "loading")

  ;; Composite UI components
  (replace-contents! (sel1 :body) (title-bar/dom))
  (append! (sel1 :body) (dev-area/dom send-event))
  (append! (sel1 :body) (query-panel/dom send-event))
  (append! (sel1 :body) (upload-area/dom send-event))
  (append! (sel1 :body) (status-bar/dom))

  ;; Register UI events
  (map-subs status-bar/recv (status-bar/events))
  (map-subs title-bar/recv (title-bar/events))
  (map-subs dev-area/recv (dev-area/events))
  (map-subs upload-area/recv (upload-area/events))
  (map-subs query-panel/recv (query-panel/events))

  (listen! (sel1 :body) :mousemove
           (fn [e]
             (let [x (.-clientX e)
                   y (.-clientY e)]
               (send-event [:mousemove {:value [x y]}]))))

  ;; Start background processes
  (clock/start send-event)

  ;; Init
  (net/poke-db send-event)
  (net/poke-query send-event)

  (.log js/console "loaded"))

(set! (.-onload js/window) main)
