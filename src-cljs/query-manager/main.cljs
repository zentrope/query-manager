(ns query-manager.main
  ;;
  ;; I wrote this before I knew about core.async. So, time to
  ;; rewrite. ;) If you see this notice, it means I'm in the process
  ;; of attempting to do that without breaking anything.
  ;;
  (:use-macros [dommy.macros :only [sel1]]
               [cljs.core.async.macros :only [go-loop]])
  ;;
  (:require [dommy.core :as dom]
            [cljs.core.async :as async]
            ;;
            [query-manager.utils :as utils]
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
    (<! (async/timeout millis))
    (event-fn)
    (recur)))

(defn- clock-loop!
  []
  (go-loop []
    (<! (async/timeout 1000))
    (let [date (js/Date.)
          pretty (str (utils/das :hour date) ":"
                      (utils/das :minute date) ":"
                      (utils/das :second date))]
      (dom/set-html! (sel1 :#title-clock) pretty)
      (recur))))

(defn- mouse-loop!
  []
  (let [ch (async/chan)]
    (go-loop []
      (when-let [[x y] (<! ch)]
        (dom/set-html! (sel1 :#sb-mouse-x) x)
        (dom/set-html! (sel1 :#sb-mouse-y) y)
        (recur)))
    (dom/listen! (sel1 :html) :mousemove
                 (fn [e]
                   (let [x (.-clientX e)
                         y (.-clientY e)]
                     (async/put! ch [x y]))))))

(defn- start-daemons!
  [event-ch interval]
  (clock-loop!)
  (mouse-loop!)
  (event-loop! (fn [] (async/put! event-ch [:jobs-poke {}])) interval)
  (event-loop! (fn [] (async/put! event-ch [:queries-poke {}])) interval))

;;-----------------------------------------------------------------------------

(defn- update-acquire-state!
  [state outputs msg]
  (doseq [o outputs]
    (async/put! o msg))
  (when (= (first msg) :db-change)
    (.log js/console "db-change" (str msg)))
  (if (and (= (first msg) :db-change)
           (:updated (:value (second msg))))
    (assoc state :db-acquired? true)
    state))

;; THIS IS REALLY MESSED UP!
;;
;; The problem is having multiple event queues. You have to track
;; them individually if you want to have a mode.
;;

(defn- acquire-db-loop!
  [initial-state app-queue inputs outputs]
  (go-loop [state initial-state]
    (.log js/console "DB-ACQ: waiting")
    (let [[msg ch] (async/alts! inputs)]
      (.log js/console "DB-ACQ:" (str msg))
      (when-not (nil? msg)
        (let [new-state (update-acquire-state! state outputs msg)]
          (if (:db-acquired? new-state)
            (do
              (async/put! app-queue [:db-form-hide {}])
              new-state)
            (if (:db-form? new-state)
              (recur new-state)
              (do (async/put! app-queue [:db-form-show {}])
                  (recur (assoc new-state :db-form? true))))))))))

;;-------

(defn- update-state!
  [state outputs msg]
  (doseq [o outputs]
    (async/put! o msg))
  state)

(defn- app-loop!
  [initial-state inputs outputs]
  (go-loop [state initial-state]
    (let [[msg ch] (async/alts! inputs)]
      (when-not (nil? msg)
        ;; (.log js/console "MAIN:" (str msg))
        (let [new-state (update-state! state outputs msg)]
          (recur new-state)))
      (.log js/console "app-loop terminated"))))

;;-------

(defn- render-frame!
  [all-views qpanel]
  (dom/replace-contents! (sel1 :body) [:div#container [:div#left] [:div#right]])
  (dom/append! (sel1 :#left) (:view qpanel))
  (doseq [v (view/views all-views)]
    (dom/append! (sel1 :#right) v)))

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
        initial-state {:db-acquired? false
                       :db-form? false}]

    (render-frame! all-views qpanel)

    (async/put! app-queue [:error-panel-toggle {}])
    (async/put! app-queue [:db-poke {}])
    (let [runner (fn [state]
                   (app-loop! state inputs outputs)
                   (start-daemons! app-queue 4000)
                   (async/put! app-queue [:queries-poke {}])
                   (async/put! app-queue [:jobs-poke {}]))]
      (async/take! (acquire-db-loop! initial-state app-queue inputs outputs) runner)))

  (.log js/console " - loaded"))

;; Invoke when page loaded.
(set! (.-onload js/window) main)
