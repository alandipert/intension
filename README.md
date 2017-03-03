# intension

[](dependency)
```clojure
[alandipert/intension "1.1.0"] ;; latest release
```
[](/dependency)

Clojure makes it easy and efficient to create, update, and access places within
immutable, nested associative structures like maps-of-maps or vectors-of-maps.

However, I haven't found in Clojure satisfying means of *querying* these
structures.

This library contains a set of functions for converting associative structures
to in-memory databases suitable for query via Datomic-flavored [Datalog][0]
implementations like [Datomic's own][1] or [DataScript][2].

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
    
;; Create a set of relations based on paths into the pets map. Every relation is
;; a path followed by the value at that path.

(def pets-db (make-db pets))

;; Find the names of all the pets:

(q '[:find ?name
     :where
     [_ :name ?name]]
   pets-db)
;;=> #{["George"] ["Francis"] ["Bob"]}

;; To find each owner and how many pets each owner owns, we might write Clojure code like this:

(->> (for [p pets, o (:owners p)] {o 1})
     (apply merge-with +))
;;=> {"Peirce" 2, "Frege" 1, "De Morgan" 1}

;; It's pretty short for this example, but I find this kind of code gets hard to
;; follow, particularly with deeper structures. I find queries involving joins
;; and filters especially difficult to express adequately in this way. Here's
;; how I would prefer to do it, with Datalog:

(->> (q '[:find ?owner (count ?pet)
          :where
          [?pet :owners _ ?owner]]
       pets-db)
     (into {}))
;;=> {"Peirce" 2, "Frege" 1, "De Morgan" 1}

;; Create another set of relations, this time prefixing each with the path
;; itself. This is useful for updating structures with update-in based on query
;; results.

(def pets-db2 (make-db pets {:paths? true}))

;; Find the paths to every pet's age over 2:

(def age-paths
  (->> (q '[:find ?path
            :where
            [?path _ :age ?age]
            [(> ?age 2)]]
          pets-db2)
       (map first)))
;;=> ([0 :age] [1 :age])
     
(reduce #(update-in %1 %2 inc) pets age-paths)
;;=> A vector of maps in which George and Francis are now 4 and 9, respectively.
```

## How it works

Consider this map:

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
queried directly easily with a popular dialect of Datalog.

For example, the following Datomic-flavored Datalog query finds the values for
`?v` in all of the relations starting with `:color`:

```clojure
[:find ?v :where [:color ?v]]
```

## Differences from specter

[specter][4] is another library that was also created with the goal making it
easier to work with nested structures in Clojure.

Unlike specter, intension supplies no means for updating structures — only
querying them using a (separate) Datalog implementation.  Also, it's only
possible to query maps and vectors currently.

[0]: https://en.wikipedia.org/wiki/Datalog
[1]: http://docs.datomic.com/query.html
[2]: https://github.com/tonsky/datascript
[3]: https://en.wikipedia.org/wiki/Relation_(database)
[4]: https://github.com/nathanmarz/specter

## License

```
Copyright (c) Alan Dipert. All rights reserved.

The use and distribution terms for this software are covered by the Eclipse
Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
be found in the file epl-v10.html at the root of this distribution. By using
this software in any fashion, you are agreeing to be bound by the terms of
this license. You must not remove this notice, or any other, from this software.
```
