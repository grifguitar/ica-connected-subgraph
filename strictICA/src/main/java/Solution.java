import graph.Graph;
import ilog.concert.IloNumExpr;
import utils.Matrix;
import utils.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record Solution(
        float L1NORM,
        Matrix matrix,
        Graph graph,
        double[] a,
        double[] f,
        double[] g,
        double[] alpha,
        double[] beta,
        double[] r,
        double[] d,
        double[] q,
        double[] x
) {
    private final static double EPS = 0.0001;
    private final static double HALF = 0.5;

    public double getObjValue() {
        double ans = 0;

        double[] squares = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            squares[i] = a[i] * a[i];
            ans += squares[i];
        }
        double[] q_prod = new double[f.length];
        double[] r_prod = new double[f.length];
        for (int i = 0; i < f.length; i++) {
            q_prod[i] = f[i] - g[i];
            r_prod[i] = q_prod[i] * r[i];
            q_prod[i] = q_prod[i] * q[i] * (1.0 / (double) f.length);
            ans += r_prod[i] + q_prod[i];
        }

        return ans;
    }

    public boolean adapt() {
        double[] p = matrix.mult(new Matrix(a).transpose()).transpose().getRow(0);

        double l1norm = 0;
        for (double val : p) {
            l1norm += Math.abs(val);
        }

//            if (PRINT_DEBUG) {
//                System.out.println("! l1norm: " + l1norm);
//            }

        if (Math.abs(l1norm) < EPS) {
            return false;
        }

        double coeff = L1NORM / l1norm;

        for (int i = 0; i < a.length; i++) {
            a[i] *= coeff;
        }

        double[] new_p = matrix.mult(new Matrix(a).transpose()).transpose().getRow(0);

        for (int i = 0; i < new_p.length; i++) {
            if (new_p[i] < 0) {
                f[i] = 0;
                alpha[i] = 0;
                g[i] = Math.abs(new_p[i]);
                beta[i] = 1;
            } else {
                f[i] = Math.abs(new_p[i]);
                alpha[i] = 1;
                g[i] = 0;
                beta[i] = 0;
            }
        }

        check();

        double max_f = -1;
        int argmax_f = -1;
        for (int i = 0; i < f.length; i++) {
            if (f[i] >= max_f) {
                max_f = f[i];
                argmax_f = i;
            }
        }

        if (argmax_f == -1 || max_f == -1) {
            throw new RuntimeException("not found max in f[] array");
        }

        int root_ind = argmax_f;
        for (int i = 0; i < graph.getNodesCount(); i++) r[i] = 0;
        r[root_ind] = 1;

        List<Boolean> visited = new ArrayList<>();
        List<Boolean> markedEdge = new ArrayList<>();
        for (int i = 0; i < graph.getNodesCount(); i++) visited.add(false);
        for (int i = 0; i < graph.getEdges().size(); i++) markedEdge.add(false);

        dfs(root_ind, visited, null, markedEdge, 1);

        int flag = 0;
        for (int i = 0; i < graph.getNodesCount(); i++) if (visited.get(i)) flag++;
        if (flag == 0) throw new RuntimeException("unexpected flag visited vertex");

        flag = 0;
        for (int i = 0; i < graph.getEdges().size(); i++) if (markedEdge.get(i)) flag++;
        if (flag == 0) throw new RuntimeException("unexpected flag marked edges");

        for (int i = 0; i < graph.getNodesCount(); i++) {
            if (!visited.get(i)) {
                r[i] = 0;
                d[i] = 1;
                q[i] = 0;
            }
        }
        for (int i = 0; i < graph.getEdges().size(); i++) {
            if (!markedEdge.get(i)) {
                x[i] = 0;
            }
        }

        return true;
    }

    private void dfs(int u, List<Boolean> vis, Integer parent_edge, List<Boolean> markEdge, int depth) {
        vis.set(u, true);
        q[u] = 1;
        d[u] = depth;
        List<Pair<Integer, Long>> neighbours = new ArrayList<>();
        for (Pair<Integer, Long> edge : graph.edgesOf(u)) {
            int num = edge.second.intValue();
            int back_num = Graph.companionEdge(num);
            double forward_x = x[num];
            double back_x = x[back_num];
            Pair<Integer, Integer> backEdge = graph.getEdges().get(back_num);
            if (backEdge.second != u) {
                throw new RuntimeException("unexpected back edge");
            }
            if (back_x >= HALF || forward_x >= HALF) {
                x[back_num] = 0;
                x[num] = 1;
                neighbours.add(edge);
            } else {
                x[back_num] = 0;
                x[num] = 0;
            }
            markEdge.set(back_num, true);
            markEdge.set(num, true);
        }
        if (parent_edge != null) {
            if (graph.getEdges().get(parent_edge).second != u) {
                throw new RuntimeException("unexpected parent edge");
            }
            x[parent_edge] = 1;
            x[Graph.companionEdge(parent_edge)] = 0;
        }
        for (Pair<Integer, Long> v : neighbours) {
            if (!vis.get(v.first) && q[v.first] >= HALF) {
                dfs(v.first, vis, v.second.intValue(), markEdge, depth + 1);
            }
        }
    }

    private void check() {
        double[] new_p = matrix.mult(new Matrix(a).transpose()).transpose().getRow(0);
        if (new_p.length != f.length) {
            throw new RuntimeException("solution check failed: 1");
        }
        double l1norm = 0;
        for (int i = 0; i < new_p.length; i++) {
            if (Math.abs(new_p[i] - (f[i] - g[i])) > EPS) {
                throw new RuntimeException("solution check failed: 2");
            }
            l1norm += Math.abs(new_p[i]);
        }
        if (Math.abs(l1norm - L1NORM) > EPS) {
            throw new RuntimeException("solution check failed: 3");
        }
        for (int i = 0; i < alpha.length; i++) {
            if (!((alpha[i] == 0 && beta[i] == 1) || (alpha[i] == 1 && beta[i] == 0))) {
                throw new RuntimeException("solution check failed: 4");
            }
        }
    }

    public double[] getAllCopy() {
        int cnt = 0;
        int new_size = a.length + f.length + g.length + alpha.length + beta.length +
                r.length + d.length + q.length + x.length;
        double[] res = new double[new_size];
        for (double y : a) res[cnt++] = y;
        for (double y : f) res[cnt++] = y;
        for (double y : g) res[cnt++] = y;
        for (double y : alpha) res[cnt++] = y;
        for (double y : beta) res[cnt++] = y;
        for (double y : r) res[cnt++] = y;
        for (double y : d) res[cnt++] = y;
        for (double y : q) res[cnt++] = y;
        for (double y : x) res[cnt++] = y;
        return res;
    }

    @Override
    public String toString() {
        return "Solution{\n" +
                "| a = " + Arrays.toString(a) + "\n" +
                "| f = " + Arrays.toString(f) + "\n" +
                "| g = " + Arrays.toString(g) + "\n" +
                "| alpha = " + Arrays.toString(alpha) + "\n" +
                "| beta = " + Arrays.toString(beta) + "\n" +
                "| r = " + Arrays.toString(r) + "\n" +
                "| d = " + Arrays.toString(d) + "\n" +
                "| q = " + Arrays.toString(q) + "\n" +
                "| x = " + Arrays.toString(x) + "\n" +
                "\n";
    }
}
