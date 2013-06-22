(ns query-manager.http
  (:require [query-manager.data :refer [get-db-spec put-db-spec]]
            [query-manager.sql :as sql]
            [clojure.data.json :as json]
            [compojure.core :refer [defroutes GET POST DELETE PUT]]
            [compojure.handler :refer [site]]
            [compojure.route :refer [resources not-found]]
            [ring.util.response :refer [redirect status response]]
            [hiccup.page :refer [html5 include-js include-css]]))

(defroutes main-routes

  ;;---------------------------------------------------------------------------
  ;; DATABASE API
  ;;
  ;; For now, any given instance of the app should only have one
  ;; configured database, so we'll just implement a GET and PUT so
  ;; client UIs can change it.
  ;;---------------------------------------------------------------------------

  (GET "/qman/api/db"
      []
    (json/write-str (get-db-spec)))

  (PUT "/qman/api/db"
      [:as r]
    (put-db-spec (json/read-str (:body r) :key-fn keyword))
      (status (response "") 201))

  ;;---------------------------------------------------------------------------
  ;; QUERY API
  ;;
  ;; For getting and putting queries-to-be-run, but not for actually
  ;; running the queries. Use the /api/job API for that.
  ;;---------------------------------------------------------------------------

  (GET "/qman/api/query/:id"
      [id]
    (if-let [query (sql/find id)]
      (json/write-str query)
      (status (response "") 404)))

  (GET "/qman/api/query"
      []
    (json/write-str (sql/all)))

  (PUT "/qman/api/query/:id"
      [id :as r]
    (if-let [query (sql/find id)]
      (let [update (merge query (json/read-str (:body r) :key-fn keyword))]
        (sql/update! update)
        (status (response "") 201))
      (status (response "") 404)))

  (POST "/qman/api/query"
      [:as r]
    (let [{:keys [sql description]} (json/read-str (:body r) :key-fn keyword)]
      (sql/create! sql description))
    (status (response "") 201))

  (DELETE "/qman/api/query/:id"
      [id]
    (sql/delete! id)
    (status (response "") 201))

  ;;---------------------------------------------------------------------------
  ;; JOB API
  ;;---------------------------------------------------------------------------

  (GET "/qman/api/job/:id"
      [id]
    (json/write-str {:error "not implemented"}))

  (GET "/qman/api/job"
      []
    (json/write-str []))

  (POST "/qman/api/job/:query-id"
      [query-id]
    (json/write-str {:error "not implemented"}))

  (DELETE "/qman/api/job/:id"
      [id]
    (json/write-str {:error "not implemented"}))

  ;;---------------------------------------------------------------------------
  ;; BUILT IN CLIENT
  ;;---------------------------------------------------------------------------

  (GET "/"
      []
    (redirect "/qman"))

  (GET "/qman"
      []
    (html5 [:head
            [:title "Query Manager"]
            (include-css "qman/styles.css")
            (include-js "qman/jquery-2.0.2.min.js")
            (include-js "qman/client.js")]
           [:body "Loading..."]))

  (resources "/")
  (not-found "<h1>Oops. Try <a href='/qman'>here</a>.</h1>"))

(def app
  (-> main-routes
      (site)))
