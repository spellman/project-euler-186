# Project Euler Problem 186

This program solves Project Euler problem 186: https://projecteuler.net/problem=186:



## Problem

### Problem Setup
Here are the records from a busy telephone system with one million users:

RecNr | Caller | Called
----- | ------ | ------
1 | 200007 | 100053
2 | 600183 | 500439
3 | 600863 | 701497
... | ... | ...

The telephone number of the caller and the called number in record n are Caller(n) = S<sub>2n-1</sub> and Called(n) = S<sub>2n</sub> where S<sub>1,2,3,...</sub> come from the "Lagged Fibonacci Generator":

For 1 ≤ k ≤ 55, S<sub>k</sub> = [100003 - 200003k + 300007k<sup>3</sup>] (modulo 1000000)   
For 56 ≤ k, S<sub>k</sub> = [S<sub>k-24</sub> + S<sub>k-55</sub>] (modulo 1000000)

If Caller(n) = Called(n) then the user is assumed to have misdialled and the call fails; otherwise the call is successful.

From the start of the records, we say that any pair of users X and Y are friends if X calls Y or vice-versa. Similarly, X is a friend of a friend of Z if X is a friend of Y and Y is a friend of Z; and so on for longer chains.

The Prime Minister's phone number is 524287.


### Problem Statement
After how many successful calls, not counting misdials, will 99% of the users (including the PM) be a friend, or a friend of a friend etc., of the Prime Minister?



## Program Usage

Use your preferred method of running Clojure programs from the command line :)

With no arguments (e.g., `$ lein run`) the program will use the graph-coloring algorithm to count friends (see below) and the values given in the problem for the Prime Minister and threshold percentage of users.

You may specify your own values with command-line flags:

Short | Long | Argument | Default | Description
----- | ---- | -------- | ------- | -----------
| | `--prime-minister` | PRIME-MINISTER  | 524287 | The number of the prime minister. int <- [0, 999,999]
| | `--percent-friends` | PERCENT-FRIENDS | 99 | Threshold percentage of friends at which to stop and return the result. num <- [0, 100]"
`-a` | `--algorithm` | ALGORITHM | graph-coloring | The friend-counting algorithm to use. See available algorithms below.
`-b` | `--benchmark` | (none) | Runs a benchmark with Criterium quick-bench.
`-h` | `--help` | (none) | (none) | Prints a description of the program and its usage.

Note that you may need to preface your options with `--` to differentiate them from arguments to lein/boot/java/other. E.g., since lein has its own `-h` option, we bring up the help text with `$ lein run -- -h`.



## Algorithms

### Graph Coloring
I initially visualized the problem via graph coloring. I represented the graph as an adjacency list:
*   An adjacency matrix (10^12) would not fit in memory on many machines.
*   I expected the matrix to be sparse, which suggests using a list instead. We stop with 2,325,629 ~ 10^6 edges so the matrix would have been very sparse indeed.

The initial graph has a vertex for each user and no edges. We proceed as follows:
1.  Generate a successful call from user i to user j.
2.  Add an undirected edge between verticies i and j. The edge is undirected because the call makes the vertices friends of each other.
    *   Note that when the Prime Minister calls user i, the PM becomes their own friend because the `friends-with` relation is given to be transitive: PM `friends-with` i `friends-with` PM => PM `friends-with` PM.
3.  Color the subgraph connected to i (which is the subgraph connected to j, since i and j are connected).
    *   This subgraph may contain cycles but we can stop and backtrack when we reach a colored vertex. The uncolored vertices of the subgraph therefore form a tree.
    *   I got stack overflow exceptions when traversing these trees with `clojure.core/tree-seq` so I used a stack to enumerate the tree depth-first and then colored the vertices in that stack. I ported the graph-walking code from http://kmkeen.com/python-trees/.
4.  If the number of colored vertices is at least the threshold number then we are finished; the number of successful calls is our answer. Otherwise, repeat this procedure.


### Disjoint Sets

After submitting my answer to Project Euler, I browsed the comment thread to find that the goal of the problem was to encourage people to learn/use the [disjoint-set data structure](https://en.wikipedia.org/wiki/Disjoint-set_data_structure). In fact, the comments showed this approach to have been used nearly universally. The reported run-times were less than I was seeing with my approach; some were two orders of magnitude less.

I wrote versions of a disjoint-set:
*   with Clojure's persistent data structures, with and without path compression
*   with a vector of atoms, without and without path compression
*   in Java, with an array of structs (Node instances), with path compression
In the Clojure cases with path compression, I tried both 1) tracking the nodes visited on the path to the root and updating all of their parents to the root, and 2) updating the parent of only the node for whose root parent we were looking. In the Java version, I only tried the latter.

Additionally, I found three existing Clojure implementations (though using them seems like it would defeat part of this exercise):
*   https://github.com/jordanlewis/data.union-find, which uses mutability
*   https://gist.github.com/retnuh/4749721, which uses a state monad
*   http://www.learningclojure.com/2013/09/implementing-data-structure-union-find_19.html, which seems the most data-centric implementation


### Comparison

I benchmarked the various algorithms with Criterium (with `criterium.core/quick-bench`) on a laptop with a 2.60 GHz quad-core i7 and 16GB RAM:

Algorithm / Flag | Time
---------------- | ----
`graph-coloring` | 17.098633 sec
`disjoint-set` w/persistent data structures, no path compress | 9.924961 sec
`disjoint-set+path-compress` w/persistent data structures, base -> root path compress | 10.949421 sec
disjoint-set w/persistent data structures, path -> root path compress | 11.793040 sec
`disjoint-set-atoms` w/vector of atoms, no path compress | 8.717043 sec
`disjoint-set-atoms+path-compress` w/vector of atoms, base -> root path compress | 8.973145 sec
disjoint-set w/vector of atoms, path -> root path compress | 12.846392 sec
`disjoint-set-java+path-compress` w/array of nodes, base -> root path compress | 5.294673 sec



## Notes

### Loop-recur

Collection/stream-processing functions (map, filter, reduce, etc.) are usually the work-horses of my Clojure code but this problem was amenable to recursion.

### Code organization

I'm comfortable with the namespaces for a project of this size. The algorithms namespace could be broken up as the graph coloring has nothing to do with the disjoint-set implementations and the latter wouldn't need suffixes to differentiate their function names.


### Algorithm speccability

The graph-coloring algorithm was easy to spec and all of the specs/functions can be exercised and/or checked.

I would need to become more proficient with spec to do the disjoint-set code, though, because the size of the val and parent indices must be constrained by the number of sets generated or else we'll get out of bounds. I'll have to think more about how to do that. We could only generate sets of size number-of-users = 1,000,000 but that makes checking very slow. We could specify a small number of users (10 or 100 or something) and always generate sets of that size if we don't plan to instrument in non-spec testing or in production. Without viable generators the return-value specs (required by fdef) are no better than comments. Ultimately, I decided to remove all of the specs from the disjoint-set code.


### Java Version Of Disjoint Set

I wanted to see how much faster a Java version would run. Not much: twice as fast as updating persistent data structures in Clojure. 
