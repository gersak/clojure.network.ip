(ns clojure.network.ip.v4
  (:import [java.net InetAddress Inet4Address Inet6Address]
           [org.apache.commons.net.util SubnetUtils]
           [java.math BigInteger]))


(defprotocol IpAddress
  (ip-address [t]))

(defprotocol Networkfy
  (network [t]))

(defn- make-ip-address [#^bytes ip]
  (InetAddress/getByAddress ip))

(defn- get-mark [#^bytes ip-address ^Integer subnet]
  (if (<= (count ip-address) 4) (- 32 subnet) (- 128 subnet)))

(defn- get-network-address [#^bytes ip-address ^Integer subnet]
  "Function takes ip address and subnet and returns network
  IP address in byte-array form"
  (let [mark (get-mark ip-address subnet)
        ip (BigInteger. ip-address)]
    (-> (.shiftRight ip mark) (.shiftLeft mark) (.toByteArray))))

(defn- get-max-address  [#^bytes ip-address ^Integer subnet]
  "Returns maximum ip address for given ip-address and subnet.
  In other words, maximum address of network"
  (let [mark (get-mark ip-address subnet)
        ip (BigInteger. ip-address)
        mask (BigInteger/valueOf (dec (bit-shift-left 1 mark)))]
    (.toByteArray (.or ip mask))))

(defn- get-all-addresses-helper [#^bytes ip-address ^Integer subnet]
  (let [high-address (.add (BigInteger. (get-max-address ip-address subnet)) (BigInteger/ONE))
        low-address (BigInteger. (get-network-address ip-address subnet))
        r (range (.longValue (.subtract high-address low-address)))
        addresses (map #(.add low-address (BigInteger/valueOf %)) r)]
    (mapv #(-> % (.toByteArray) make-ip-address (.getHostAddress)) addresses)))


(defn print-bytes [x]
  (java.lang.Long/toBinaryString x))

(defn ba2long [#^bytes b-value]
  (let [length (count b-value)
        args (partition 2 (interleave b-value (map #(* 4 %) (-> length range reverse))))
        helper-fn (fn [long-number [byte-m shift]] (bit-xor long-number (bit-shift-left byte-m shift)))]
    (reduce helper-fn 0 args)))

;;(defn ip-formatter [#^bytes b-value]
;;  (if (>= 4 (count b-value))
;;    (loop [b b-value
;;           r nil]
;;      (if (seq b)
;;        (recur (rest b) (conj r (bit-and (first b) 0xff)))
;;        (apply str (interpose "." (reverse r)))))
;;    (let [s (map #(.toHexString %) b-value)]
;;      s)))

(defn ipv4-formatter [#^java.lang.Long value]
  (loop [c 0 
         r nil]
    (if (< c 4) 
        (recur (inc c) (conj r (bit-shift-right (bit-and value (bit-shift-left 0xff (* 8 c))) (* 8 c))))
      (apply str (interpose "." r)))))

;; Ne Diraj
;;(defn- get-all-addresses-helper [#^bytes ip-address ^Integer subnet]
;;  (let [high-address (.add (BigInteger. (get-max-address ip-address subnet)) (BigInteger/ONE))
;;        low-address (BigInteger. (get-network-address ip-address subnet))
;;    ;;    r (range (.longValue (.subtract high-address low-address)))
;;    ;;    addresses (map #(.add low-address (BigInteger/valueOf %)) r)]
;;    ;;(mapv #(-> % (.toByteArray) make-ip-address (.getHostAddress)) addresses)))
;;        inc-bingint (fn [x] (.add x (BigInteger/ONE)))
;;        c (atom low-address)
;;        result (atom [low-address])]
;;    (while (<= @c high-address)
;;      (swap! result #(conj % (.getHostAddress (make-ip-address (.toByteArray (swap! c inc-bingint)))))))))



(defn- is-in-network?-helper [#^bytes ip-address ^Integer subnet ^String q-address]
  (let [q-address (BigInteger. (.getAddress (InetAddress/getByName q-address)))
        h-address (BigInteger. (get-max-address ip-address subnet))
        l-address (BigInteger. (get-network-address ip-address subnet))]
    (boolean (and (>= h-address q-address) (<= l-address q-address)))))


(defprotocol NetworkCommon
  (get-subnet [this] "Returns subnet in String form")
  (get-highest-address [this] "Returns highest address in String form")
  (get-lowest-address [this] "Returns lowest address in String from")
  (get-network [this] "Returns CIDR notated String form")
  (is-in-network? [this ^String ip-address] "Evaluates if ip-address is in this network")
  (get-ip [this] "Returns IP address")
  (get-all-addresses [this] [this inclusive?] "Returns seq of addresses in this network. If second argument
                                              is true than ALL adresses are returned. If false, than addresses
                                              without broadcast and network ip addresses are returned")
  (get-host-name [this] "Evaluates hostname of this IP address"))

(defrecord Network [^InetAddress ip-address ^Integer subnet]
  NetworkCommon
  (get-subnet [_] subnet)
  (get-highest-address [_] (-> (get-max-address (.getAddress ip-address) subnet) make-ip-address (.getHostAddress)))
  (get-lowest-address [this] (-> (get-network-address (.getAddress (:ip-address this)) (:subnet this)) make-ip-address (.getHostAddress)))
  (get-network [_] (-> (get-network-address (.getAddress ip-address) subnet) make-ip-address (.getHostAddress) (str "/" subnet)))
  (is-in-network? [_ q-address] (is-in-network?-helper (.getAddress ip-address) subnet q-address))
  (get-ip [_] (.getHostAddress ip-address))
  (get-all-addresses [this inclusive?]
    (let [addresses (get-all-addresses-helper (.getAddress ip-address) subnet)]
      (if inclusive? addresses
        (rest (butlast addresses)))))
  (get-all-addresses [this] (get-all-addresses this false))
  (get-host-name [_] (.getHostName ip-address)))



(extend java.lang.String
  IpAddress
  {:ip-address #(java.net.InetAddress/getByName %)}
  Networkfy
  {:network #(let [[ip sub] (clojure.string/split % #"/")
                   sub (Integer/parseInt sub)
                   ip (ip-address ip)] 
               (if (instance? Inet4Address ip) 
                 (do (assert (and (<= sub 32) (pos? sub)) "Subnet mask IPv4 is limited to 0-32 bits.")
                     (Network. ip sub))
                 (do (assert (and (<= sub 128) (pos? sub)) "Subnet mask IPv6 is limited to 0-128 bits.")
                     (Network. ip sub))))})


(def test-network "192.168.0.253/16")
;;(def test-network "2001:0db8:0000:0000:0000:ff00:0042:8329/120")
(def x (network test-network))

(defn apache-test []
  (let [addresses (time (.. (SubnetUtils. test-network) (getInfo) (getAllAddresses)))]
    (println "Address count:" (count addresses))))

(defn my-test []
  (let [addresses (time (get-all-addresses (network test-network)))]
    (println "Address count:" (count addresses))))


(defn compare-speed []
  (println "APACHE:")
  (apache-test)
  (println "MY:")
  (my-test))
