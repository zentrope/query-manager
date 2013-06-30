(ns query-manager.main
  (:use-macros [dommy.macros :only [sel sel1 node]])
  (:require [dommy.core :refer [set-html! replace! listen!]]
            [query-manager.net :refer [dump]]))

(let [c (atom 0)]
  (defn- counter
    []
    (swap! c inc)))

(defn- ni-template
  []
  (node [:div#container
         [:h1 "Query Manager App"]
         [:p "Client not yet implemented."]
         [:p [:button#test-button "Test"]]
         [:p.flash "."]]))

(defn- flash
  [msg]
  (set-html! (sel1 :.flash) (str msg " &bull; " (counter))))

(defn- fake-action
  []
  (let [data [{:id "1" :sql "select * from foo" :description "Blah"}]]
    (dump data
          (fn [_] (flash "worked"))
          (fn [e] (flash (str "Failed: " (:status e) " -> " (:reason e)))))))

(defn- ni-view
  []
  (let [ni (ni-template)]
    (listen! [ni :#test-button] :click fake-action)
    ni))

(defn main
  []
  (.log js/console "Hello.")
  (replace! (sel1 :body) (ni-view))
  (.log js/console "Goodbye."))

(set! (.-onload js/window) main)
