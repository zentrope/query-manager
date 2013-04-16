(defproject queryizer "0.0.1"
  :description "query stuff"
  :dependencies
    [[org.clojure/clojure "1.2.0"]
     [org.clojure/clojure-contrib "1.2.0"]
     [ring/ring-core "0.2.5"]
     [ring/ring-devel "0.2.5"]
     [ring/ring-jetty-adapter "0.2.5"]
     [compojure "0.4.0"]
     [hiccup "0.2.6"]
     ;;[org.clojure/java.jdbc "0.0.6"]         ;; jdbc 
     ;;[mysql/mysql-connector-java "5.1.6"]    ;; mysql driver
     [korma "0.3.0-RC5"]]
  :dev-dependencies
    [[lein-run "1.0.0-SNAPSHOT"]]
  :main
   	queryizer.core)


