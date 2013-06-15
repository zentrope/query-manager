(defproject queryizer "0.2"
  :description "Submitting, monitoring and viewing the results of long-running queries."
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.2.0-SNAPSHOT"]
                 [ring/ring-jetty-adapter "1.2.0-SNAPSHOT"]
                 [compojure "1.2.0-SNAPSHOT"]
                 [hiccup "1.0.3"]
                 [org.clojure/data.json "0.2.2"]

                 [org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.0.7"]

                 [org.clojure/java.jdbc "0.3.0-alpha4"]  ;; jdbc
                 [mysql/mysql-connector-java "5.1.25"]   ;; mysql driver
                 [net.sourceforge.jtds/jtds "1.2.4"]     ;; sql-server
                 [hsqldb/hsqldb "1.8.0.10"]              ;; hsqldb
                 [postgresql/postgresql "8.4-702.jdbc4"] ;; postgresql

                 [org.clojure/tools.nrepl "0.2.3"]]

  :min-lein-version "2.2.0"
  :main queryizer.main)
