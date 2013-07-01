(ns query-manager.main
  (:use-macros [dommy.macros :only [sel sel1 node]])
  (:require [dommy.core :refer [set-html! replace! replace-contents!
                                listen! append!]]
            [query-manager.view.status-bar :as status-bar]
            [query-manager.view.title-bar :as title-bar]
            [query-manager.net :refer [dump get-db]]))

;;-----------------------------------------------------------------------------

(defn- now
  []
  (.getTime (js/Date.)))

(defn- start-clock
  []
  (js/setTimeout (fn []
                   (title-bar/send [:clock {:value (now)}])
                   (start-clock)) 1000))

(defn- ni-template
  []
  (node [:div#container
         [:h1 "Query Manager App"]
         [:p "Client not yet implemented."]
         [:p [:button#test-button "Test"]]
         [:p.flash "~"]]))

(defn- loading
  [toggle]
  (status-bar/send [:loading {:value toggle :timestamp (now)}]))

(defn- flash
  [msg]
  (set-html! (sel1 :.flash) msg))

(defn- fake-action
  []
  (loading true)
  (let [data [{:id "1" :sql "select * from foo" :description "Blah"}]]

    (get-db (fn [data]
              (let [db (js->clj data :keywordize-keys true)]
                (loading false)
                (status-bar/send [:db-change {:value db :timestamp (now)}])
                (flash (str db))))
            (fn [e] (flash (str "Failed: " (:status e) " -> " (:reason e)))))))

(defn- ni-view
  []
  (let [ni (ni-template)]
    (listen! [ni :#test-button] :click fake-action)
    ni))

(defn main
  []
  (.log js/console "Hello.")
  (replace-contents! (sel1 :body) (title-bar/dom))
  (append! (sel1 :body) (ni-view))
  (append! (sel1 :body) (status-bar/dom))

  (listen! (sel1 :body) :mousemove
           (fn [e]
             (status-bar/send [:mousemove {:value [(.-clientX e)
                                                   (.-clientY e)]}])))
  (start-clock)

  (.log js/console "Goodbye."))

(set! (.-onload js/window) main)
