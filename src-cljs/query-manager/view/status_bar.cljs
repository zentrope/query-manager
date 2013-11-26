(ns query-manager.view.status-bar
  ;;
  (:use-macros [dommy.macros           :only [sel1 node]]
               [cljs.core.async.macros :only [go-loop]])
  (:require    [dommy.core             :as dom]
               [cljs.core.async        :as async]))

;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#status-bar
         [:div.status-buttons
          [:button#sb-db {:class "not-showing"} "db"]
          [:button#sb-err "log"]]
         [:div#mouse-coords
          "(" [:span#sb-mouse-x "0"] ":" [:span#sb-mouse-y "0"] ")"]]))

(defn- on-toggle!
  [send-ch topic]
  (fn [e]
    (async/put! send-ch [topic {}])))

(defn- mk-template
  [send-ch]
  (let [t (template)]
    (dom/listen! [t :#sb-db] :click (on-toggle! send-ch :db-form-show))
    (dom/listen! [t :#sb-err] :click (on-toggle! send-ch :error-panel-toggle))
    t))

(defn- process
  [output-ch [topic msg]]
  (case topic
    :db-form-hide (dom/add-class! (sel1 :#sb-db) "not-showing")
    :db-form-show (dom/remove-class! (sel1 :#sb-db) "not-showing")
    :error-panel-toggle (dom/toggle-class! (sel1 :#sb-err) "not-showing")
    :noop))

(defn- block-loop
  [input-ch output-ch]
  (go-loop []
    (when-let [msg (async/<! input-ch)]
      (process output-ch msg)
      (recur))))

;;-----------------------------------------------------------------------------

(defn instance
  []
  (let [recv-ch (async/chan) ;; put here for app to recv
        send-ch (async/chan) ;; listen here to get from app
        block (block-loop send-ch recv-ch)]
    {:recv recv-ch
     :send send-ch
     :view (mk-template recv-ch)
     :block block}))
