(defproject kovacnica/clojure.network "0.1.0"
  :description "FIXME: write description"
  :target-path "target/%s/"
  :compile-path "%s/classy-files"
  :url "http://example.com/FIXME"
  :source-paths ["src"]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.122"]]
  :cljsbuild {
              :builds [{:id "network"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main "clojure.network.ip"
                                   :asset-path "js/out"
                                   :output-to "resources/public/js/network.js"
                                   :output-dir "resources/public/js/out"}}]})
