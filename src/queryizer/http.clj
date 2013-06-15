(ns queryizer.http
  (:require [queryizer.data :refer [get-db-spec put-db-spec]]
            [clojure.data.json :as json]
            [compojure.core :refer [defroutes GET POST DELETE PUT]]
            [compojure.handler :as handler]
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
  ;;
  ;;---------------------------------------------------------------------------

  (GET "/qzer/api/db"
      []
    (json/write-str (get-db-spec)))

  (PUT "/qzer/api/db"
      [:as r]
    (put-db-spec (json/read-str (:body r) :key-fn keyword))
      (status (response "") 201))

  ;;---------------------------------------------------------------------------
  ;; QUERY API
  ;;---------------------------------------------------------------------------

  (GET "/qzer/api/query/:id"
      [id]
    (-> (json/write-str {:error "not implemented"})
        (response)
        (status 501)))

  (GET "/qzer/api/query"
      []
    (json/write-str []))

  (POST "/qzer/api/query"
      ;; Should be a version of this to upload a file.
      [query-id]
    (json/write-str {:error "not implemented"}))

  (DELETE "/qzer/api/query/:id"
      [id]
    (json/write-str {:error "not implemented"}))

  ;;---------------------------------------------------------------------------
  ;; JOB API
  ;;---------------------------------------------------------------------------

  (GET "/qzer/api/job/:id"
      [id]
    (json/write-str {:error "not implemented"}))

  (GET "/qzer/api/job"
      []
    (json/write-str []))

  (POST "/qzer/api/job/:query-id"
      [query-id]
    (json/write-str {:error "not implemented"}))

  (DELETE "/qzer/api/job/:id"
      [id]
    (json/write-str {:error "not implemented"}))

  ;;---------------------------------------------------------------------------
  ;; BUILT IN CLIENT
  ;;---------------------------------------------------------------------------

  (GET "/"
      []
    (redirect "/qzer"))

  (GET "/qzer"
      []
    (html5 [:head
            [:title "Long Running Query Manager"]
            (include-css "styles.css")
            (include-js "jquery-2.0.2.min.js")
            (include-js "client.js")]
           [:body "Loading..."]))

  (resources "/")
  (not-found "<h1>Oops. Try <a href='/'>here</a>.</h1>"))

(def app
  (-> main-routes
      (handler/site)))
