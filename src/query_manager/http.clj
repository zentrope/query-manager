(ns query-manager.http
  (:require [query-manager.db :as db]
            [query-manager.sql :as sql]
            [query-manager.job :as job]
            [query-manager.test-db :as test-db]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [clojure.tools.logging :refer [info]]
            [compojure.core :refer [routes GET POST DELETE PUT]]
            [compojure.handler :refer [site]]
            [compojure.route :refer [resources not-found]]
            [ring.util.response :refer [redirect status response header]]
            [hiccup.page :refer [html5 include-js include-css]]))

;;-----------------------------------------------------------------------------

(defn- sread
  [stream]
  (if-not (string? stream)
    (with-open [r (clojure.java.io/reader stream)]
      (slurp r))
    stream))

(defn- jconvert
  [k v]
  (cond
    (instance? java.sql.Timestamp v)
    (str v)
    (and (number? v)
         (< v 0))
    (str v)
    (and (instance? java.lang.Long v)
         (< v 0))
    (str v)
    :else v))

(defn- jwrite
  [value]
  (json/write-str value :value-fn jconvert))

(defn- jread
  [request]
  (json/read-str (sread (:body request)) :key-fn keyword))

(defn- as-json
  [doc]
  (-> (response doc)
      (header "Content-Type" "application/json")
      (status 200)))

(defn- as-empty
  [code]
  (-> (response "")
      (header "Content-Type" "plain/text")
      (status code)))

;;-----------------------------------------------------------------------------

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
     (as-json (jwrite (db/get))))

   (PUT "/qman/api/db"
       [:as r]
     (db/put (jread r))
     (as-empty 201))

   (POST "/qman/api/db/test"
       [:as r]
     ;;
     ;; Returning a result from a POST is a no-no, but, well it's the
     ;; simplist thing that can possible work, so foo.
     ;;
     (-> (jread r)
         (db/specialize)
         (test-db/test-connection)
         (jwrite)
         (as-json)))

   ;;---------------------------------------------------------------------------
   ;; QUERY API
   ;;
   ;; For getting and putting queries-to-be-run, but not for actually
   ;; running the queries. Use the /api/job API for that.
   ;;---------------------------------------------------------------------------

   (GET "/qman/api/query/:id"
       [id]
     (if-let [query (sql/one id)]
       (as-json (jwrite query))
       (as-empty 404)))

   (GET "/qman/api/query"
       []
     (as-json (jwrite (sql/all))))

   (PUT "/qman/api/query/:id"
       [id :as r]
     (if-let [query (sql/one id)]
       (let [update (merge query (jread r))]
         (sql/update! update)
         (as-empty 201))
       (as-empty 404)))

   (POST "/qman/api/query"
       [:as r]
     (let [{:keys [sql description]} (jread r)]
       (sql/create! sql description))
     (as-empty 201))

   (DELETE "/qman/api/query/:id"
       [id]
     (sql/delete! id)
     (as-empty 201))

   ;;---------------------------------------------------------------------------
   ;; JOB API
   ;;---------------------------------------------------------------------------

   (GET "/qman/api/job/:id"
       [id :as req]
     (if-let [result (job/one jobs (Long/parseLong id))]
       (as-json (jwrite result))
       (as-empty 404)))

   (GET "/qman/api/job"
       [:as req]
     (as-json (jwrite (job/all jobs))))

   (POST "/qman/api/job/:query-id"
       [query-id :as req]
     (job/create jobs (db/spec) (sql/one query-id))
     (as-empty 201))

   (DELETE "/qman/api/job/:id"
       [id :as req]
     (job/delete! jobs id)
     (as-empty 201))

   ;;---------------------------------------------------------------------------
   ;; BUILT-IN CLIENT
   ;;---------------------------------------------------------------------------

   (GET "/"
       []
     (redirect "/qman"))

   (GET "/qman/queries/download"
       []
     (-> (sql/all)
         (as-> stuff
               (mapv #(dissoc % :id) stuff)
               (with-out-str (clojure.pprint/pprint stuff))
               (clojure.string/replace (str stuff) #"\\n" "\n"))
         (response)
         (status 200)
         (header "Content-Type" "application/octet-stream")
         (header "Content-Disposition" "attachment;filename=\"queries.clj\"")))

   (GET "/qman"
       []
     (html5 [:head
             [:title "Query Manager"]
             [:link {:rel "shortcut icon" :href "/qman/favicon.ico"}]
             (include-css "qman/styles.css")
             (include-js "qman/main.js")]
            [:body "Loading..."]))

   (resources "/")
   (not-found "<h1>Oops. Try <a href='/qman'>here</a>.</h1>")))

(defn mk-web-app
  [jobs]
  (fn [request]
    ((-> (main-routes jobs) (site)) request)))
