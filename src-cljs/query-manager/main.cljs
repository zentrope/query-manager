(ns query-manager.main
  (:use-macros [dommy.macros :only [sel1]])
  (:require [dommy.core :refer [replace-contents! listen! append!]]

            ;; Events
            [query-manager.event :as event]

            ;; Views
            [query-manager.view.status-bar :as status-bar]
            [query-manager.view.title-bar :as title-bar]
            [query-manager.view.db-panel :as db-panel]
            [query-manager.view.db-form :as db-form]
            [query-manager.view.job-panel :as job-panel]
            [query-manager.view.job-viewer :as job-viewer]
            [query-manager.view.upload-panel :as upload-panel]
            [query-manager.view.query-panel :as query-panel]
            [query-manager.view.query-form :as query-form]
            [query-manager.view.error-panel :as error-panel]

            ;; Processes
            [query-manager.proc.clock :as clock]
            [query-manager.proc.job-monitor :as job-monitor]

            ;; Network
            [query-manager.view.export :as export]
            [query-manager.net :as net]))

;;-----------------------------------------------------------------------------
;; Main
;;-----------------------------------------------------------------------------

(defn main
  []
  (.log js/console "loading")

  ;; Composite UI components
  (replace-contents! (sel1 :body) nil)

  (append! (sel1 :body)

           ;; UI functions and status
           (title-bar/dom event/broadcast)
           (status-bar/dom event/broadcast)

           ;; Popup forms
           (db-form/dom event/broadcast)
           (query-form/dom event/broadcast)
           (job-viewer/dom event/broadcast)

           ;; Content panels
;;           (export-panel/dom event/broadcast)
           (db-panel/dom event/broadcast)
           (job-panel/dom event/broadcast)
           (query-panel/dom event/broadcast)
           (upload-panel/dom event/broadcast)
           (error-panel/dom event/broadcast))


  ;; Register UI event subscriptions
  (event/map-subs status-bar/recv (status-bar/events))
  (event/map-subs title-bar/recv (title-bar/events))
  (event/map-subs db-panel/recv (db-panel/events))
  (event/map-subs db-form/recv (db-form/events))
  (event/map-subs job-panel/recv (job-panel/events))
  (event/map-subs upload-panel/recv (upload-panel/events))
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

  ;; Just for fun, for now.
  (listen! (sel1 :html) :mousemove
           (fn [e]
             (let [x (.-clientX e)
                   y (.-clientY e)]
               (event/broadcast [:mousemove {:value [x y]}]))))

  ;; Start background processes
  (clock/start event/broadcast)
  (job-monitor/start event/broadcast)

  ;; Init
  (event/broadcast [:db-poke {}])
  (event/broadcast [:queries-poke {}])
  (event/broadcast [:jobs-poke {}])

  (.log js/console " - loaded"))

(set! (.-onload js/window) main)
