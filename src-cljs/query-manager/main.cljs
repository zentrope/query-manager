(ns query-manager.main
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-html! html replace! listen!]]
            [query-manager.net :refer [dump]]))


(defn- not-implemented
  []
  (node [:div#container
         [:h1 "Query Manager App"]
         [:p "Client not yet implemented."]
         [:p [:button#test-button "Test"]]
         [:p.flash]]))

(defn- flash
  [msg]
  (replace! (sel1 :.flash) msg))

(defn- fake-action
  []
  (let [data [{:id "1" :sql "select * from foo" :description "Blah"}]]
    (dump data
          (fn [] (flash "Worked!"))
          (fn [e] (flash (str "Failed: " (:status e) " -> " (:reason e)))))))

(defn- noimpl-events
  []
  (let [ni (not-implemented)]
    (listen! ni :click fake-action)))

(defn ^:export main
  []
  (.log js/console "Hello.")
  (replace! (sel1 :body) (noimpl-events))
  (.log js/console "Goodbye."))

(set! (.-onload js/window) main)
