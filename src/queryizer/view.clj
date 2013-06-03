(ns queryizer.view
  (:use compojure.core)
  (:use hiccup.core)
  (:use queryizer.controller)
  (:require [clojure.data.json :as json]
    [compojure.handler :as handler]))


(defn view-layout [& content]
  (html
    [:head [:meta {:http-equiv "Content-type"
    :content "text/html; charset=utf-8"}]
    [:title "Queryizer"]]
    [:body content]))

;;hyperlinked list of perdefined queries 
(def query-selection
  (list [:h2 "Available queries"]
    (for
      [elem queryizer.controller/available-queries]
      (list
        [:a {:href (str "/jobs/" (:id elem))}
        (:id elem)] [:br ]))))

(defn view-input []
  (view-layout
    [:h2 "Enter query"]
    [:form {:method "post" :action "/"}
    [:input.math {:type "text" :name "query"}] [:br ]
    [:input.action {:type "submit" :value "Enter"}]]
    query-selection))

(defn view-jobs []
  (println "view-jobs")
  (view-layout
    [:h2 "Current Jobs"]
    (for [job (queryizer.controller/list-jobs)]
      [:p (str job)])))

(defn view-output [query]
  (let [rows (queryizer.controller/submit-query query)
    cols (keys (first rows))]
    (view-layout
      [:h2 "Here is your result"]
      [:table [:tr (for [col cols] [:th (name col)])]
      (for [row rows] [:tr (for [item (vals row)] [:td item])])]
      [:a.action {:href "/"} "Enter another?"] [:br ])))

(defroutes main-routes
  (GET "/" []
    (view-input))
  (GET "/tables" [] (view-output "show tables"))
  (GET "/query/:id" [id] (view-output (queryizer.controller/query id)))
  (GET "/tables/:table" [table] (view-output (str "select * from " table)))
  (POST "/" [query]
    (println "obvious debug statment: " query)
    (view-output query))
  
;;-------These are the asynch parts...
(GET "/jobs" [] (println "GET jobs")
  (view-jobs))
(GET "/jobs/:job" [job] (println "POST jobs")
  (queryizer.controller/submit-job (query job))
  (view-jobs)))


(def app (-> main-routes (handler/site)))

