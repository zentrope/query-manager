(defproject com.zentrope/query-manager "2.0.10"

  :url "https://github.com/zentrope/query-manager"
  :license {:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Submitting, monitoring and viewing the results of long-running queries."

  :dependencies [[org.clojure/clojure "1.5.1"]

                 ;; Web
                 [http-kit "2.1.16"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.4"]
                 [org.clojure/data.json "0.2.4"]
                 [javax.servlet/servlet-api "3.0-alpha-1"]

                 ;; Logging
                 [org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.0.13"]

                 ;; Utils
                 [me.raynes/fs "1.4.5"]

                 ;; Database
                 [org.clojure/java.jdbc "0.3.2"]           ;; jdbc
                 [mysql/mysql-connector-java "5.1.28"]     ;; mysql
                 [net.sourceforge.jtds/jtds "1.2.8"]       ;; sql-server (java.6 compat)
                 [com.h2database/h2 "1.3.175"]             ;; h2 database
                 [postgresql/postgresql "9.1-901-1.jdbc4"] ;; postgresql
                 [org.clojars.zentrope/ojdbc "11.2.0.3.0"] ;; oracle

                 ;; ClojureScript
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [prismatic/dommy "0.1.2" :exclusions [crate prismatic/cljs-test]]]

  :plugins [[lein-cljsbuild "1.0.1"]]

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

  :scm {:name "git" :url "https://github.com/zentrope/query-manager"}
  :jvm-opts ["-Dapple.awt.UIElement=true"]
  :min-lein-version "2.3.4"
  :profiles {:dev {:dependencies [[org.clojure/tools.nrepl "0.2.3"]
                                  [clojure-complete "0.2.3"]]}
             :uberjar {:aot [query-manager.main]}}
  :main ^:skip-aot query-manager.main)
