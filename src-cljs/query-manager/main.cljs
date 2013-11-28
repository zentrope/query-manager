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
            ;; [query-manager.event :as event]
            ;; [query-manager.protocols :as proto]
            ;;
            ;; Views
            ;;
            [query-manager.view :as view]
            [query-manager.view.query-panel :as query-panel]
            ;;
            ;; IO
            ;;
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
  [event-ch interval]
  (clock-loop!)
  (mouse-loop!)
  (event-loop! (fn [] (async/put! event-ch [:jobs-poke {}])) interval)
  (event-loop! (fn [] (async/put! event-ch [:queries-poke {}])) interval))

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
        ;; (.log js/console "MAIN:" (str msg))
        (let [new-state (update-state! state outputs msg)]
          (recur new-state)))
      (.log js/console "app-loop terminated"))))

;;-----------------------------------------------------------------------------
;; Main
;;-----------------------------------------------------------------------------

(defn- render-frame!
  [all-views qpanel]
  (replace-contents! (sel1 :body) [:div#container [:div#left] [:div#right]])
  (append! (sel1 :#left) (:view qpanel))
  (doseq [v (view/views all-views)]
    (append! (sel1 :#right) v)))

(defn main
  []
  (.log js/console "loading")

  (let [all-views (view/instance)
        net-lib (net/instance)
        qpanel (query-panel/instance)
        exporter (export/instance)
        app-queue (async/chan)
        inputs (conj (view/receivers all-views) app-queue (:recv net-lib) (:recv qpanel))
        outputs (conj (view/senders all-views) (:send qpanel) (:send net-lib) (:send exporter))
        state {}]

    (render-frame! all-views qpanel)

    (app-loop! state inputs outputs)

    (start-daemons! app-queue 4000)
    (put! app-queue [:db-poke {}])
    ;;
    ;; Need to intercept the :db-change and see if there's any
    ;; data. If not, force the DB ui to show up. Could probably set up
    ;; a go-loop that "takes over" from the app-loop, though I think
    ;; that might take some effort. Once solved, though, it prefigures
    ;; how to do a login thing.
    ;;
    (put! app-queue [:error-panel-toggle {}])
    (put! app-queue [:queries-poke {}])
    (put! app-queue [:jobs-poke {}]))

  (.log js/console " - loaded"))

;; Invoke when page loaded.
(set! (.-onload js/window) main)
