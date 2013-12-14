(defproject query-manager "2"

  :description "Submitting, monitoring and viewing the results of long-running queries."

  :dependencies [[org.clojure/clojure "1.5.1"]

                 ;; Web
                 [http-kit "2.1.13"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.4"]
                 [org.clojure/data.json "0.2.3"]
                 [javax.servlet/servlet-api "3.0-alpha-1"]

                 ;; Logging
                 [org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.0.13"]

                 ;; Utils
                 [me.raynes/fs "1.4.4"]

                 ;; Database
                 [org.clojure/java.jdbc "0.3.0-rc1"]       ;; jdbc
                 [mysql/mysql-connector-java "5.1.27"]     ;; mysql
                 [net.sourceforge.jtds/jtds "1.2.8"]       ;; sql-server (java.6 compat)
                 [com.h2database/h2 "1.3.174"]             ;; h2 database
                 [postgresql/postgresql "9.1-901-1.jdbc4"] ;; postgresql
                 [org.clojars.zentrope/ojdbc "11.2.0.3.0"] ;; oracle

                 ;; ClojureScript
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/clojurescript "0.0-2120"]
                 [prismatic/dommy "0.1.2"]]

  :plugins [[lein-cljsbuild "1.0.0"]]

  :hooks [leiningen.cljsbuild]

  :cljsbuild {:builds {:dev
                       {:source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/qman/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}
                       :prod
                       {:source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/qman/main.js"
                                   :optimizations :advanced
                                   :externs ["resources/externs.js"]
                                   :pretty-print false}}}}

  :jvm-opts ["-Dapple.awt.UIElement=true"]
  :min-lein-version "2.3.4"
  :profiles {:offline {:offline? true}
             :uberjar {:aot [query-manager.main]}}
  :main query-manager.main)
