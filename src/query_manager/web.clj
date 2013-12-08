(ns query-manager.web
  (:require [org.httpkit.server :as httpd]
            [clojure.tools.logging :as log]
            [clojure.core.async :as async]
            [clojure.data.json :as json]
            [compojure.core :refer [routes GET POST]]
            [compojure.handler :refer [site]]
            [compojure.route :refer [resources not-found]]
            [hiccup.page :refer [html5 include-js include-css]]))

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

(defn- mk-response
  ([status]
     (mk-response "{}"))
  ([status body]
     {:status status
      :body body
      :headers {"Content-Type" "application/json"}}))

;;-----------------------------------------------------------------------------
;; Publishing
;;-----------------------------------------------------------------------------

(defn- wait-loop!
  [channel-hub]
  (async/go-loop []
    (if (> (count @channel-hub) 0)
      :done
      (do (async/<! (async/timeout 10))
          (recur)))))

(defn- publish-loop!
  [channel-hub pub-q]
  (async/go-loop []
    (when-let [msg (async/<! pub-q)]
      (<! (wait-loop! channel-hub))
      (doseq [[web-channel req] @channel-hub]
        (if (= (first msg) (:http-error msg))
          (httpd/send! web-channel (mk-response (second msg)))
          (httpd/send! web-channel (mk-response 200 (jwrite msg)))))
      (recur))))

(defn- message-handler
  [channel-hub]
  (fn [request]
    (httpd/with-channel request web-channel
      (swap! channel-hub assoc web-channel request)
      (httpd/on-close web-channel (fn [status]
                                    (log/debug " - closing" web-channel)
                                    (swap! channel-hub dissoc web-channel))))))

;;-----------------------------------------------------------------------------
;; Routing
;;-----------------------------------------------------------------------------

(defn- normalize
  [[topic msg]]
  [(keyword topic) msg])

(defn- main-routes
  [client]
  (routes

   (GET "/qmain/api/messages"
       []
     (message-handler (:hub @client)))

   (POST "/qman/api/messages"
       [:as r]
     (async/put! (:event-q @client) (normalize (jread r)))
     {:status 201 :headers {"content-type" "application/json"} :body "{}"})

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

;;-----------------------------------------------------------------------------
;; Service
;;-----------------------------------------------------------------------------

(defn- mk-app
  [client]
  (fn [request]
    ((-> (main-routes client)
         (site))
     request)))

(defn publisher
  [client]
  (:pub-q @client))

(defn client
  [port event-q]
  (atom {:port port
         :event-q event-q
         :hub (atom {})
         :httpd nil
         :pub-q (async/chan)}))

(defn start!
  [client]
  (log/info "DID YOU REMEMBER TO START THE PUB LOOP?")
  (log/info "Starting web application on port: " (str (:port @client) "."))
  (let [server (httpd/run-server (mk-app client) {:port (:port @client)})]
    (swap! client assoc :httpd server)))

(defn stop!
  [client]
  (log/info "Stopping web application.")
  (when-let [server (:httpd @client)]
    (server))
  (reset! client (client (:port @client) (:event-q @client))))
