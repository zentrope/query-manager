(ns query-manager.main
  ;;
  ;; I wrote this before I knew about core.async. So, time to
  ;; rewrite. ;) If you see this notice, it means I'm in the process
  ;; of attempting to do that without breaking anything.
  ;;
  (:use-macros [dommy.macros :only [sel1]]
               [cljs.core.async.macros :only [go-loop]])
  ;;
  (:require [dommy.core :refer [replace-contents! listen! append! set-html!]]
            [cljs.core.async :as async :refer [put! timeout <! chan]]
            ;;
            ;;
            [query-manager.utils :refer [das]]
            ;;
            ;; Events
            ;;
            [query-manager.event :as event]
            [query-manager.protocols :as proto]
            ;;
            ;; Views
            ;;
            [query-manager.view :as view]
            ;;
            [query-manager.view.query-panel :as query-panel]
            [query-manager.view.error-panel :as error-panel]
            [query-manager.view.query-form :as query-form]
            [query-manager.view.job-viewer :as job-viewer]
            ;;
            ;; IO
            ;;
            [query-manager.import :as import]
            [query-manager.export :as export]
            [query-manager.net :as net]))

;;-----------------------------------------------------------------------------
;; Daemons
;;-----------------------------------------------------------------------------

(defn- event-loop!
  [event-fn millis]
  (go-loop []
    (<! (timeout millis))
    (event-fn)
    (recur)))

(defn- clock-loop!
  []
  (go-loop []
    (<! (timeout 1000))
    (let [date (js/Date.)
          pretty (str (das :hour date) ":"
                      (das :minute date) ":"
                      (das :second date))]
      (set-html! (sel1 :#title-clock) pretty)
      (recur))))

(defn- mouse-loop!
  []
  (let [ch (chan)]
    (go-loop []
      (when-let [[x y] (<! ch)]
        (set-html! (sel1 :#sb-mouse-x) x)
        (set-html! (sel1 :#sb-mouse-y) y)
        (recur)))
    (listen! (sel1 :html) :mousemove
             (fn [e]
               (let [x (.-clientX e)
                     y (.-clientY e)]
                 (put! ch [x y]))))))

(defn- start-daemons!
  [event-ch]
  (clock-loop!)
  (mouse-loop!)
  (event-loop! (fn [] (async/put! event-ch [:jobs-poke {}])) 2000)
  (event-loop! (fn [] (async/put! event-ch [:queries-poke {}])) 2000))

;;-----------------------------------------------------------------------------

(defn- update-state!
  [state outputs msg]
  (doseq [o outputs]
    (put! o msg))
  state)

(defn- app-loop!
  [initial-state inputs outputs]
  (go-loop [state initial-state]
    (let [[msg ch] (alts! inputs)]
      (when-not (nil? msg)
;;        (.log js/console (str msg))
        (let [new-state (update-state! state outputs msg)]
          (recur new-state)))
      (.log js/console "app-loop terminated"))))

;;-----------------------------------------------------------------------------
;; Main
;;-----------------------------------------------------------------------------

(def ^:private bus (event/mk-event-bus))
(def ^:private errorPanel (error-panel/mk-view! bus))
(def ^:private queryPanel (query-panel/mk-view! bus))
(def ^:private queryForm (query-form/mk-view! bus))
(def ^:private jobViewer (job-viewer/mk-view! bus))
(def ^:private importPanel (import/mk-view! bus))


(def ^:private VIEWS (view/instance))

(defn- render-frame
  []
  (replace-contents! (sel1 :body) [:div#container
                                   [:div#left]
                                   [:div#right]])

  ;; Composite UI components
  (append! (sel1 :#left)
           (proto/dom queryPanel))

  (append! (sel1 :#right)
           (proto/dom errorPanel)
           (proto/dom queryForm)
           (proto/dom jobViewer)
           (proto/dom importPanel))

  (doseq [v (view/views VIEWS)]
    (append! (sel1 :#right) v)))

(defn main
  []
  (.log js/console "loading")

  (render-frame)

  ;; Turn off the stuff we don't want to see right away.
  (proto/publish! bus :error-panel-toggle {})

  ;; Register non-UI event subscribers
  (export/init! bus)

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

  ;;===========================================================================
  ;; New stuff

  ;; Eventually: map across view for :recv and :send, filter out nils, etc.

  (let [net-lib (net/instance)
        app-queue (async/chan)
        inputs (conj (view/receivers VIEWS) app-queue)
        outputs (conj (view/senders VIEWS) (:send net-lib))
        state {}]

    (app-loop! state inputs outputs)

    (start-daemons! app-queue)
    (put! app-queue [:db-form-show {}]))

  ;; End New
  ;;===========================================================================

  (.log js/console " - loaded"))

;; Invoke when page loaded.
(set! (.-onload js/window) main)
