(ns query-manager.view.query-form
  ;;
  ;; Appears when the user wants to create (or edit) a query.
  ;;
  (:use-macros [dommy.macros :only [sel1 node]]
               [cljs.core.async.macros :only [go-loop]])
  (:require [dommy.core :as dom]
            [cljs.core.async :as async]
            [query-manager.utils :as utils]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#query-form-container.form-container {:style {:display "none"}}
         [:div#query-form.form
          [:h2 "Query Definition"]
          [:input#qf-id {:type "hidden"}]
          [:table
           [:tr [:th "description"] [:td [:input#qf-desc {:type "text"}]]]
           [:tr [:th "query"] [:td [:textarea#qf-sql {:rows "18"}]]]]
          [:div.form-buttons
           [:button#qf-save "save"]
           [:button#qf-cancel "cancel"]]]]))

;; Local event handlers

(defn- on-cancel
  [output-ch]
  (fn [e]
    (async/put! output-ch [:query-form-hide {}])))

(defn- on-save
  [output-ch]
  (fn [e]
    (let [query {:id (dom/value (sel1 :#qf-id))
                 :sql (dom/value (sel1 :#qf-sql))
                 :description (dom/value (sel1 :#qf-desc))}]
      (if (empty? (:id query))
        (async/put! output-ch [:query-save query])
        (async/put! output-ch [:query-update query]))
      (async/put! output-ch [:query-form-hide {}]))))

;; Incoming event handlers

(defn- on-show
  []
  (let [id (dom/value (sel1 :#qf-id))]
    (dom/set-html! (sel1 :#qf-save) (if (empty? id) "create" "save")))
  (dom/show! (sel1 :#query-form-container)))

(defn- on-hide
  []
  (dom/hide! (sel1 :#query-form-container))
  (dom/set-value! (sel1 :#qf-id) "")
  (dom/set-value! (sel1 :#qf-desc) "")
  (dom/set-value! (sel1 :#qf-sql) "")
  (dom/set-html! (sel1 :#qf-save) "create"))

(defn- on-update
  [{:keys [id sql description]}]
  (dom/set-value! (sel1 :#qf-id) id)
  (dom/set-value! (sel1 :#qf-desc) description)
  (dom/set-value! (sel1 :#qf-sql) sql)
  (dom/set-html! (sel1 :#qf-save) "save"))

(defn- mk-template
  [output-ch]
  (let [t (template)]
    (dom/listen! [t :#qf-save] :click (on-save output-ch))
    (dom/listen! [t :#qf-cancel] :click (on-cancel output-ch))
    t))

(defn- process
  [[topic msg]]
  (case topic
    :query-form-show (on-show)
    :query-form-hide (on-hide)
    :query-get (on-update (:value msg))
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
  (let [recv-ch (async/chan)
        send-ch (utils/subscriber-ch :query-form-show :query-form-hide :query-get)
        block (block-loop send-ch)]
    {:recv recv-ch
     :send send-ch
     :view (mk-template recv-ch)
     :block block}))
