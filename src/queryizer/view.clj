(ns queryizer.view
  (:use compojure.core)
  (:use hiccup.core)
  (:use queryizer.controller )
  (:require [clojure.data.json :as json] [compojure.handler :as handler]))


(defn view-layout [& content]
  (html
    [:head
        [:meta {:http-equiv "Content-type"
                :content "text/html; charset=utf-8"}]
        [:title "Queryizer"]]
      [:body content]))

(defn view-input []
  (view-layout
    [:h2 "Enter query"]
    [:form {:method "post" :action "/"}
      [:input.math {:type "text" :name "query"}] [:br]
      [:input.action {:type "submit" :value "Enter"}]]
    [:h2 "Available queries"]
   (for 
      [elem queryizer.controller/available-queries] 
        [:p (:id elem)]
   )
  )
)


(defn view-output [query]
  (let [rows (queryizer.controller/run-query query) 
        cols (keys (first rows))] 
  (view-layout
    [:h2 "Here is your result"]
    [:table 
      [:tr (for [col cols] [:th (name col)])]
      (for [row rows] [:tr (for [item (vals row)] [:td item])])]
    [:a.action {:href "/"} "Enter another?"] [:br])))

(defroutes main-routes
  (GET "/" []
    (view-input))

  (POST "/" [query]
    (println "obvious debug statment: " query)
      (view-output query)))

(def app (-> main-routes (handler/site)))