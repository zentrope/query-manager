(ns query-manager.view.error-panel
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace-contents! listen! toggle!]]
            [query-manager.view :as view]
            [query-manager.protocols :refer [publish!]]))

;;-----------------------------------------------------------------------------
;; Implementation
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
  [mbus]
  (fn [e]
    (reset! errors [])
    (replace-contents! (sel1 :#ep-list) (node [:p "No errors!"]))))

(defn- on-error
  [mbus error-event]
  (let [errs (swap! errors conj error-event)]
    (replace-contents! (sel1 :#ep-list) (table-of (reverse (sort-by :timestamp errs))))
    (listen! (sel1 :#ep-clear) :click (on-clear mbus))
    (when (> (count @errors) 15)
      (swap! errors (fn [es] (take 15 es))))))

(defn- on-visibility-toggle!
  [mbus]
  (toggle! (sel1 :#error-panel)))

(defn- mk-template
  [mbus]
  template)

(def ^:private subscriptions
  {:web-error (fn [mbus msg] (on-error mbus (:value msg)))
   :error-panel-toggle (fn [mbus msg] (on-visibility-toggle! mbus))})

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn mk-view!
  [mbus]
  (view/mk-view mbus mk-template subscriptions))
