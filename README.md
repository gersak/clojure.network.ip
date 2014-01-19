# clojure.network

A Clojure library designed to ... well, that part is up to you.

## Usage

In develompent in linux:

For usage of jpcap library with leiningen java process has to have cap_net_raw,cap_net_admin capabilitites.
This means that java jre execuatable file has to be set with setcap command:

sudo setcap cap_net_raw,cap_net_admin,cap_dac_override+eip  /usr/lib/jvm/java-7-oracle/jre/bin/java 

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
