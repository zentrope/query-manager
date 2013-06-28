(ns query-manager.main
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-html! html replace! listen!]]))

(defn- not-implemented
  []
  (node [:div#container
         [:h1 "Query Manager App"]
         [:p "Client not yet implemented."]
         [:button#test-button "Test"]]))

(defn- noimpl-events
  []
  (let [ni (not-implemented)]
    (listen! ni :click (fn [] (js/alert "Clicked!!")))))

(defn ^:export main
  []
  (.log js/console "Hello.")
  (replace! (sel1 :body) (noimpl-events))
  (.log js/console "Goodbye."))

(set! (.-onload js/window) main)
