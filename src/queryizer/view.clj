(ns queryizer.view
  (:use compojure.core)
  (:use hiccup.core)
  (:use hiccup.page-helpers)
  (:use queryizer.controller ))


(defn view-layout [& content]
  (html
    (doctype :xhtml-strict)
    (xhtml-tag "en"
      [:head
        [:meta {:http-equiv "Content-type"
                :content "text/html; charset=utf-8"}]
        [:title "Queryizer"]]
      [:body content])))

(defn view-input []
  (view-layout
    [:h2 "Enter query"]
    [:form {:method "post" :action "/"}
      [:input.math {:type "text" :name "query"}] [:br]
      [:input.action {:type "submit" :value "Enter"}]]
      [:h2 "Available queries"]
      ;;(for 
      ;;  [elem queryizer.controller/available-queries] 
      ;;    ([:p (get elem :id)] [:br]))
      ))


(defn view-output [query]
  (view-layout
    [:h2 "Here is your result"]
    [:p (queryizer.controller/run-query query)]
    [:a.action {:href "/"} "Enter another?"] [:br]))

(defroutes app
  (GET "/" []
    (view-input))

  (POST "/" [query]
      (view-output query)))