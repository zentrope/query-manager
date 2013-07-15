(ns query-manager.view.status-bar
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [replace! listen! set-html! toggle-class!]]
            [query-manager.protocols :refer [subscribe!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#status-bar
         [:div#db-info "conn: none"]
         [:div.status-buttons
          [:button#sb-db.not-showing "db"]
          [:button#sb-jobs "jobs"]
          [:button#sb-sql "queries"]
          [:button#sb-err "err"]]
         [:div#mouse-coords
          "(" [:span#sb-mouse-x "0"] ":" [:span#sb-mouse-y "0"] ")"]]))

(defn- set-mouse-coords!
  [[x y]]
  (set-html! (sel1 :#sb-mouse-x) x)
  (set-html! (sel1 :#sb-mouse-y) y))

(defn- set-db-info!
  [{:keys [type host] :as db}]
  (replace! (sel1 :#db-info) (node [:div#db-info
                                    "conn: "
                                    [:span#db-type type]
                                    " on "
                                    [:span#db-host host]])))

(defn- on-toggle!
  [channel topic]
  (fn [e]
    (channel [topic {}])))

(defn- mk-template
  [channel]
  (let [t (template)]
    (listen! [t :#sb-db] :click (on-toggle! channel :db-form-show))
    (listen! [t :#sb-jobs] :click (on-toggle! channel :job-panel-toggle))
    (listen! [t :#sb-sql] :click (on-toggle! channel :query-panel-toggle))
    (listen! [t :#sb-err] :click (on-toggle! channel :error-panel-toggle))
    t))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn dom
  [channel]
  (mk-template channel))

;;-----------------------------------------------------------------------------
;; Experiment

(def ^:private subscriptions
  {:mousemove (fn [mbus msg] (set-mouse-coords! (:value msg)))
   :job-panel-toggle (fn [mbus msg] (toggle-class! (sel1 :#sb-jobs) "not-showing"))
   :db-change (fn [mbus msg] (set-db-info! (:value msg)))
   :db-form-hide (fn [mbus msg] (toggle-class! (sel1 :#sb-db) "not-showing"))
   :db-form-show (fn [mbus msg] (toggle-class! (sel1 :#sb-db) "not-showing"))
   :query-panel-toggle (fn [mbus msg] (toggle-class! (sel1 :#sb-sql) "not-showing"))
   :error-panel-toggle (fn [mbus msg] (toggle-class! (sel1 :#sb-err) "not-showing"))})

;; Does this need to return the dom, or should we have
;; two separate calls?
;;
;; (defprotocol View
;;   (init! [this])
;;   (dom [this]))
;;
;; (defrecord StatusView [mbus]
;;   View
;;   (init! [this]
;;      (subscribe! mbus ...)
;;      this)
;;   (dom [this]
;;     (mk-template this)))
;;
;; (defn mk-view [mbus]
;;   (init! (StatusView. mbus)))
;;
;; All that could be genericized so that all a view has to do is
;; supply a template fn and a map of events to handlers and be
;; done. Hm.
;;
;; (defn dom [mbus]
;;   (view/mk-view mbus mk-template subscriptions))
;;
;; Then the caller just has to use "view" functions:
;;
;; (dom status-bar)
;;

(defn mk-view!
  [mbus]
  (let [t nil ] ;(mk-template mbus)
    (doseq [[topic handler] subscriptions]
      (subscribe! mbus topic handler))
    t))
