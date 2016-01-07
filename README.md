# intension

[](dependency)
```clojure
[alandipert/intension "1.0.0"] ;; latest release
```
[](/dependency)

Clojure makes it easy and efficient to create, update, and access places within
immutable, nested associative structures like maps-of-maps or vectors-of-maps.

However, Clojure doesn't help much with **querying** these structures. As a
result, programmers are often compelled to implement bespoke query engines and
DSLs on a per-application basis.

This library contains a set of functions for converting associative structures
to in-memory databases suitable for query via [Datalog][0]
implementations like those found in [Datomic][1] and [DataScript][2].

## Usage

```clojure
(require '[alandipert.intension :refer [make-db]]
         '[datascript.core      :refer [q]])

(def pets
  [{:name "George"
    :species "Parakeet"
    :age 3
    :owners ["Frege" "Peirce"]}
   {:name "Francis"
    :species "Dog"
    :age 8
    :owners ["De Morgan"]}
   {:name "Bob"
    :species "Goldfish"
    :age 1
    :owners ["Peirce"]}])
    
;; Create a set of relations based on paths into the pets map. Every relation
;; starts with the path the relation is based on, and ends with the value at
;; that path. The elements of the path are in between. Including the path prefix
;; is useful if you plan on updating the structure based on the query result.

(def pets-db (make-db pets {:prefix-paths? true}))

;; Find the names of all the pets:

(q '[:find ?name
     :where
     [_ _ :name ?name]]
   pets-db)
;;=> #{["George"] ["Francis"] ["Bob"]}

;; Find each owner, and how many pets each owns:

(q '[:find ?owner (count ?pet)
     :where
     [_ ?pet :owners _ ?owner]]
   pets-db))
;;=> (["Peirce" 2] ["Frege" 1] ["De Morgan" 1])

;; Find the paths to every pet's age over 2:

(def age-paths
  (->> (q '[:find ?path
            :where
            [?path _ :age ?age]
            [(> ?age 5)]]
          pets-db)
       (map first))
     
;; Return a new map with every pet's age over 2 inc'd:

(reduce #(update-in %1 %2 inc) pets age-paths)
;;=> a map in which George and Francis are now 4 and 9, respectively.
```

## Theory of operation

### Maps as sets of pairs

Consider the following map:

```clojure
(def m1
  {:color "red"
   :year 1992
   :sound "ringing"})
```

The same information could be represented as a set of vectors, or ordered pairs:

```clojure
(def m2
  #{[:color "red"]
    [:year 1992]
    [:sound "ringing"]})
```

A set of ordered pairs is equivalent to a map in that the `[key, value]`
associations are distinct within both structures. The map is superior for most
programming purposes in that given a key, the corresponding value can be found
efficiently.

However, the set-of-tuples structure has its own special affordance: it can be
viewed as set of 2-place [relations][3].

The advantage of a relational view of the structure is that it can be
queried directly with a popular dialect of Datalog.

For example, the following Datomic-flavored Datalog query finds the values for
`?v` in all of the relations starting with `:color`:

```clojure
[:find ?v
 :where [:color ?v]]
```

### Cardinality of associations

In a map, there can only be one association between a key and a value, so the
above query will only ever return one value for `?v`.  In other applications of
Datalog, such as against a Datomic database, a similar query might yield multiple
values for `?v` if the *cardinality* of the `:color` relation is "many".

### Deep structures

Any k-deep associative structure can be represented as a set of relations
varying in arity between 2 and k.  A path into a nested structure corresponds to
a relation in a set.

### Associativity of sets

It's not especially important for the purposes of this library that sets be
queryable. Most of the time I need to query data from a JSON[5] source, and JSON
does not support sets. However, I learned while developing the
library that sets like `#{:a :b :c}` can be viewed as sets of 1-place relations
like these:

```clojure
#{[:a] [:b] [:c]}
```

Under this scheme, the map `{:xs #{1 2 3}}` is represented as this set of
2-place relations:

```clojure
#{[:xs 1] 
  [:xs 2]
  [:xs 3]}
```

A variant of `assoc` can then be defined that can operate on sets:

```clojure
(defn assoc* 
  [coll k v]
  "Like assoc but works also on sets."
  (if (set? coll)
    (conj (disj coll k) v)
    (assoc coll k v)))
```

A variant of `update-in` can be defined that works on structures
possibly containing sets:

```clojure
(defn update-in*
  "Like update-in but works also on sets."
  ([m [k & ks] f & args]
   (if ks
     (assoc* m k (apply update-in* (get m k) ks f args))
     (assoc* m k (apply f (get m k) args)))))
```

Combining our scheme for representing paths into sets and `update-in*`, we can
now (arguably meaningfully?) "update" values into structures containing sets:

```clojure
(update-in* {:xs #{1 2 3}} [:xs 3] inc) ;=> {:xs #{1 2 4}}
```

> Note: Clojure already supports `get` on sets: `(get #{1 2 3} 2)` is `2`.

Relations representing paths to sets can be thought of as having "many"
cardinality.

## Differences from spectre

[spectre][6] is another library that was also created with the goal making it
easier to work with nested structures in Clojure.

Unlike spectre, intension supplies no means for updating structures — only
querying them using a (separate) Datalog implementation.  Also, it's only
possible to query maps and vectors currently.

## Ideas for improvement

It would be useful to support "wildcards."  Sometimes you don't know how deep
the path is to some key/value in the structure you're interested in - you just
know you're interested in the shallowest value for `:name` or whatever.

It might be good to create another function, like `make-meta-db`, that returned
a database of convenience relations generated at some extra expense. The meta db
might contain 3-place relations for every distinct key/value regardless of
depth, prefixed by path. Meta data could be joined against data generated by
`make-db` on the path value.

[0]: https://en.wikipedia.org/wiki/Datalog
[1]: http://docs.datomic.com/query.html
[2]: https://github.com/tonsky/datascript
[3]: https://en.wikipedia.org/wiki/Relation_(database)
[4]: https://en.wikipedia.org/wiki/Unification_(computer_science)
[5]: https://en.wikipedia.org/wiki/JSON
[6]: https://github.com/nathanmarz/specter

## License

```
Copyright (c) Alan Dipert. All rights reserved.

The use and distribution terms for this software are covered by the Eclipse
Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
be found in the file epl-v10.html at the root of this distribution. By using
this software in any fashion, you are agreeing to be bound by the terms of
this license. You must not remove this notice, or any other, from this software.
```
