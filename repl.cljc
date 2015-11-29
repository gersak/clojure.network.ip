(require
  '[cljs.repl :as repl]
  '[cljs.repl.node :as node] )

#_(repl/repl*  (rhino/repl-env)
            {:output-dir "out"
             :optimizations :none
             :cache-analysis true
             :source-map true})

(repl/repl*  (node/repl-env)
            {:output-dir "out"
             :optimizations :none
             :cache-analysis true
             :source-map true})
