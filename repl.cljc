(require
  '[cljs.repl :as repl]
  '[cljs.repl.rhino :as rhino]
  '[cljs.repl.node :as node] )

(repl/repl*  (rhino/repl-env)
            {:output-dir "out"
             :optimizations :none
             :cache-analysis true
             :source-map true})

#_(repl/repl*  (node/repl-env)
            {:output-dir "out"
             :optimizations :none
             :cache-analysis true
             :source-map true})
