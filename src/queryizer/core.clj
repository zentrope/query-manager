(ns queryizer.core
  (:require [queryizer.view :as views]
  	[ring.adapter.jetty :as jetty]))

(defn -main [& args]
	(let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
	(jetty/run-jetty #'views/app {:port port :join? true})
	(println "good bye!")))


