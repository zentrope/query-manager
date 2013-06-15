(defproject queryizer "0.2"
  :description "query stuff"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.2.0-SNAPSHOT"]
                 [ring/ring-jetty-adapter "1.2.0-SNAPSHOT"]
                 [compojure "1.2.0-SNAPSHOT"]
                 [hiccup "1.0.3"]
                 [org.clojure/data.json "0.2.2"]

                 [org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.0.7"]

                 [mysql/mysql-connector-java "5.1.25"]    ;; mysql driver
                 [korma "0.3.0-RC5"]]

  :main queryizer.core)
