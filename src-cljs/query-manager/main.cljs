(ns query-manager.main
  (:use-macros [dommy.macros :only [sel1]])
  (:require [dommy.core :refer [replace-contents! listen! append!]]

            ;; Events
            [query-manager.event :as event]

            ;; Views
            [query-manager.view.status-bar :as status-bar]
            [query-manager.view.title-bar :as title-bar]
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
           (title-bar/dom event/publish!)
           (status-bar/dom event/publish!)

           ;; Popup forms (or similar)
           (db-form/dom event/publish!)
           (query-form/dom event/publish!)
           (job-viewer/dom event/publish!)
           (import/dom event/publish!)

           ;; Content panels
           (job-panel/dom event/publish!)
           (query-panel/dom event/publish!)
           (error-panel/dom event/publish!))

  ;; Register UI event subscriptions
  (event/subscribe! status-bar/recv (status-bar/topics))
  (event/subscribe! title-bar/recv (title-bar/topics))
  (event/subscribe! db-form/recv (db-form/topics))
  (event/subscribe! job-panel/recv (job-panel/topics))
  (event/subscribe! query-panel/recv (query-panel/topics))
  (event/subscribe! query-form/recv (query-form/topics))
  (event/subscribe! error-panel/recv (error-panel/topics))
  (event/subscribe! job-viewer/recv (job-viewer/topics))

  ;; Turn off the stuff we don't want to see right away.
  (event/publish! [:error-panel-toggle {}])
  (event/publish! [:upload-panel-toggle {}])

  ;; Register non-UI event subscribers
  (event/subscribe! net/recv (net/topics))
  (event/subscribe! export/recv (export/topics))
  (event/subscribe! import/recv (import/topics))

  ;; Just for fun, for now.
  (listen! (sel1 :html) :mousemove
           (fn [e]
             (let [x (.-clientX e)
                   y (.-clientY e)]
               (event/publish! [:mousemove {:value [x y]}]))))

  ;; Start background processes
  (proc/start event/publish!)

  ;; Init
  (event/publish! [:db-poke {}])
  (event/publish! [:queries-poke {}])
  (event/publish! [:jobs-poke {}])

  ;; Only bring up the DB connection form if it hasn't been changed
  ;; since the app started up.

  (letfn [(db-prompter [c [topic {:keys [value]}]]
            (when-not (:updated value)
              (c [:db-form-show {}])
              (event/unsubscribe! db-prompter :db-change)))]
    (event/subscribe! db-prompter [:db-change]))

  (.log js/console " - loaded"))

;; Invoke when page loaded.
(set! (.-onload js/window) main)
