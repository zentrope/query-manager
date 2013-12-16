(ns query-manager.web
  (:require [org.httpkit.server :as httpd]
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
;; Response Handling
;;-----------------------------------------------------------------------------

(defn- wait-until-client-connected!
  [hub]
  (go-loop []
    (when (zero? (count @hub))
      (<! (timeout 10))
      (recur))))

(defn- response-loop!
  [hub event-queue]
  (go-loop []
    (when-let [msg (<! event-queue)]
      (<! (wait-until-client-connected! hub))
      (doseq [[web-chan req] @hub]
       (if (= (first msg) (:http-error msg))
         (httpd/send! web-chan (mk-response (second msg)))
         (httpd/send! web-chan (mk-response 200 (jwrite msg)))))
      (recur))))

(defn- message-handler
  [hub event-queue]
  (fn [request]
    (httpd/with-channel request web-chan
      (swap! hub assoc web-chan request)
      (httpd/on-close web-chan (fn [status] (swap! hub dissoc web-chan))))))

;;-----------------------------------------------------------------------------
;; Routing
;;-----------------------------------------------------------------------------

(defn- normalize
  [[topic msg]]
  [(keyword topic) msg])

(defn- main-routes
  [request-q response-q hub]
  (routes

   (GET "/qman/api/messages"
       []
     (message-handler hub response-q))

   (POST "/qman/api/messages"
       [:as r]
     (put! request-q (normalize (jread r)))
     {:status 201 :headers {"content-type" "application/json"} :body "{}"})

   (GET "/qman/queries/download"
       []
     (log/info " [[ exporting queries ]]")
     (-> (state/all-queries)
         (as-> queries
               (mapv #(dissoc % :id) queries)
               (with-out-str (clojure.pprint/pprint queries))
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
       [:title "Query Manager"]
       [:link {:rel "shortcut icon" :href "/qman/favicon.ico"}]
       (html/include-css "qman/styles.css")
       (html/include-js "qman/main.js")]
      [:body "Loading..."]))

   (route/resources "/")
   (route/not-found "<h1>Oops. Try <a href='/qman'>here</a>.</h1>")))

(defn- mk-app
  [{:keys [request-q response-q hub]}]
  (fn [request]
    ((-> (main-routes request-q response-q hub)
         (wrap-params)
         (wrap-keyword-params))
     request)))

;;-----------------------------------------------------------------------------
;; Service
;;-----------------------------------------------------------------------------

(defn make
  [port request-q response-q]
  (atom {:port port
         :request-q request-q
         :response-q response-q
         :hub (atom {})
         :httpd nil}))

(defn start!
  [this]
  (log/info "Starting web application on port:" (str (:port @this) "."))
  ;;
  (let [port (:port @this)
        app (mk-app @this)
        params {:port port :worker-name-prefix "httpkit-"}
        server (httpd/run-server app params)
        delegate (delegate-ch (:response-q @this))]
    (response-loop! (:hub @this) delegate)
    (swap! this assoc :httpd server :delegate delegate)))

(defn stop!
  [this]
  (log/info "Stopping web application.")
  ;;
  (when-let [server (:httpd @this)]
    (server))
  (close! (:delegate @this))
  (swap! this (fn [s] (assoc s :httpd nil))))
