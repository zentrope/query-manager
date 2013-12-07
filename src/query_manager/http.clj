(ns query-manager.http
  (:require [clojure.core.async :as async]
            [org.httpkit.server :as httpd]
            [query-manager.db :as db]
            [query-manager.sql :as sql]
            [query-manager.job :as job]
            [query-manager.repo :as repo]
            [query-manager.test-db :as test-db]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log :refer [info]]
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

;;-----------------------------------------------------------------------------

(defn- mk-response
  ([status]
     (mk-response "{}"))
  ([status body]
     {:status status
      :body body
      :headers {"Content-Type" "application/json"}}))

(defn- log-event!
  [name event]
  (let [s (format "%s" event)]
    (try
      (log/info name (subs s 0 76) "... ]")
      (catch Throwable t
        (log/info name s)))))

(defn- wait-loop!
  "Terminate when there's a web-channel available for publishing."
  [channel-hub]
  (log/debug "waiting for connection.")
  (async/go-loop []
    (if (> (count @channel-hub) 0)
      (do
        (log/debug " -" (count @channel-hub) " web-channels available")
        :done)
      (do (async/<! (async/timeout 10))
          (recur)))))

(defn- publish-loop!
  [channel-hub publish-ch]
  (async/go-loop []
    (when-let [msg (async/<! publish-ch)]
      (log-event! "pub:" msg)
      (log/debug " - channels pending" (count @channel-hub))
      (<! (wait-loop! channel-hub))
      (doseq [[web-channel req] @channel-hub]
        (log/debug " - sending " (first msg) "to" web-channel)
        (if (= (first msg) (:http-error msg))
          (httpd/send! web-channel (mk-response (second msg)))
          (httpd/send! web-channel (mk-response 200 (jwrite msg)))))
      (recur))))

(defn- process!
  [jobs db files control-ch publish-ch [topic msg]]
  (case topic

    :app-init
    (do (async/put! publish-ch [:job-change (job/all jobs)])
        (async/put! publish-ch [:query-change (sql/all)]))

    :db-get
    (async/put! publish-ch [:db-change (db/get db)])

    :db-test
    (let [result (test-db/test-connection (db/specialize msg))]
      (async/put! publish-ch [:db-test-result result]))

    :db-save
    (do (db/put db msg)
        (repo/save-database! files (db/get db))
        (async/put! publish-ch [:db-change (db/get db)]))

    :job-list
    (async/put! publish-ch [:job-change (job/all jobs)])

    :job-run-all
    (do (doseq [query (sql/all)]
          (job/create jobs (db/spec db) query control-ch))
        (async/put! publish-ch [:job-change (job/all jobs)]))

    :job-run
    (do (job/create jobs (db/spec db) (sql/one msg) control-ch)
        (async/put! publish-ch [:job-change (job/all jobs)]))

    :job-get
    (async/put! publish-ch [:job-get (job/one jobs (Long/parseLong msg))])

    :job-delete
    (do (job/delete! jobs (str msg))
        (async/put! publish-ch [:job-change (job/all jobs)]))

    :job-delete-all
    (do (job/delete-all! jobs)
        (async/put! publish-ch [:job-change (job/all jobs)]))

    :job-complete
    (async/put! publish-ch [:job-change (job/all jobs)])

    :query-get
    (async/put! publish-ch [:query-get (sql/one msg)])

    :query-list
    (async/put! publish-ch [:query-change (sql/all)])

    :query-update
    (if-let [query (sql/one (:id msg))]
      (let [update (merge query msg)]
        (sql/update! update)
        (async/put! publish-ch [:query-change (sql/all)]))
      (async/put! [:http-error 404]))

    :query-create
    (let [{:keys [sql description]} msg]
      (sql/create! sql description)
      (async/put! publish-ch [:query-change (sql/all)]))

    :query-delete
    (do (sql/delete! msg)
        (async/put! publish-ch [:query-change (sql/all)]))

    :noop))

(defn- normalize
  [[topic msg]]
  [(keyword topic) msg])

(defn- control-loop!
  [jobs db files control-ch publish-ch]
  (async/go-loop []
    (when-let [msg (async/<! control-ch)]
      (let [event (normalize msg)]
        (log-event! "con:" event)
        (process! jobs db files control-ch publish-ch event)
        (recur)))))

(defn- message-handler
  [channel-hub]
  (fn [request]
    (httpd/with-channel request web-channel
      (swap! channel-hub assoc web-channel request)
      (httpd/on-close web-channel (fn [status]
                                    (log/debug " - closing" web-channel)
                                    (swap! channel-hub dissoc web-channel))))))

;;-----------------------------------------------------------------------------

(defn- main-routes
  [jobs channel-hub publish-ch control-ch]
  (routes

   (GET "/qman/api/messages"
       []
     (message-handler channel-hub))

   (POST "/qman/api/messages"
       [:as r]
     (async/put! control-ch (jread r))
     {:status 201 :headers {"content-type" "application/json"} :body "{}"})

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

(defn- mk-app
  [app]
  (let [{:keys [port jobs channel-hub publish-ch control-ch]} @app]
    (fn [request]
      ((-> (main-routes jobs channel-hub publish-ch control-ch)
           (site))
       request))))

(defn instance
  [port jobs db files]
  (atom {:jobs jobs
         :db db
         :files files
         :port port
         :httpd nil
         :publish-ch (async/chan)
         :control-ch (async/chan)
         :channel-hub (atom {})}))

(defn start!
  [app]
  (let [{:keys [port jobs db files channel-hub publish-ch control-ch]} @app]
    (publish-loop! channel-hub publish-ch)
    (control-loop! jobs db files control-ch publish-ch)
    (swap! app assoc :httpd (httpd/run-server (mk-app app) {:port port}))
    (log/info "Starting web application on port" port)))

(defn stop!
  [app]
  (log/info "Stopping web application.")
  (async/close! (:publish-ch @app))
  (async/close! (:control-ch @app))
  (when-let [server (:httpd @app)]
    (server))
  (reset! app (instance (:port @app) (:jobs app)))
  :stopped)
