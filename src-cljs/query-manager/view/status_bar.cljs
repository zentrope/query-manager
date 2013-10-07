(ns query-manager.view.status-bar
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace! listen! set-html! toggle-class!
                                add-class! remove-class!]]
            [query-manager.view :as view]
            [query-manager.protocols :refer [publish!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#status-bar
         [:div.status-buttons
          [:button#sb-db {:class "not-showing"} "db"]
          [:button#sb-err "log"]]
         [:div#mouse-coords
          "(" [:span#sb-mouse-x "0"] ":" [:span#sb-mouse-y "0"] ")"]]))

(defn- set-mouse-coords!
  [[x y]]
  (set-html! (sel1 :#sb-mouse-x) x)
  (set-html! (sel1 :#sb-mouse-y) y))

(defn- on-toggle!
  [mbus topic]
  (fn [e]
    (publish! mbus topic {})))

(defn- mk-template
  [mbus]
  (let [t (template)]
    (listen! [t :#sb-db] :click (on-toggle! mbus :db-form-show))
    (listen! [t :#sb-err] :click (on-toggle! mbus :error-panel-toggle))
    t))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(def ^:private subscriptions
  {:mousemove (fn [mbus msg] (set-mouse-coords! (:value msg)))
   :db-form-hide (fn [mbus msg] (add-class! (sel1 :#sb-db) "not-showing"))
   :db-form-show (fn [mbus msg] (remove-class! (sel1 :#sb-db) "not-showing"))
   :error-panel-toggle (fn [mbus msg] (toggle-class! (sel1 :#sb-err) "not-showing"))})

(defn mk-view!
  [mbus]
  (view/mk-view mbus mk-template subscriptions))
