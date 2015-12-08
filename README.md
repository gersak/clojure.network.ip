# kovacnica/clojure.network.ip

A Clojure/Clojurescript library for IPv4 and IPv6 network and address operations. Main goal is to support easy ip address manipulation through +, -, first, last, counted operations.

## Installation

Add the following to the `:dependencies` section of your `project.clj` file:

```clj
[kovacnica/clojure.network.ip "0.1.0"]
```

## Usage

### Simple IP address manipulation
In namespace clojure.network.ip there are two types defined. IPAddress and Network. These types extend protocols IPInfo that contains 3 methods:

* `ip-address` - returns string representation of IPAddress
* `version` - Integer value that reflects IP version. Either 4 or 6
* `numeric-value` - method returns java.math.BigInteger( Clojure) or goog.math.Integer( ClojureScript)

Constructor for IPAddress record is `make-ip-address`. It takes single argument of following types:

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

## Network operations

## License

Copyright © 2015

Distributed under the Eclipse Public License, the same as Clojure.
