(ns query-manager.web
  (:require
    [org.httpkit.server :as httpd]
    [clojure.tools.logging :as log]
    [clojure.data.json :as json]
    [compojure.route :as route]
    [hiccup.page :as html]
    [clojure.core.async :refer [go-loop <! timeout go put! chan close!]]
    [compojure.core :refer [routes GET POST]]
    [ring.util.response :refer [redirect status response header]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [query-manager.state :as state]))

;;-----------------------------------------------------------------------------
;; Utilities
;;-----------------------------------------------------------------------------

(defn- sread
  [stream]
  (if-not (string? stream)
    (with-open [r (clojure.java.io/reader stream)]
      (slurp r))
    stream))

(defn- jread
  [request]
  (json/read-str (sread (:body request)) :key-fn keyword))

(defn- jconvert
  [k v]
  (cond
    (instance? java.sql.Timestamp v)           (str v)
    (and (number? v) (< v 0))                  (str v)
    (and (instance? java.lang.Long v) (< v 0)) (str v)
    :else                                      v))

(defn- jwrite
  [value]
  (json/write-str value :value-fn jconvert))

(defn- mk-response
  ([status]
     (mk-response "{}"))
  ([status body]
     {:status status
      :body body
      :headers {"Content-Type" "application/json"}}))

(defn- delegate-ch
  [response-q]
  (let [delegate (chan)]
    (go-loop []
      (when-let [value (<! response-q)]
        (put! delegate value)
        (recur)))
    delegate))

;;-----------------------------------------------------------------------------
;; Sessions
;;-----------------------------------------------------------------------------

(defn- drop-session!
  [sessions client-id]
  (when-let [c (client-id @sessions)]
    (close! c)
    (log/info "[session] dropping session for" client-id)
    (swap! sessions dissoc client-id)))

(defn- create-session!
  [sessions client-id]
  (when (nil? (client-id @sessions))
    (log/info "[session] creating session for" client-id)
    (let [c (chan)]
      (swap! sessions assoc client-id c)
      c)))

(defn- get-session
  "Returns a specific channel."
  [sessions client-id]
  (if-let [c (client-id @sessions)]
    c
    (create-session! sessions client-id)))

;;-----------------------------------------------------------------------------
;; Response Handling
;;-----------------------------------------------------------------------------

(defn- response-loop!
  [sessions event-queue]
  ;;
  ;; Wait for incoming messages from the event queue, then distribute
  ;; them to all the available sessions.
  ;;
  (go-loop []
    (when-let [msg (<! event-queue)]
      (doseq [[client-id ch] @sessions]
        (put! ch msg))
      (recur))))

(defn- fulfill-request
  [sessions web-chan client-id queue]
  ;;
  ;; For every request that comes in, set up a go block to respond
  ;; when something comes in on the event queue.
  ;;
  (go
    (when-let [msg (<! queue)]
      (if (= (first msg) (:http-error msg))
        (httpd/send! web-chan (mk-response (second msg)))
        (httpd/send! web-chan (mk-response 200 (jwrite msg)))))))

(defn- message-handler
  [sessions event-queue client-id]
  (fn [request]
    (httpd/with-channel request web-chan
      (httpd/on-close web-chan
        (fn [status]
          (when (= status :client-close)
            (drop-session! sessions client-id))))
      (fulfill-request sessions web-chan client-id (get-session sessions client-id)))))

;;-----------------------------------------------------------------------------
;; Routing
;;-----------------------------------------------------------------------------

(defn- no-cache
  [handler]
  (fn [request]
    (-> (handler request)
        (header "cache-control" "no-cache")
        (header "expires" "-1"))))

(defn- normalize
  [[topic msg]]
  [(keyword topic) msg])

(defn- main-routes
  [request-q sessions app-title]
  (routes

   (GET "/qman/api/messages" [:as r]
     (let [client-id (keyword (get-in r [:headers "client-id"]))]
       (message-handler sessions request-q client-id)))

   (POST "/qman/api/messages" [:as r]
     (when-let [client-id (keyword (get-in r [:headers "client-id"]))]
       (create-session! sessions client-id))
     (put! request-q (normalize (jread r)))
     {:status 201 :headers {"content-type" "application/json"} :body "{}"})

   (GET "/qman/queries/download"
       []
     (log/info " [[ exporting queries ]]")
     (-> (state/all-queries)
         (as-> queries
               (sort-by :id queries)
               (for [q queries] (with-out-str (clojure.pprint/pprint q)))
               (str "[\n" (clojure.string/join ",\n" queries) "\n]")
               (clojure.string/replace (str queries) #"\\n" "\n"))
         (response)
         (status 200)
         (header "Content-Type" "application/octet-stream")
         (header "Content-Disposition" "attachment;filename=\"queries.clj\"")))

   (GET "/qman/archive/:archive-name/download"
       [archive-name]
     (log/info " [[ exporting file:" archive-name "]]")
     (-> (response (state/archive-file archive-name))
         (status 200)
         (header "Content-Type" "application/zip")
         (header "Content-Disposition"
                 (str "attachment;filename=\"" archive-name "\""))))

   (GET "/qman"
       []
     (html/html5
      [:head
       [:title app-title]
       [:meta {:charset "utf-8"}]
       [:meta {:http-quiv "X-UA-Compatible" :content "IE=edge"}]
       [:link {:rel "shortcut icon" :href "/qman/favicon.ico"}]
       (html/include-css "qman/styles.css")
       (html/include-js "qman/main.js")]
      [:body "Loading..."]))

   (route/resources "/")
   (route/not-found "<h1>Oops. Try <a href='/qman'>here</a>.</h1>")))

(defn- mk-app
  [{:keys [request-q app-title sessions]}]
  (fn [request]
    ((-> (main-routes request-q sessions app-title)
         (wrap-params)
         (wrap-keyword-params)
         (no-cache))
     request)))

;;-----------------------------------------------------------------------------
;; Service
;;-----------------------------------------------------------------------------

(defn make
  [app-title port request-q response-q]
  (atom {:port (if (string? port) (Integer/parseInt port) port)
         :request-q request-q
         :response-q response-q
         :sessions (atom {})
         :app-title app-title
         :httpd nil}))

(defn start!
  [this]
  (log/info (format "Starting web application: http://localhost:%s/qman." (:port @this)))
  ;;
  (let [port (:port @this)
        app (mk-app @this)
        params {:port port :worker-name-prefix "http-"}
        server (httpd/run-server app params)
        delegate (delegate-ch (:response-q @this))]
    (response-loop! (:sessions @this) delegate)
    (swap! this assoc :httpd server :delegate delegate)))

(defn stop!
  [this]
  (log/info "Stopping web application.")
  ;;
  (when-let [server (:httpd @this)]
    (server))
  (close! (:delegate @this))
  (swap! this (fn [s] (assoc s :httpd nil))))
