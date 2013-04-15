(ns queryizer.core
  (:use compojure.core)
  (:use hiccup.core)
  (:use hiccup.page-helpers)
  (:use ring.adapter.jetty)
  (:use ring.middleware.reload)
  (:use ring.middleware.stacktrace)
  (:use ring.util.response)
  (:use queryizer.middleware)
  (:use ring.middleware.file)
  (:use ring.middleware.file-info))

(def production?
  (= "production" (get (System/getenv) "APP_ENV")))

(def development?
  (not production?))

(defn view-layout [& content]
  (html
    (doctype :xhtml-strict)
    (xhtml-tag "en"
      [:head
        [:meta {:http-equiv "Content-type"
                :content "text/html; charset=utf-8"}]
        [:title "queryizer"]
        [:link {:href "/queryizer.css" :rel "stylesheet" :type "text/css"}]]
      [:body content])))

(defn view-input [& [a b]]
  (view-layout
    [:h2 "add two numbers"]
    [:form {:method "post" :action "/"}
      (if (and a b)
      	[:p "those are not both numbers!"])
      [:input.math {:type "text" :name "a"}] [:span.math " + "]
      [:input.math {:type "text" :name "b"}] [:br]
      [:input.action {:type "submit" :value "add"}]]))

(defn view-output [a b sum]
  (view-layout
    [:h2 "two numbers added"]
    [:p.math a " + " b " = " sum]
    [:a.action {:href "/"} "add more numbers"]))

(defn parse-input [a b]
  [(Integer/parseInt a) (Integer/parseInt b)])

(defroutes handler
  (GET "/" []
    (view-input))

  (POST "/" [a b]
    (try
      (let [[a b] (parse-input a b)
            sum   (+ a b)]
        (view-output a b sum))
      (catch NumberFormatException e
        (view-input a b))))
  (ANY "/*" [path]
    (redirect "/")))

(defn wrap-bounce-favicon [handler]
  (fn [req]
    (if (= [:get "/favicon.ico"] [(:request-method req) (:uri req)])
      {:status 404
       :headers {}
       :body ""}
      (handler req))))

(def app
  (-> #'handler
    (wrap-file "public")
    (wrap-file-info)
    (wrap-request-logging)
    (wrap-if development? wrap-reload '[queryizer.middleware queryizer.core])
    (wrap-bounce-favicon)
    (wrap-exception-logging)
    (wrap-if production?  wrap-failsafe)
    (wrap-if development? wrap-stacktrace)))



(defn -main [& args]
	(let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
	(run-jetty #'queryizer.core/app {:port port :join? true})
	(println "hello world!")))


