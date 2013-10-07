(ns query-manager.main
  (:use-macros [dommy.macros :only [sel1]])
  (:require [dommy.core :refer [replace-contents! listen! append!]]
            ;;
            ;; Events
            ;;
            [query-manager.event :as event]
            [query-manager.protocols :as proto]
            ;;
            ;; Views
            ;;
            [query-manager.view.status-bar :as status-bar]
            [query-manager.view.title-bar :as title-bar]
            [query-manager.view.job-panel :as job-panel]
            [query-manager.view.query-panel :as query-panel]
            [query-manager.view.error-panel :as error-panel]
            [query-manager.view.db-form :as db-form]
            [query-manager.view.query-form :as query-form]
            [query-manager.view.job-viewer :as job-viewer]
            ;;
            ;; IO
            ;;
            [query-manager.proc :as proc]
            [query-manager.import :as import]
            [query-manager.export :as export]
            [query-manager.net :as net]))

;;-----------------------------------------------------------------------------
;; Main
;;-----------------------------------------------------------------------------

(def ^:private bus (event/mk-event-bus))
(def ^:private statusBar (status-bar/mk-view! bus))
(def ^:private titleBar (title-bar/mk-view! bus))
(def ^:private errorPanel (error-panel/mk-view! bus))
(def ^:private jobPanel (job-panel/mk-view! bus))
(def ^:private queryPanel (query-panel/mk-view! bus))
(def ^:private dbForm (db-form/mk-view! bus))
(def ^:private queryForm (query-form/mk-view! bus))
(def ^:private jobViewer (job-viewer/mk-view! bus))
(def ^:private importPanel (import/mk-view! bus))

(defn main
  []
  (.log js/console "loading")

  ;; Clear body
  (replace-contents! (sel1 :body) [:div#container
                                   [:div#left]
                                   [:div#right]])

  ;; Composite UI components
  (append! (sel1 :#left)
           (proto/dom queryPanel))

  (append! (sel1 :#right)

           (proto/dom titleBar)
           (proto/dom statusBar)
           (proto/dom jobPanel)

           (proto/dom errorPanel)

           (proto/dom dbForm)
           (proto/dom queryForm)
           (proto/dom jobViewer)
           (proto/dom importPanel))

  ;; Turn off the stuff we don't want to see right away.
  (proto/publish! bus :error-panel-toggle {})

  ;; Register non-UI event subscribers
  (net/init! bus)
  (export/init! bus)

  ;; Just for fun, for now.
  (listen! (sel1 :html) :mousemove
           (fn [e]
             (let [x (.-clientX e)
                   y (.-clientY e)]
               (proto/publish! bus :mousemove {:value [x y]}))))

  ;; Start background processes
  (proc/start bus)

  ;; Init
  (proto/publish! bus :db-poke {})
  (proto/publish! bus :db-poke {})
  (proto/publish! bus :queries-poke {})
  (proto/publish! bus :jobs-poke {})

  ;; Only bring up the DB connection form if it hasn't been changed
  ;; since the app started up.

  (letfn [(handler [mbus {:keys [value]}]
            (when-not (:updated value)
              (proto/publish! mbus :db-form-show {})
              (proto/unsubscribe! mbus :db-change handler)))]
    (proto/subscribe! bus :db-change handler))

  (.log js/console " - loaded"))

;; Invoke when page loaded.
(set! (.-onload js/window) main)
