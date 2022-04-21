//import graph.Graph;
//import utils.Matrix;
//import utils.Pair;
//
//import java.util.*;
//
//public record Solution(
//        float L1NORM,
//        Matrix matrix,
//        Graph graph,
//        double[] a,
//        double[] f,
//        double[] g,
//        double[] alpha,
//        double[] beta,
//        double[] r,
//        //double[] d,
//        double[] q,
//        double[] x
//) {
//    private final static double EPS = 0.0001;
//
//    public boolean adapt() {
//        double[] p = matrix.mult(new Matrix(a).transpose()).transpose().getRow(0);
//
//        double l1norm = 0;
//        for (double val : p) {
//            l1norm += Math.abs(val);
//        }
//
////            if (PRINT_DEBUG) {
////                System.out.println("! l1norm: " + l1norm);
////            }
//
//        if (Math.abs(l1norm) < EPS) {
//            return false;
//        }
//
//        double coeff = L1NORM / l1norm;
//
//        for (int i = 0; i < a.length; i++) {
//            a[i] *= coeff;
//        }
//
//        double[] new_p = matrix.mult(new Matrix(a).transpose()).transpose().getRow(0);
//
//        for (int i = 0; i < new_p.length; i++) {
//            if (new_p[i] < 0) {
//                f[i] = 0;
//                alpha[i] = 0;
//                g[i] = Math.abs(new_p[i]);
//                beta[i] = 1;
//            } else {
//                f[i] = Math.abs(new_p[i]);
//                alpha[i] = 1;
//                g[i] = 0;
//                beta[i] = 0;
//            }
//        }
//
//        check();
//
//        double max_f = -1;
//        int argmax_f = -1;
//        for (int i = 0; i < q.length; i++) {
//            if (q[i] >= max_f) {
//                max_f = q[i];
//                argmax_f = i;
//            }
//        }
//
//        if (argmax_f == -1 || max_f == -1) {
//            throw new RuntimeException("not found max in q[] array");
//        }
//
//        int root_ind = argmax_f;
//        for (int i = 0; i < graph.getNodesCount(); i++) r[i] = 0;
//        r[root_ind] = 1;
//
//        MainSolver.out_debug.println(q[root_ind]);
//
//        if (q[root_ind] < MainSolver.SMALL) {
//            return false;
//        }
//
//        boolean[] visited = new boolean[graph.getNodesCount()];
//        boolean[] visitedEdge = new boolean[graph.getEdges().size()];
//
//        dfs(root_ind, visited, visitedEdge, false, null);
//
//        boolean[] visitedNew = new boolean[graph.getNodesCount()];
//
//        dfs(root_ind, visitedNew, visitedEdge, true, visited);
//
//        for (int i = 0; i < visited.length; i++)
//            if (!visited[i] && !visitedNew[i]) {
//                //q[i] = 0;
//                throw new RuntimeException("unexpected dfs 1");
//            }
//        for (int i = 0; i < visitedEdge.length; i++)
//            if (!visitedEdge[i]) {
//                x[i] = 0;
//                //throw new RuntimeException("unexpected dfs 2");
//            }
//
//        return true;
//    }
//
//    private void dfs(int u, boolean[] vis, boolean[] visEdge, boolean second, boolean[] oldVis) {
//        vis[u] = true;
//
//        List<Pair<Integer, Long>> neighbours = new ArrayList<>(graph.edgesOf(u));
//        neighbours.sort(Comparator.comparingDouble(x -> q[x.first]));
//        Collections.reverse(neighbours);
//
//        // check:
//        for (int i = 0; i < neighbours.size() - 1; i++) {
//            if (q[neighbours.get(i).first] < q[neighbours.get(i + 1).first]) {
//                throw new RuntimeException("unsorted neighbours");
//            }
//        }
//
//        for (Pair<Integer, Long> edge : neighbours) {
//            System.out.println(neighbours);
//
//            int to = edge.first;
//            int num = edge.second.intValue();
//            int back_num = Graph.companionEdge(num);
//
//            // check:
//            Pair<Integer, Integer> backE = graph.getEdges().get(back_num);
//            if (backE.second != u) {
//                throw new RuntimeException("unexpected back edge");
//            }
//
//            if (!second) {
//
//                if (!vis[to] && (q[u] - q[to] >= MainSolver.SMALL)) {
//                    x[num] = 1;
//                    visEdge[num] = true;
//                    dfs(to, vis, visEdge, false, null);
//                } else {
//                    x[num] = 0;
//                    visEdge[num] = true;
//                }
//
//            } else {
//
//                if (!vis[to]) {
//                    if (q[u] - q[to] < MainSolver.SMALL) {
//                        q[to] = Math.max(q[u] - MainSolver.SMALL, 0);
//                        x[num] = 1;
//                        visEdge[num] = true;
//                    }
//                    dfs(to, vis, visEdge, true, oldVis);
//                }
//
//            }
//
//        }
//    }
//
//    private void check() {
//        double[] new_p = matrix.mult(new Matrix(a).transpose()).transpose().getRow(0);
//        if (new_p.length != f.length) {
//            throw new RuntimeException("solution check failed: 1");
//        }
//        double l1norm = 0;
//        for (int i = 0; i < new_p.length; i++) {
//            if (Math.abs(new_p[i] - (f[i] - g[i])) > EPS) {
//                throw new RuntimeException("solution check failed: 2");
//            }
//            l1norm += Math.abs(new_p[i]);
//        }
//        if (Math.abs(l1norm - L1NORM) > EPS) {
//            throw new RuntimeException("solution check failed: 3");
//        }
//        for (int i = 0; i < alpha.length; i++) {
//            if (!((alpha[i] == 0 && beta[i] == 1) || (alpha[i] == 1 && beta[i] == 0))) {
//                throw new RuntimeException("solution check failed: 4");
//            }
//        }
//    }
//
//    public double[] getAllCopy() {
//        int cnt = 0;
//        int new_size = a.length + f.length + g.length + alpha.length + beta.length +
//                r.length + /*d.length*/ +q.length + x.length;
//        double[] res = new double[new_size];
//        for (double y : a) res[cnt++] = y;
//        for (double y : f) res[cnt++] = y;
//        for (double y : g) res[cnt++] = y;
//        for (double y : alpha) res[cnt++] = y;
//        for (double y : beta) res[cnt++] = y;
//        for (double y : r) res[cnt++] = y;
//        //for (double y : d) res[cnt++] = y;
//        for (double y : q) res[cnt++] = y;
//        for (double y : x) res[cnt++] = y;
//        return res;
//    }
//
//    @Override
//    public String toString() {
//        return "Solution{\n" +
//                "| a = " + Arrays.toString(a) + "\n" +
//                "| f = " + Arrays.toString(f) + "\n" +
//                "| g = " + Arrays.toString(g) + "\n" +
//                "| alpha = " + Arrays.toString(alpha) + "\n" +
//                "| beta = " + Arrays.toString(beta) + "\n" +
//                "| r = " + Arrays.toString(r) + "\n" +
//                //"| d = " + Arrays.toString(d) + "\n" +
//                "| q = " + Arrays.toString(q) + "\n" +
//                "| x = " + Arrays.toString(x) + "\n" +
//                "\n";
//    }
//}
