(defproject kovacnica/clojure.network.ip "0.1.3-SNAPSHOT"
  :description "Clojure/Clojurescript library for IP address and Network operations."
  :target-path "target/%s/"
  :compile-path "%s/classy-files"
  :url "https://github.com/kovacnica/clojure.networking"
  :source-paths ["src"]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.122"]]
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :cljsbuild {:builds [{:id "network"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main "clojure.network.ip"
                                   :asset-path "js/out"
                                   :output-to "resources/public/js/network.js"
                                   :output-dir "resources/public/js/out"}}]})
