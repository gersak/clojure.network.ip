(defproject clojure.network "0.1.0-beta3"
  :description "FIXME: write description"
  :aot [clojure.network.l3.ip.Network
        clojure.network.l3.ip.IPAddress]
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 ;;[jpcap/jpcap "1.0"]
                 [commons-net/commons-net "3.0.1"]])
