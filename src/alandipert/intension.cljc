(ns alandipert.intension)

(defn paths
  "Returns the set of paths into a nested map/vector."
  ([root]
   {:pre [(or (map? root)
              (vector? root))]}
   (paths [] root))
  ([parent x]
   (cond (map? x)
         (mapcat (fn [[k v]] (paths (conj parent k) v)) x)
         (vector? x)
         (mapcat #(paths (conj parent %1) %2) (range) x)
         :else [parent])))


(defn make-db
  "Converts a nested structure of vectors/maps into a set of tuples suitable for
   query by Datalog.  Takes an optional configuration map that can contain these options:

     :prefix-paths? - true by default.  Whether or not to prefix every tuple with the path.
                      Useful for processing structures with update-in based on query results."
  [coll & [{:keys [prefix-paths?]
            :or   {prefix-paths? true}}]]
  (mapv (fn [path]
          (conj
           (if prefix-paths? (vec (list* path path)) path)
           (get-in coll path)))
        (paths coll)))
