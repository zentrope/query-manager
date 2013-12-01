(ns query-manager.http
  (:require [clojure.core.async :as async]
            [org.httpkit.server :as httpd]
            [query-manager.db :as db]
            [query-manager.sql :as sql]
            [query-manager.job :as job]
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

;; Long poll experiment stuff

(defn- publish-loop!
  [channel-hub publish-ch]
  ;;
  ;; When something is put in the queue, it's send to all pending
  ;; web-channels.
  ;;
  ;; PROBLEM: If you send one message to a web-channel, it'll close,
  ;;          thus allowing you to miss the next few messages while
  ;;          it reconnects.
  ;;
  (async/go-loop []
    (when-let [msg (async/<! publish-ch)]
      (log/info "publish-loop: " msg)
      (doseq [[web-channel req] @channel-hub]
        (if (= (first msg) (:http-error msg))
          (httpd/send! web-channel {:status (second msg)
                                    :headers {"content-type" "application/json"}
                                    :body ""})
          (httpd/send! web-channel {:status 200
                                    :headers {"content-type" "application/json"}
                                    :body (jwrite msg)})))
      ;;
      ;; Wait awhile to allow a client to reconnect if there are
      ;; pending messages.
      ;;
      (<! (async/timeout 50)) ;; should wait in a loop until len channel-hub non-zero
      (recur))))

(defn- process!
  [jobs control-ch publish-ch [topic msg]]
  (case topic

    :app-init
    (do (async/put! publish-ch [:job-change (job/all jobs)])
        (async/put! publish-ch [:query-change (sql/all)]))

    :db-get
    (async/put! publish-ch [:db-change (db/get)])

    :db-test
    (let [result (test-db/test-connection (db/specialize msg))]
      (async/put! publish-ch [:db-test-result result]))

    :db-save
    (do (db/put msg)
        (async/put! publish-ch [:db-change (db/get)]))

    :job-list
    (async/put! publish-ch [:job-change (job/all jobs)])

    :job-run
    (do (job/create jobs (db/spec) (sql/one msg) control-ch)
        (async/put! publish-ch [:job-change (job/all jobs)]))

    :job-get
    (async/put! publish-ch [:job-get (job/one jobs (Long/parseLong msg))])

    :job-delete
    (do (job/delete! jobs (str msg))
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
  [jobs control-ch publish-ch]
  (async/go-loop []
    (when-let [msg (async/<! control-ch)]
      (log/info "control-loop: " (normalize msg))
      (process! jobs control-ch publish-ch (normalize msg))
      (recur))))

(defn- message-handler
  [channel-hub]
  (fn [request]
    (httpd/with-channel request web-channel
      (swap! channel-hub assoc web-channel request)
      (httpd/on-close web-channel (fn [status]
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
     {:status 201 :body "{}"})

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
  (let [publish-ch (async/chan)
        control-ch (async/chan)
        channel-hub (atom {})]
    (publish-loop! channel-hub publish-ch)
    (control-loop! jobs control-ch publish-ch)
    (fn [request]
      ((-> (main-routes jobs channel-hub publish-ch control-ch)
           (site))
       request))))
