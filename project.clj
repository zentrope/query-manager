(defproject queryizer "0.0.1"
  :description "query stuff"
  :dependencies
    [[org.clojure/clojure "1.5.1"]
     [ring/ring-core "1.2.0-SNAPSHOT"]
     [ring/ring-devel "1.2.0-SNAPSHOT"]
     [ring/ring-jetty-adapter "1.2.0-SNAPSHOT"]
     [compojure/compojure "1.2.0-SNAPSHOT"]
     [hiccup/hiccup "1.0.3"]
     [mysql/mysql-connector-java "5.1.24"]    ;; mysql driver
     [korma "0.3.0-RC5"]
     [org.clojure/data.json "0.2.2"]]
  :dev-dependencies
    [[lein-run "1.0.0-SNAPSHOT"]]
  :main
   	queryizer.core
  :profiles {:user {:plugins [[lein-outdated "1.0.0"]]}})


