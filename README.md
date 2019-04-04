# kovacnica/clojure.network.ip

A Clojure/Clojurescript library for IPv4 and IPv6 network and address operations. Main goal is to support easy ip address manipulation through +, -, first, last, counted operations.

## Installation

Add the following to the `:dependencies` section of your `project.clj` file:

## Current version:
[![Clojars Project](https://img.shields.io/clojars/v/kovacnica/clojure.network.ip.svg)](https://clojars.org/kovacnica/clojure.network.ip)
[![Clojars Project](https://img.shields.io/clojars/v/kovacnica/clojure.network.ip.svg)](https://clojars.org/kovacnica/clojure.network.ip)

## Usage

### Simple IP address manipulation
In namespace clojure.network.ip there are two types defined. IPAddress and Network. These types extend protocols IPInfo that contains 3 methods:

* `ip-address` - returns string representation of IPAddress
* `version` - Integer value that reflects IP version. Either 4 or 6
* `numeric-value` - method returns java.math.BigInteger( Clojure) or goog.math.Integer( ClojureScript)

Constructor for IPAddress type is `make-ip-address`. It takes single argument of following types:

* `Clojure` - BigInteger, IPAddress, Network, Byte [], String, BigInt
* `ClojureScript` - IPAddress, Network, String, goog.math.Integer, number

```clj
user=> (use 'clojure.network.ip)
nil
user=> (def ip (make-ip-address "192.168.100.1"))
#'user/ip
user=> ip
IP Address: 192.168.100.1
Version: 4
```

Once IPAddress has been constructed we can call listed methods upon it:

```clj
user=> (ip-address ip)
"192.168.100.1"
user=> (version ip)
4
user=> (numeric-value ip)
-1062706175
user=> (str ip)
"192.168.100.1"
```

Through numeric value we can use standard operations inc,dec,+,-,= on IPAddress

```clj
user=> (-> ip numeric-value (+ 100) make-ip-address)
IP Address: 192.168.100.101
Version: 4

user=> (-> ip numeric-value (- 100) make-ip-address)
IP Address: 192.168.99.157
Version: 4

user=> (-> ip numeric-value inc make-ip-address)
IP Address: 192.168.100.2
Version: 4

user=> (-> ip numeric-value dec make-ip-address)
IP Address: 192.168.100.0
Version: 4
```

## Basic Network operations

Networks are constructed through make-network method. Method supports upto 2 arguments. One arrity method accepts only String representation of network. i.e. "192.168.100.1/20", "FE80::0202:B3FF:FE1E:8329/120". For two arrity input method accepts different combinations. i.e. `(make-network "192.168.100.1" 24) (make-network "192.168.100.1" "24") (make-network "192.168.100.1" "255.255.255.0") ` . For first argument `make-network` calls `make-ip-address` so basically everything that can be cast to IPAddress is valid argument. Second argument can be either Integer or String that reflects subnet mask of network.

```clj
user=> (def ipv6 (make-ip-address "FE80::0202:B3FF:FE1E:8329"))
#'user/ipv6

user=> (make-network ipv6 120)
Network: fe80:0:0:0:202:b3ff:fe1e:8329/120
Network address: fe80:0:0:0:202:b3ff:fe1e:8300
Broadcast address: fe80:0:0:0:202:b3ff:fe1e:83ff
Address count: 256

;; ip-address function returns Network[ip subnet] ip part cast to String
user=> (ip-address *1)
"fe80:0:0:0:202:b3ff:fe1e:8329"
```

It is possible to compare networks

```clj
user=> (def n1 (make-network "192.168.100.1/24"))
#'user/n1
user=> (def n2 (make-network "192.168.100.240/24"))
#'user/n2
user=> (def n3 (make-network "192.168.101.1/24"))
#'user/n3
user=> (= n1 n2)
true
user=> (= n1 n3)
false
user=> (= n2 n3)
false
```

And get all addresses in certain network:

```clj
user=> (seq n1)
(IP Address: 192.168.100.0
Version: 4
 IP Address: 192.168.100.1
Version: 4
 IP Address: 192.168.100.2
Version: 4
 IP Address: 192.168.100.3
Version: 4
 IP Address: 192.168.100.4
Version: 4
 IP Address: 192.168.100.5
Version: 4
 IP Address: 192.168.100.6
Version: 4
 IP Address: 192.168.100.7
Version: 4
 IP Address: 192.168.100.8
Version: 4
 IP Address: 192.168.100.9
Version: 4
 IP Address: 192.168.100.10
.
.
.
 IP Address: 192.168.100.253
Version: 4
 IP Address: 192.168.100.254
Version: 4
 IP Address: 192.168.100.255
Version: 4
)
```

clojure.network.ip.Network type mimics clojure.lang.PersistetSet of fixed size. This enables some nice features like:

```clj
user=> (first n1)
IP Address: 192.168.100.0
Version: 4

user=> (last n1)
IP Address: 192.168.100.255
Version: 4

user=> (nth n1 100)
IP Address: 192.168.100.100
Version: 4


user=> (-> n1 (nth 100) numeric-value (+ 20) (make-network 24))
Network: 192.168.100.120/24
Network address: 192.168.100.0
Broadcast address: 192.168.100.255
Address count: 256

user=> (contains? (make-network "192.168.100.110/28") "192.168.100.101")
true

;; You can easily filter or remove addresses of target subnet

user=> (filter (partial contains? (make-network "192.168.100.110/28")) (seq n1))
(IP Address: 192.168.100.96
Version: 4
 IP Address: 192.168.100.97
Version: 4
 IP Address: 192.168.100.98
Version: 4
 IP Address: 192.168.100.99
Version: 4
 IP Address: 192.168.100.100
Version: 4
 IP Address: 192.168.100.101
Version: 4
 IP Address: 192.168.100.102
Version: 4
 IP Address: 192.168.100.103
Version: 4
 IP Address: 192.168.100.104
Version: 4
 IP Address: 192.168.100.105
Version: 4
 IP Address: 192.168.100.106
Version: 4
 IP Address: 192.168.100.107
Version: 4
 IP Address: 192.168.100.108
Version: 4
 IP Address: 192.168.100.109
Version: 4
 IP Address: 192.168.100.110
Version: 4
 IP Address: 192.168.100.111
Version: 4
)

```

## Advanced network operations

There are some advanced operations for Network type.

### Devide network

```clj
;; Splits target network onto multiple parts


user=> (devide-network n1 2)
(Network: 192.168.100.1/25
Network address: 192.168.100.0
Broadcast address: 192.168.100.127
Address count: 128
 Network: 192.168.100.129/25
Network address: 192.168.100.128
Broadcast address: 192.168.100.255
Address count: 128
)


user=> (devide-network n1 4)
(Network: 192.168.100.1/26
Network address: 192.168.100.0
Broadcast address: 192.168.100.63
Address count: 64
 Network: 192.168.100.65/26
Network address: 192.168.100.64
Broadcast address: 192.168.100.127
Address count: 64
 Network: 192.168.100.129/26
Network address: 192.168.100.128
Broadcast address: 192.168.100.191
Address count: 64
 Network: 192.168.100.193/26
Network address: 192.168.100.192
Broadcast address: 192.168.100.255
Address count: 64
)
```
### Break network

```clj
;; Function breaks target-network( n1) to multiple networks of second argument count( 16)

user=> (break-network n1 16)
(Network: 192.168.100.1/28
Network address: 192.168.100.0
Broadcast address: 192.168.100.15
Address count: 16
 Network: 192.168.100.17/28
Network address: 192.168.100.16
Broadcast address: 192.168.100.31
Address count: 16
 Network: 192.168.100.33/28
Network address: 192.168.100.32
Broadcast address: 192.168.100.47
Address count: 16
.
.
.
Broadcast address: 192.168.100.175
Address count: 16
 Network: 192.168.100.177/28
Network address: 192.168.100.176
Broadcast address: 192.168.100.191
Address count: 16
 Network: 192.168.100.193/28
Network address: 192.168.100.192
Broadcast address: 192.168.100.207
Address count: 16
 Network: 192.168.100.209/28
Network address: 192.168.100.208
Broadcast address: 192.168.100.223
Address count: 16
 Network: 192.168.100.225/28
Network address: 192.168.100.224
Broadcast address: 192.168.100.239
Address count: 16
 Network: 192.168.100.241/28
Network address: 192.168.100.240
Broadcast address: 192.168.100.255
Address count: 16
)
```


## License

Copyright © 2015-2018 Robert Gersak

Distributed under the Eclipse Public License, the same as Clojure.
