(ns project-euler-186.algorithms
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.gen.alpha :as gen]
            [clojure.set]
            [clojure.algo.generic.math-functions :as math]))

;; LAGGED FIBONACCI GENERATOR
;; See https://en.wikipedia.org/wiki/Lagged_Fibonacci_generator.

(s/def ::user (s/int-in 0 1000000))
(s/def ::call (s/coll-of ::user :count 2))

;; (*) 2017-10-25 Cort Spellman
;; There is a bug in s/int-in-range?: https://dev.clojure.org/jira/browse/CLJ-2167
;; The function int? is being checked for truthiness; not the result of applying
;; int? to the arg:
;; https://github.com/clojure/clojure/blob/d920ada9fab7e9b8342d28d8295a600a814c1d8a/src/clj/clojure/spec.clj#L1618
;; TODO: Remove the extra int? check once int-in-range? is fixed.

;; I took the mod of the linear and cubic terms before taking the mod of the
;; sum. It doesn't seem to affect the run-time of the algorithm much either way.
(def lagged-fib-generator
  (memoize
   (fn [n]
     (if (and (int? n) ; See (*) above.
              (s/int-in-range? 1 56 n))
       (int (mod (+ 100003N
                    (mod (* -200003N n) 1000000N)
                    (mod (* 300007N (bigint (math/pow n 3))) 1000000N))
                 1000000N))
       (int (mod (+ (lagged-fib-generator (- n 24))
                    (lagged-fib-generator (- n 55)))
                 1000000N))))))

(s/fdef lagged-fib-generator
        :args (s/cat :n pos-int?)
        :ret ::user)

(defn make-call [n]
  [(lagged-fib-generator (dec (* 2 n)))
   (lagged-fib-generator (* 2 n))])

(s/fdef make-call
        :args (s/cat :call-idx pos-int?)
        :ret ::call)

(defn valid-call? [call]
  (apply not= call))

(s/fdef valid-call?
        :args (s/cat :call ::call)
        :ret boolean?)







;; GRAPH-COLORING ALGORITHM
;;
;; Let friends in the problem statement be called immediate friends.
;; Let generalized friends be called friends.
;;
;; Outline of algorithm:
;; ---------------------
;; 1. Generate next call = [caller callee]
;; 2. If call is invalid, then repeat from 1. Else, continue.
;; 3. Add edge to graph (adjacency list).
;; 4. Add new friends to set. We can visualize this as coloring these vertices.
;; 5. Count friends = colored vertices.
;; 6. Stop if |friends| >= p * # users. Else, repeat from 1.
;;
;; NOTE: The Prime Minister cannot call himself but he becomes his own friend
;; by transitivity once he calls someone and thus becomes their friend.

;; We can use an adjacency list to represent the graph whose vertices are users
;; and whose edges are calls between users.
;; And adjacency matrix would not fit in memory on many machines and the matrix
;; will likely be sparse anyway. Therefore, an adjacency list makes sense.
(s/def ::user->immed-friends
  (s/every-kv ::user (s/every ::user :kind set?) :kind map?))

;; We can visualize the set of friends of the Prime Minister as colored
;; vertices.
(s/def ::friends (s/every ::user :kind set?))



(defn add-immed-friends
  "Adds the edge represented by call to the graph represented by the adjacency
  list user->immed-friends. Returns the updated graph."
  [user->immed-friends call]
  (let [[user1 user2] call]
    (-> user->immed-friends
        (update user1 (comp set conj) user2)
        (update user2 (comp set conj) user1))))

(s/fdef add-immed-friends
        :args (s/cat :user->immed-friends ::user->immed-friends
                     :call ::call)
        :ret ::user->immed-friends
        :fn (s/and
             #(<= (-> % :args :user->immed-friends count)
                  (-> % :ret count))
             #(let [[u1 u2] (-> % :args :call)]
                (contains? (get-in % [:ret u1]) u2))
             #(let [[u1 u2] (-> % :args :call)]
                (contains? (get-in % [:ret u2]) u1))))

(defn can-make-friends? [prime-minister friends user]
  (or (= prime-minister user)
      (contains? friends user)))

(s/fdef can-make-friends?
        :args (s/cat :prime-minister ::user :friends ::friends :user ::user)
        :ret boolean?)

(defn can-be-friended? [friends user]
  (not (contains? friends user)))

(s/fdef can-be-friended?
        :args (s/cat :friends ::friends :user ::user)
        :ret boolean?)

(defn made-new-friend?
  [prime-minister friends candidate-friend-maker candidate-new-friend]
  (and (can-make-friends? prime-minister friends candidate-friend-maker)
       (can-be-friended? friends candidate-new-friend)))

(s/fdef made-new-friend?
        :args (s/cat :prime-minister ::user
                     :friends ::friends
                     :candidate-friend-maker ::user
                     :candidate-new-friend ::user)
        :ret boolean?)

(defn subgraph->seq
  "Takes a vertex, an adjacency list (map of vertex to set of adjacent
  vertices), and a set of bounding vertices. Walks the subgraph connected to
  vertex depth-first, backtracking when it reaches a cycle or one of the
  bounding vertices.
  Returns a seq of the walked vertices in the subgraph (a subtree, given the
  bounding).
  The depth-first search is achieved by using a stack to hold the vertices to
  be walked, where vertices connected to the current vertex are pushed onto the
  to-be-walk stack."
  [vertex adj-list bounding-vertices]
  (loop [to-walk-stack [vertex]
         visited #{}
         result []]
    (if-not (seq to-walk-stack)
      result
      (let [current (peek to-walk-stack)
            popped (pop to-walk-stack)]
        (if (or (contains? bounding-vertices current)
                (contains? visited current))
          (recur popped visited result)
          (let [visited' (conj visited current)]
            (recur
             (into popped (clojure.set/difference (adj-list current) visited'))
             visited'
             (conj result current))))))))

(defn add-friends
  "Color the friends of new-friend by adding them to the set of friends of the
  Prime Minister."
  [friends user->immed-friends new-friend]
  (into friends
        (subgraph->seq new-friend user->immed-friends friends)))

(s/fdef add-friends
        :args (s/cat :friends ::friends
                     :user->immed-friends ::user->immed-friends
                     :new-friend ::user)
        :ret ::friends
        :fn (s/and #(<= (-> % :args :friends count)
                        (-> % :ret count))
                   #(contains? (:ret %) (-> % :args :new-friend))))

(defn update-graph
  "We add an edge to the graph to represent the call. If the call involved a
  friend of the Prime Minister and a non-friend (i.e., colored and uncolored
  vertices), then the latter and all of their friends become friends of the
  Prime Minister (i.e., we color the subgraph connected to the uncolored
  vertex)."
  [prime-minister friends user->immed-friends call]
  (let [[user1 user2] call
        user->immed-friends' (add-immed-friends user->immed-friends call)]
    (cond
      (made-new-friend? prime-minister friends user1 user2)
      (let [friends' (add-friends friends user->immed-friends' user2)]
        [friends' user->immed-friends'])

      (made-new-friend? prime-minister friends user2 user1)
      (let [friends' (add-friends friends user->immed-friends' user1)]
        [friends' user->immed-friends'])

      :else [friends user->immed-friends'])))

(s/fdef update-graph
        :args (s/cat :prime-minister ::user
                     :friends ::friends
                     :user->immed-friends ::user->immed-friends
                     :call ::call)
        :ret (s/tuple ::friends ::user->immed-friends)
        :fn (s/and #(<= (-> % :args :friends count)
                        (-> % :ret (get 0) count))
                   #(<= (-> % :args :user->immed-friends count)
                        (-> % :ret (get 1) count))))

(defn graph-coloring [num-users prime-minister percent-friends]
  (let [num-friends-stop-threshold (* (/ percent-friends 100)
                                      num-users)]
    (loop [call-idx (long 1)
           num-success-calls (long 0)
           friends #{}
           user->immed-friends {}]
      (if (<= num-friends-stop-threshold (count friends))
        num-success-calls
        (let [call (make-call call-idx)]
         (if-not (valid-call? call)
           (recur (inc call-idx) num-success-calls friends user->immed-friends)
           (let [[friends' user->immed-friends']
                 (update-graph prime-minister friends user->immed-friends call)]
             (recur (inc call-idx) (inc num-success-calls) friends' user->immed-friends'))))))))

(s/fdef graph-coloring
        :args (s/cat :num-users nat-int?
                     :prime-minister ::user
                     :percent-friends (s/int-in 1 101))
        :ret pos-int?)







;; DISJOINT-SET WITH PERSISTENT DATA STRUCTURES AND WITHOUT PATH-COMPRESSION
;;
;; See https://en.wikipedia.org/wiki/Disjoint-set_data_structure.
;; Each vertex begins as in a set by itself, as its own parent.
;; When an edge is added to the graph, its vertices are merged by updating the
;; parent of the vertex of lesser rank to be the vertex of greater rank.
;; Rather than count the vertices in a partition of the graph after every call
;; (i.e., after adding each edge), each vertex maintains a count of the sets to
;; which it is connected. The root parent of each partition will then have a
;; count of its partition.
;; After each call, then, we will compare the count of the root of the partition
;; containing the Prime Minister to the threshold number of friends of the Prime
;; Minister.

(defn make-set [i]
  {:val i
   :parent i
   :rank 0
   :connectedness 1})

(defn make-sets [n]
  (mapv make-set (range n)))

(defn find-root-parent [sets idx]
  (loop [i idx]
    (let [node (sets i)]
      (if (= (:val node) (:parent node))
        node
        (recur (:parent node))))))

(defn assoc-parent-to-node [sets parent node]
  (-> sets
      (assoc-in [(:val node) :parent] (:val parent))
      (update-in [(:val parent) :connectedness] + (:connectedness node))))

(defn union [sets i1 i2]
  (let [root1 (find-root-parent sets i1)
        root2 (find-root-parent sets i2)
        rank1 (:rank root1)
        rank2 (:rank root2)]
    (cond
      (= (:val root1) (:val root2)) sets
      (< rank1 rank2) (assoc-parent-to-node sets root2 root1)
      (> rank1 rank2) (assoc-parent-to-node sets root1 root2)
      :else (-> sets
                (assoc-parent-to-node root1 root2)
                (update-in [(:val root1) :rank] inc)))))

(defn disj-set [num-users prime-minister percent-friends]
  (let [num-friends-stop-threshold (* (/ percent-friends 100)
                                      num-users)]
    (loop [call-idx (long 1)
           num-success-calls (long 0)
           sets (make-sets num-users)]
      (if (<= num-friends-stop-threshold
              (->> prime-minister (find-root-parent sets) :connectedness))
        num-success-calls
        (let [[u1 u2 :as call] (make-call call-idx)]
          (if-not (valid-call? call)
            (recur (inc call-idx) num-success-calls sets)
            (recur (inc call-idx) (inc num-success-calls) (union sets u1 u2))))))))







;; DISJOINT-SET WITH PERSISTENT DATA STRUCTURES AND WITH PATH-COMPRESSION

(defn make-set-p [i]
  {:val i
   :parent i
   :rank 0
   :connectedness 1})

(defn make-sets-p [n]
  (mapv make-set-p (range n)))

(defn find-root-parent-p [sets idx]
  (let [parent (loop [i idx]
                 (let [node (sets i)]
                   (if (= (:val node) (:parent node))
                     node
                     (recur (:parent node)))))]
    [(assoc-in sets [idx :parent] (:val parent))
     parent]))

(defn assoc-parent-to-node-p [sets parent node]
  (-> sets
      (assoc-in [(:val node) :parent] (:val parent))
      (update-in [(:val parent) :connectedness] + (:connectedness node))))

(defn union-p [sets idx1 idx2]
  (let [[sets' root1] (find-root-parent-p sets idx1)
        [sets'' root2] (find-root-parent-p sets' idx2)
        rank1 (:rank root1)
        rank2 (:rank root2)]
    (cond
      (= (:val root1) (:val root2)) sets''
      (< rank1 rank2) (assoc-parent-to-node-p sets'' root2 root1)
      (> rank1 rank2) (assoc-parent-to-node-p sets'' root1 root2)
      :else (-> sets''
                (assoc-parent-to-node-p root1 root2)
                (update-in [(:val root1) :rank] inc)))))

(defn disj-set-p [num-users prime-minister percent-friends]
  (let [num-friends-stop-threshold (* (/ percent-friends 100)
                                      num-users)]
    (loop [call-idx (long 1)
           num-success-calls (long 0)
           sets (make-sets num-users)]
      (let [[sets' pm-parent] (find-root-parent-p sets prime-minister)]
        (if (<= num-friends-stop-threshold (:connectedness pm-parent))
          num-success-calls
          (let [[u1 u2 :as call] (make-call call-idx)]
            (if-not (valid-call? call)
              (recur (inc call-idx) num-success-calls sets')
              (recur (inc call-idx) (inc num-success-calls) (union sets' u1 u2)))))))))







;; DISJOINT-SET WITH VECTOR OF ATOMS WITHOUT PATH-COMPRESSION

(defn make-set-a [i]
  (atom {:parent i
         :rank 0
         :connectedness 1}))

(defn make-sets-a [n]
  (mapv make-set-a (range n)))

(defn find-root-parent-a [sets idx]
  (loop [i idx]
    (let [node (sets i)
          p (:parent @node)]
      (if (= i p)
        node
        (recur (-> p sets deref :parent))))))

(defn assoc-parent-to-node-a [parent node]
  (swap! node assoc :parent (:parent @parent))
  (swap! parent update :connectedness + (:connectedness @node)))

(defn union-a [sets i1 i2]
  (let [root1 (find-root-parent-a sets i1)
        root2 (find-root-parent-a sets i2)
        rank1 (:rank @root1)
        rank2 (:rank @root2)]
    (cond
      (= (:parent @root1) (:parent @root2)) true
      (< rank1 rank2) (assoc-parent-to-node-a root2 root1)
      (> rank1 rank2) (assoc-parent-to-node-a root1 root2)
      :else (do
              (assoc-parent-to-node-a root1 root2)
              (swap! root1 update :rank inc)))))

(defn disj-set-a [num-users prime-minister percent-friends]
  (let [num-friends-stop-threshold (* (/ percent-friends 100)
                                      num-users)
        sets (make-sets-a num-users)]
    (loop [call-idx (long 1)
           num-success-calls (long 0)]
      (if (<= num-friends-stop-threshold
              (->> prime-minister (find-root-parent-a sets) deref :connectedness))
        num-success-calls
        (let [[u1 u2 :as call] (make-call call-idx)]
          (if-not (valid-call? call)
            (recur (inc call-idx) num-success-calls)
            (do
              (union-a sets u1 u2)
              (recur (inc call-idx) (inc num-success-calls)))))))))







;; DISJOINT-SET WITH VECTOR OF ATOMS WITH PATH-COMPRESSION

(defn make-set-ap [i]
  (atom {:parent i
         :rank 0
         :connectedness 1}))

(defn make-sets-ap [n]
  (mapv make-set-ap (range n)))

(defn find-root-parent-ap [sets idx]
  (let [base (sets idx)
        parent (loop [i idx]
                 (let [node (sets i)
                       p (:parent @node)]
                   (if (= i p) node (recur p))))]
    (swap! base assoc :parent (:parent @parent))
    parent))

(defn assoc-parent-to-node-ap [parent node]
  (swap! node assoc :parent (:parent @parent))
  (swap! parent update :connectedness + (:connectedness @node)))

(defn union-ap [sets i1 i2]
  (let [root1 (find-root-parent-ap sets i1)
        root2 (find-root-parent-ap sets i2)
        rank1 (:rank @root1)
        rank2 (:rank @root2)]
    (cond
      (= (:parent @root1) (:parent @root2)) true
      (< rank1 rank2) (assoc-parent-to-node-ap root2 root1)
      (> rank1 rank2) (assoc-parent-to-node-ap root1 root2)
      :else (do
              (assoc-parent-to-node-ap root1 root2)
              (swap! root1 update :rank inc)))))

(defn disj-set-ap [num-users prime-minister percent-friends]
  (let [num-friends-stop-threshold (* (/ percent-friends 100)
                                      num-users)
        sets (make-sets-ap num-users)]
    (loop [call-idx (long 1)
           num-success-calls (long 0)]
      (if (<= num-friends-stop-threshold
              (->> prime-minister (find-root-parent-ap sets) deref :connectedness))
        num-success-calls
        (let [[u1 u2 :as call] (make-call call-idx)]
          (if-not (valid-call? call)
            (recur (inc call-idx) num-success-calls)
            (do
              (union-ap sets u1 u2)
              (recur (inc call-idx) (inc num-success-calls)))))))))
