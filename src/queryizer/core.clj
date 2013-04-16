(ns queryizer.core
  (:use queryizer.view)
  (:use ring.adapter.jetty))

(defn -main [& args]
	(let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
	(run-jetty #'queryizer.view/app {:port port :join? true})
	(println "good bye!")))


