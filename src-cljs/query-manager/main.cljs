(ns query-manager.main
  (:use-macros [dommy.macros :only [sel1]])
  (:require [dommy.core :refer [replace-contents! listen! append!]]

            ;; Events
            [query-manager.event :as event]

            ;; Views
            [query-manager.view.status-bar :as status-bar]
            [query-manager.view.title-bar :as title-bar]
            [query-manager.view.db-panel :as db-panel]
            [query-manager.view.job-panel :as job-panel]
            [query-manager.view.query-panel :as query-panel]
            [query-manager.view.error-panel :as error-panel]
            [query-manager.view.db-form :as db-form]
            [query-manager.view.query-form :as query-form]
            [query-manager.view.job-viewer :as job-viewer]

            ;; IO
            [query-manager.proc :as proc]
            [query-manager.import :as import]
            [query-manager.export :as export]
            [query-manager.net :as net]))

;;-----------------------------------------------------------------------------
;; Main
;;-----------------------------------------------------------------------------

(defn main
  []
  (.log js/console "loading")

  ;; Clear body
  (replace-contents! (sel1 :body) nil)

  ;; Composite UI components
  (append! (sel1 :body)

           ;; UI functions and status
           (title-bar/dom event/broadcast)
           (status-bar/dom event/broadcast)

           ;; Popup forms (or similar)
           (db-form/dom event/broadcast)
           (query-form/dom event/broadcast)
           (job-viewer/dom event/broadcast)
           (import/dom event/broadcast)

           ;; Content panels
           (db-panel/dom event/broadcast)
           (job-panel/dom event/broadcast)
           (query-panel/dom event/broadcast)
           (error-panel/dom event/broadcast))

  ;; Register UI event subscriptions
  (event/map-subs status-bar/recv (status-bar/events))
  (event/map-subs title-bar/recv (title-bar/events))
  (event/map-subs db-panel/recv (db-panel/events))
  (event/map-subs db-form/recv (db-form/events))
  (event/map-subs job-panel/recv (job-panel/events))
  (event/map-subs query-panel/recv (query-panel/events))
  (event/map-subs query-form/recv (query-form/events))
  (event/map-subs error-panel/recv (error-panel/events))
  (event/map-subs job-viewer/recv (job-viewer/events))

  ;; Turn off the stuff we don't want to see right away.
  (event/broadcast [:error-panel-toggle {}])
  (event/broadcast [:upload-panel-toggle {}])

  ;; Register non-UI event subscribers
  (event/map-subs net/recv (net/events))
  (event/map-subs export/recv (export/events))
  (event/map-subs import/recv (import/events))

  ;; Just for fun, for now.
  (listen! (sel1 :html) :mousemove
           (fn [e]
             (let [x (.-clientX e)
                   y (.-clientY e)]
               (event/broadcast [:mousemove {:value [x y]}]))))

  ;; Start background processes
  (proc/start event/broadcast)

  ;; Init
  (event/broadcast [:db-poke {}])
  (event/broadcast [:queries-poke {}])
  (event/broadcast [:jobs-poke {}])

  (.log js/console " - loaded"))

;; Invoke when page loaded.
(set! (.-onload js/window) main)
