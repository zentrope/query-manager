(ns query-manager.view.query-form
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :as dom]
            [cljs.core.async :as async]))

(defn- template
  []
  (node [:div#query-form-container.form-container
         [:div#query-form.form
          [:h2 "Query Definition"]
          [:input#qf-id {:type "hidden"}]
          [:table
           [:tr [:th "description"] [:td [:input#qf-desc {:type "text"}]]]
           [:tr [:th "query"] [:td [:textarea#qf-sql {:rows "18"}]]]]
          [:div.form-buttons
           [:button#qf-save "create"]
           [:button#qf-cancel "cancel"]]]]))

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

;;-----------------------------------------------------------------------------

(defn show!
  [queue]
  (let [body (mk-template queue)]
    (dom/append! (sel1 :body) body)))

(defn hide!
  []
  (when-let [place (sel1 :#query-form-container)]
    (dom/remove! place)))

(defn fill!
  [{:keys [id sql description]}]
  (dom/set-value! (sel1 :#qf-id) id)
  (dom/set-value! (sel1 :#qf-desc) description)
  (dom/set-value! (sel1 :#qf-sql) sql)
  (dom/set-html! (sel1 :#qf-save) "save"))
