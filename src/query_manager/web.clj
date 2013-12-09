(ns query-manager.web
  (:require [org.httpkit.server :as httpd]
            [clojure.tools.logging :as log]
            [clojure.core.async :as async]
            [clojure.data.json :as json]
            [compojure.route :as route]
            [hiccup.page :as html]
            [compojure.core :refer [routes GET POST]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]))

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

;;-----------------------------------------------------------------------------
;; Response Handling
;;-----------------------------------------------------------------------------

(defn- wait-loop!
  "Block until the client has connected."
  [hub]
  (async/go-loop []
    (if (> (count @hub) 0)
      :done
      (do (async/<! (async/timeout 10))
          (recur)))))

(defn- message-handler
  [hub event-queue]
  (fn [request]
    (httpd/with-channel request web-chan
      (swap! hub assoc web-chan request)
      (httpd/on-close web-chan (fn [status]
                                 (swap! hub dissoc web-chan)
                                 (when-not (= status :server-close)
                                   (log/warn "abnormal connection close:" status))))
      (async/go
       (when-let [msg (<! event-queue)]
         (async/<! (wait-loop! hub))
         (doseq [[web-chan req] @hub]
           (if (= (first msg) (:http-error msg))
             (httpd/send! web-chan (mk-response (second msg)))
             (httpd/send! web-chan (mk-response 200 (jwrite msg))))))))))

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
     (async/put! request-q (normalize (jread r)))
     {:status 201 :headers {"content-type" "application/json"} :body "{}"})

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
        server (httpd/run-server app params)]
    (swap! this assoc :httpd server)))

(defn stop!
  [this]
  (log/info "Stopping web application.")
  ;;
  (when-let [server (:httpd @this)]
    (server))
  (swap! this (fn [s] (assoc s :httpd nil))))
