(ns alandipert.intension
  "Functions for converting nested structures into sets of tuples.")

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
   query by Datalog.  Attaches the original structure to the vector returned under the :alandipert.intension/original key.

   Takes an optional configuration map that can contain these options:

     :paths? - false by default.  Whether or not to prefix every tuple with the path.
               Useful for processing structures with update-in based on query results."
  [coll & [{:keys [paths?]
            :or   {paths? false}}]]
  (with-meta
    (mapv (fn [path]
            (conj
             (if paths? (vec (list* path path)) path)
             (get-in coll path)))
          (paths coll))
    {::original coll}))
