(ns query-manager.view.error-panel
  (:use-macros [dommy.macros :only [sel1 node]]
               [cljs.core.async.macros :only [go-loop]])
  (:require [dommy.core :as dom]
            [query-manager.utils :as utils]
            [cljs.core.async :as async]))

;;-----------------------------------------------------------------------------

(def ^:private errors (atom []))

(def ^:private template
  (node [:div#error-panel.panel
         [:div.panel-header "error log"]
         [:div.panel-body
          [:div#ep-list.lister
           [:p "No errors!"]]]]))

(defn- table-of
  [errors]
  (node (list [:table
               [:tr
                [:th {:width "10%"} "when"]
                [:th {:width "10%"} "status"]
                [:th {:width "40%"} "uri"]
                [:th {:width "40%"} "error"]]
               (for [{:keys [timestamp status uri reason]} errors]
                 [:tr
                  [:td timestamp]
                  [:td status]
                  [:td uri]
                  [:td reason]])]
              [:button#ep-clear "clear"])))

(defn- on-clear
  []
  (fn [e]
    (reset! errors [])
    (dom/replace-contents! (sel1 :#ep-list) (node [:p "No errors!"]))))

(defn- on-error
  [error-event]
  (let [errs (swap! errors conj error-event)]
    (dom/replace-contents! (sel1 :#ep-list) (table-of (reverse (sort-by :timestamp errs))))
    (dom/listen! (sel1 :#ep-clear) :click (on-clear))
    (when (> (count @errors) 15)
      (swap! errors (fn [es] (take 15 es))))))

(defn- on-visibility-toggle!
  []
  (dom/toggle! (sel1 :#error-panel)))

(defn- mk-template
  []
  template)

(defn- process
  [[topic msg]]
  (case topic
    :web-error (on-error (:value msg))
    :error-panel-toggle (on-visibility-toggle!)
    :noop))

(defn- block-loop
  [input-ch]
  (go-loop []
    (when-let [msg (async/<! input-ch)]
      (process msg)
      (recur))))

;;-----------------------------------------------------------------------------

(defn instance
  []
  (let [send-ch (utils/subscriber-ch :web-error :error-panel-toggle)
        block (block-loop send-ch)]
    {:recv nil
     :send send-ch
     :view (mk-template)
     :block block}))
