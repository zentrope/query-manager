(ns query-manager.http
  (:require [query-manager.db :as db]
            [query-manager.sql :as sql]
            [query-manager.job :as job]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [clojure.tools.logging :refer [info]]
            [compojure.core :refer [routes GET POST DELETE PUT]]
            [compojure.handler :refer [site]]
            [compojure.route :refer [resources not-found]]
            [ring.util.response :refer [redirect status response]]
            [hiccup.page :refer [html5 include-js include-css]]))

(defn- sread
  [stream]
  (if-not (string? stream)
    (with-open [r (clojure.java.io/reader stream)]
      (slurp r))
    stream))

(defn- jwrite
  [value]
  (json/write-str value))

(defn- jread
  [request]
  (json/read-str (sread (:body request)) :key-fn keyword))

(defn- main-routes
  [jobs]
  (routes

   ;;---------------------------------------------------------------------------
   ;;DATABASE API
   ;;
   ;; For now, any given instance of the app should only have one
   ;; configured database, so we'll just implement a GET and PUT so
   ;; client UIs can change it.
   ;;---------------------------------------------------------------------------

   (GET "/qman/api/db"
       []
     (status (response (jwrite (db/get))) 200))

   (PUT "/qman/api/db"
       [:as r]
     (db/put (jread r))
     (status (response "") 201))

   ;;---------------------------------------------------------------------------
   ;; QUERY API
   ;;
   ;; For getting and putting queries-to-be-run, but not for actually
   ;; running the queries. Use the /api/job API for that.
   ;;---------------------------------------------------------------------------

   (GET "/qman/api/query/:id"
       [id]
     (if-let [query (sql/one id)]
       (jwrite query)
       (status (response "") 404)))

   (GET "/qman/api/query"
       []
     (jwrite (sql/all)))

   (PUT "/qman/api/query/:id"
       [id :as r]
     (if-let [query (sql/one id)]
       (let [update (merge query (jread r))]
         (sql/update! update)
         (status (response "") 201))
       (status (response "") 404)))

   (POST "/qman/api/query"
       [:as r]
     (let [{:keys [sql description]} (jread r)]
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
       [id :as req]
     (if-let [result (job/one jobs (Long/parseLong id))]
       (status (response (jwrite result)) 200)
       (status (response "") 404)))

   (GET "/qman/api/job"
       [:as req]
     (status (response (jwrite (job/all jobs))) 200))

   (POST "/qman/api/job/:query-id"
       [query-id :as req]
     (job/create jobs (db/spec) (sql/one query-id))
     (status (response "") 201))

   (DELETE "/qman/api/job/:id"
       [id :as req]
     (job/delete! jobs id)
     (status (response "") 201))

   ;;---------------------------------------------------------------------------
   ;; UTILITIES APIs
   ;; Mainly so I can learn a few new web techniqes.
   ;;---------------------------------------------------------------------------

   (POST "/qman/api/dump"
       [:as request]

;;     (pprint request)
     (info "DUMP.body: " (sread (:body request)))
     (status (response "") 201))

   ;;---------------------------------------------------------------------------
   ;; BUILT-IN CLIENT
   ;;---------------------------------------------------------------------------

   (GET "/"
       []
     (redirect "/qman"))

   (GET "/qman"
       []
     (html5 [:head
             [:title "Query Manager"]
             (include-css "qman/styles.css")
;;             (include-js "qman/jquery-2.0.2.min.js")
             (include-js "qman/main.js")]
            [:body "Loading..."]))

   (resources "/")
   (not-found "<h1>Oops. Try <a href='/qman'>here</a>.</h1>")))

(defn mk-web-app
  [jobs]
  (fn [request]
    ((-> (main-routes jobs) (site)) request)))
