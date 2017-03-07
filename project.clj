(defproject ethlance-emailer "0.1.0-SNAPSHOT"
  :description "Process listens for Ethlance events and sends notifications emails"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[bidi "2.0.14"]
                 [camel-snake-kebab "0.4.0"]
                 [cljs-web3 "0.18.2-0"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [com.cemerick/piggieback "0.2.1"]
                 [figwheel-sidecar "0.5.9"]
                 [medley "0.8.3"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.456"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [print-foo-cljs "2.0.3"]]
  :plugins [[lein-npm "0.6.2"]
            [lein-cljsbuild "1.1.5"]
            [lein-figwheel "0.5.9"]]
  :npm {:dependencies [[source-map-support "0.4.0"]
                       [web3 "0.18.2"]
                       [ws "2.0.1"]
                       [solidity-sha3 "0.4.1"]
                       [sendgrid "4.7.1"]
                       [node-schedule "1.2.0"]]}
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :source-paths ["src" "target/classes" "dev"]
  :cljsbuild {:builds
              {:main {                                      ;:notify-command ["node" "ethlance-emailer.js"]
                      :compiler {:main ethlance-emailer.cmd
                                 :output-to "ethlance-emailer.js",
                                 :output-dir "release",
                                 :target :nodejs,
                                 :optimizations :advanced,
                                 :verbose false
                                 :source-map "ethlance-emailer.js.map"
                                 :externs ["src/ethlance_emailer/externs.js"]
                                 }
                      :source-paths ["src"]}}}
  :clean-targets ["out" "release" "ethlance-emailer.js"]
  :target-path "target")
