(ns query-manager.main
  (:use-macros [dommy.macros :only [sel1]])
  (:require [dommy.core :refer [replace-contents! listen! append!]]

            ;; Events
            [query-manager.event :as event]

            ;; Views
            [query-manager.view.status-bar :as status-bar]
            [query-manager.view.title-bar :as title-bar]
            [query-manager.view.dev-area :as dev-area]
            [query-manager.view.upload-area :as upload-area]
            [query-manager.view.query-panel :as query-panel]

            ;; Processes
            [query-manager.proc.clock :as clock]

            ;; Network
            [query-manager.net :as net]))

;;-----------------------------------------------------------------------------
;; Main
;;-----------------------------------------------------------------------------

(defn main
  []
  (.log js/console "loading")

  ;; Composite UI components
  (replace-contents! (sel1 :body) (title-bar/dom))
  (append! (sel1 :body) (dev-area/dom event/send-event))
  (append! (sel1 :body) (query-panel/dom event/send-event))
  (append! (sel1 :body) (upload-area/dom event/send-event))
  (append! (sel1 :body) (status-bar/dom))

  ;; Register UI event subscriptions
  (event/map-subs status-bar/recv (status-bar/events))
  (event/map-subs title-bar/recv (title-bar/events))
  (event/map-subs dev-area/recv (dev-area/events))
  (event/map-subs upload-area/recv (upload-area/events))
  (event/map-subs query-panel/recv (query-panel/events))

  ;; Just for fun, for now.
  (listen! (sel1 :html) :mousemove
           (fn [e]
             (let [x (.-clientX e)
                   y (.-clientY e)]
               (event/send-event [:mousemove {:value [x y]}]))))

  ;; Start background processes
  (clock/start event/send-event)

  ;; Init
  (net/poke-db event/send-event)
  (net/poke-query event/send-event)

  (.log js/console "loaded"))

(set! (.-onload js/window) main)
