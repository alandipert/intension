(set-env!
 :dependencies '[[org.clojure/clojure "1.7.0"  :scope "provided"]
                 [adzerk/bootlaces    "0.1.13" :scope "test"]
                 [datascript          "0.13.3" :scope "test"]]
 :resource-paths #{"src"})

(require '[adzerk.bootlaces :refer :all])

(def +version+ "1.1.0")

(bootlaces! +version+)

(task-options!
  pom  {:project     'alandipert/intension
        :version     +version+
        :description "Query maps/vectors with Datalog"
        :url         "https://github.com/alandipert/intension"
        :scm         {:url "https://github.com/alandipert/intension"}
        :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})
