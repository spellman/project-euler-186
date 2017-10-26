public final class DisjointSet {
    private Node[] nodes;

    public DisjointSet(int n) {
        nodes = new Node[n];

        for (int i = 0; i < n; i++) {
            nodes[i] = new Node(i);
        }
    }

    private Node findRootParent(int idx) {
        Node base = nodes[idx];
        int i = idx;

        while (true) {
            Node node = nodes[i];
            int p = node.parent;

            if (i == p) {
                base.parent = p;
                return node;
            }

            i = p;
        }
    }

    private void replaceParent(Node parent, Node node) {
        node.parent = parent.parent;
        parent.connectedness += node.connectedness;
    }

    public void union(int idx1, int idx2) {
        Node root1 = findRootParent(idx1);
        Node root2 = findRootParent(idx2);

        if (root1 == root2) {
            return;
        }

        int rank1 = root1.rank;
        int rank2 = root2.rank;

        if (rank1 < rank2) {
            replaceParent(root2, root1);
        }
        else if (rank1 > rank2) {
            replaceParent(root1, root2);
        }
        else {
            replaceParent(root1, root2);
            root1.rank += 1;
        }
    }

    public int connectedness(int idx) {
        return findRootParent(idx).connectedness;
    }

    private final class Node {
        int parent;
        int rank;
        int connectedness;

        public Node(int i) {
            parent = i;
            rank = 0;
            connectedness = 1;
        }
    }
}
