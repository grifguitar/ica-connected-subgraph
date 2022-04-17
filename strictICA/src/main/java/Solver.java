import graph.Graph;
import ilog.concert.*;
import ilog.cplex.*;
import utils.Matrix;
import utils.Pair;

import java.io.PrintWriter;
import java.util.*;

public class Solver {

    // constants:

    private final static float INF = 1000;
    //private static float N_INF = INF;
    private final static int TIME_LIMIT = 100;
    private final static int L1NORM = 250;

    private final Matrix matrix;
    private final Graph graph;

    private final List<IloNumVar> r;
    //private final List<IloNumVar> d;
    private final List<IloNumVar> q;
    private final List<IloNumVar> x;


    private final IloCplex cplex;

    private final List<IloNumVar> vars_A;
    private final List<IloNumVar> vars_P;

//    private final List<IloNumVar> vars_D;
//    private final List<IloNumVar> vars_E;
//    private final List<IloNumVar> vars_alpha;
//    private final List<IloNumVar> vars_beta;

    public Solver(Matrix matrix, Graph graph) throws IloException {
        this.matrix = matrix;
        this.graph = graph;

        if (matrix.numRows() != graph.getNodesCount()) {
            throw new RuntimeException("unexpected");
        }

        //N_INF = matrix.numRows();

        this.cplex = new IloCplex();
        this.cplex.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal);

        this.cplex.setParam(IloCplex.Param.TimeLimit, TIME_LIMIT);

        this.vars_A = new ArrayList<>();
        this.vars_P = new ArrayList<>();

//        this.vars_D = new ArrayList<>();
//        this.vars_E = new ArrayList<>();
//        this.vars_alpha = new ArrayList<>();
//        this.vars_beta = new ArrayList<>();

        this.r = new ArrayList<>();
        //this.d = new ArrayList<>();
        this.q = new ArrayList<>();
        this.x = new ArrayList<>();

        addVariables();
        addObjective();
        addConstraint();
        addConnectivityConstraint();
    }

    private void addVariables() throws IloException {
        for (int i = 0; i < matrix.numCols(); i++) {
            vars_A.add(cplex.numVar(-INF, INF, IloNumVarType.Float, varNameOf("a", i)));
        }
        for (int i = 0; i < matrix.numRows(); i++) {
            vars_P.add(cplex.numVar(-INF, INF, IloNumVarType.Float, varNameOf("p", i)));
        }

//        for (int i = 0; i < matrix.numRows(); i++) {
//            vars_D.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("d", i)));
//        }
//        for (int i = 0; i < matrix.numRows(); i++) {
//            vars_E.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("e", i)));
//        }
//        for (int i = 0; i < matrix.numRows(); i++) {
//            vars_alpha.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("alpha", i)));
//        }
//        for (int i = 0; i < matrix.numRows(); i++) {
//            vars_beta.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("beta", i)));
//        }

        for (int i = 0; i < graph.getNodesCount(); i++) {
            //d.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("d", i)));
            r.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("r", i)));
            q.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("q", i)));
        }

        for (Pair<Integer, Integer> edge : graph.getEdges()) {
            x.add(cplex.numVar(0, 1, IloNumVarType.Int, "x_" + edge.first + "_" + edge.second));
        }
    }

    private void addObjective() throws IloException {
        IloNumExpr[] squares = new IloNumExpr[matrix.numCols()];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = cplex.prod(vars_A.get(i), vars_A.get(i));
        }
        cplex.addMaximize(cplex.sum(squares));
    }

    private void addConstraint() throws IloException {
        for (int i = 0; i < matrix.numRows(); i++) {
            cplex.addEq(cplex.scalProd(matrix.getRow(i), toArray(vars_A)), vars_P.get(i));
            //cplex.addEq(cplex.scalProd(matrix.getRow(i), toArray(vars_A)), cplex.diff(vars_D.get(i), vars_E.get(i)));
        }

//        for (int i = 0; i < matrix.numRows(); i++) {
//            cplex.addEq(cplex.sum(vars_alpha.get(i), vars_beta.get(i)), 1);
//        }
//        for (int i = 0; i < matrix.numRows(); i++) {
//            cplex.addGe(cplex.prod(vars_alpha.get(i), INF), vars_D.get(i));
//            cplex.addGe(cplex.prod(vars_beta.get(i), INF), vars_E.get(i));
//        }

        IloNumExpr[] l1normP = new IloNumExpr[matrix.numRows()];
        for (int i = 0; i < l1normP.length; i++) {
            //l1normP[i] = cplex.sum(vars_D.get(i), vars_E.get(i));
            l1normP[i] = cplex.abs(vars_P.get(i));
        }
        cplex.addEq(cplex.sum(l1normP), L1NORM);
    }

    private void addConnectivityConstraint() throws IloException {
        connectivity1();
        connectivity3();
        connectivity4();
        //connectivity11();
        //connectivity12_13();
        //connectivity_new();
    }

    private void connectivity1() throws IloException {
        IloNumVar[] r2 = new IloNumVar[graph.getNodesCount()];
        for (int i = 0; i < graph.getNodesCount(); i++) {
            r2[i] = r.get(i);
        }
        cplex.addEq(cplex.sum(r2), 1);
    }

    private void connectivity3() throws IloException {
        for (int node = 0; node < graph.getNodesCount(); node++) {
            IloNumVar[] x_from_node = new IloNumVar[graph.edgesOf(node).size()];
            IloNumVar[] x_to_node = new IloNumVar[graph.edgesOf(node).size()];
            int cnt = 0;
            for (Pair<Integer, Long> edge : graph.edgesOf(node)) {
                int num = edge.second.intValue();
                if (num % 2 == 0) {
                    x_from_node[cnt] = x.get(num);
                    x_to_node[cnt] = x.get(num + 1);
                } else {
                    x_from_node[cnt] = x.get(num);
                    x_to_node[cnt] = x.get(num - 1);
                }
                cnt++;
            }
            // x[from][to] + r[to] = q[to]
            cplex.addEq(cplex.sum(cplex.sum(x_to_node), r.get(node)), q.get(node));
            // x[from][to] <= inf * q[from]
            cplex.addGe(cplex.prod(INF, q.get(node)), cplex.sum(x_from_node));
        }
    }

    private void connectivity4() throws IloException {
        for (int i = 0; i < graph.getEdges().size(); i += 2) {
            cplex.addGe(1, cplex.sum(x.get(i), x.get(i + 1)));
            Pair<Integer, Integer> edge = graph.getEdges().get(i);
            cplex.addGe(q.get(edge.first), x.get(i));
            cplex.addGe(q.get(edge.second), x.get(i));
            cplex.addGe(q.get(edge.first), x.get(i + 1));
            cplex.addGe(q.get(edge.second), x.get(i + 1));
        }
    }

//    private void connectivity_new() throws IloException {
//        for (int i = 0; i < graph.getEdges().size(); i++) {
//            Pair<Integer, Integer> edge = graph.getEdges().get(i);
//            int _from = edge.first;
//            int _to = edge.second;
//            // d[to] - d[from] >= x[i] - INF * (1 - x[i])
//            cplex.addGe(
//                    cplex.diff(d.get(_to), d.get(_from)),
//                    cplex.diff(x.get(i), cplex.prod(INF, cplex.diff(1, x.get(i))))
//            );
//        }
//    }

//    private void connectivity11() throws IloException {
//        for (Node node : graph.getNodes()) {
//            cplex.addGe(
//                    N_INF + 1,
//                    cplex.sum(d.get(node), cplex.prod(N_INF, r.get(node)))
//            );
//        }
//    }

//    private void connectivity12_13() throws IloException {
//        for (Edge edge : graph.getEdges()) {
//            Node _from = edge.getFrom();
//            Node _to = edge.getTo();
//            //(12)
//            cplex.addGe(
//                    cplex.diff(cplex.sum(N_INF, d.get(_to)), d.get(_from)),
//                    cplex.prod(N_INF + 1, x.get(edge).first)
//            );
//            cplex.addGe(
//                    cplex.diff(cplex.sum(N_INF, d.get(_from)), d.get(_to)),
//                    cplex.prod(N_INF + 1, x.get(edge).second)
//            );
//            //(13)
//            cplex.addGe(
//                    cplex.diff(cplex.sum(N_INF, d.get(_from)), d.get(_to)),
//                    cplex.prod(N_INF - 1, x.get(edge).first)
//            );
//            cplex.addGe(
//                    cplex.diff(cplex.sum(INF, d.get(_to)), d.get(_from)),
//                    cplex.prod(N_INF - 1, x.get(edge).second)
//            );
//        }
//    }

    public boolean solve() throws IloException {
        return cplex.solve();
    }

    public void printResults(PrintWriter out) throws IloException {
        System.out.println("obj = " + cplex.getObjValue());
        for (int i = 0; i < vars_A.size(); i++) {
            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(vars_A.get(i)));
        }
        for (int i = 0; i < vars_P.size(); i++) {
            System.out.println(varNameOf("p", i) + " = " + cplex.getValue(vars_P.get(i)));
            out.println(cplex.getValue(vars_P.get(i)));
        }
//        for (int i = 0; i < vars_D.size(); i++) {
//            System.out.println(varNameOf("d", i) + " = " + cplex.getValue(vars_D.get(i)));
//        }
//        for (int i = 0; i < vars_E.size(); i++) {
//            System.out.println(varNameOf("e", i) + " = " + cplex.getValue(vars_E.get(i)));
//        }
    }

    private static String varNameOf(String arg1, int arg2) {
        return arg1 + arg2;
    }

    private static IloNumVar[] toArray(List<IloNumVar> arg) {
        IloNumVar[] result = new IloNumVar[arg.size()];
        for (int i = 0; i < arg.size(); i++) {
            result[i] = arg.get(i);
        }
        return result;
    }

}

//class Solver {
//
//    final AtomicReference<Double> lb = new AtomicReference<>((double) -INF);
//
//    final static float INF = 1000;
//    final static float L1NORM = 250;
//
//    final IloCplex cplex;
//    final Vars vars;
//
//    Solver(Matrix matrix, Graph graph) throws IloException {
//        this.vars = new Vars(matrix);
//
//        this.cplex = new IloCplex();
//        this.cplex.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal);
//
//        this.cplex.setParam(IloCplex.Param.TimeLimit, 20);
//
//        addVariables();
//        addObjective();
//        addConstraint();
//
//        tuning();
//    }
//
//    void addVariables() throws IloException {
//        for (int i = 0; i < vars.D; i++) {
//            vars._A.add(cplex.numVar(-INF, INF, IloNumVarType.Float, varNameOf("a", i)));
//        }
//        for (int i = 0; i < vars.N; i++) {
//            vars._D.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("d", i)));
//        }
//        for (int i = 0; i < vars.N; i++) {
//            vars._E.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("e", i)));
//        }
//        for (int i = 0; i < vars.N; i++) {
//            vars._ALPHA.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("alpha", i)));
//        }
//        for (int i = 0; i < vars.N; i++) {
//            vars._BETA.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("beta", i)));
//        }
//    }
//
//    void addObjective() throws IloException {
//        IloNumExpr[] squares = new IloNumExpr[vars.D];
//        for (int i = 0; i < squares.length; i++) {
//            squares[i] = cplex.prod(vars._A.get(i), vars._A.get(i));
//        }
//        cplex.addMaximize(cplex.sum(squares));
//    }
//
//    void addConstraint() throws IloException {
//        for (int i = 0; i < vars.N; i++) {
//            cplex.addEq(cplex.scalProd(vars.matrix.getRow(i), toArray(vars._A)), cplex.diff(vars._D.get(i), vars._E.get(i)));
//        }
//        for (int i = 0; i < vars.N; i++) {
//            cplex.addEq(cplex.sum(vars._ALPHA.get(i), vars._BETA.get(i)), 1);
//        }
//        for (int i = 0; i < vars.N; i++) {
//            cplex.addGe(cplex.prod(vars._ALPHA.get(i), INF), vars._D.get(i));
//            cplex.addGe(cplex.prod(vars._BETA.get(i), INF), vars._E.get(i));
//        }
//
//        IloNumExpr[] l1normP = new IloNumExpr[vars.N];
//        for (int i = 0; i < l1normP.length; i++) {
//            l1normP[i] = cplex.sum(vars._D.get(i), vars._E.get(i));
//        }
//        cplex.addEq(cplex.sum(l1normP), L1NORM);
//    }
//
//    void tuning() throws IloException {
//        //cplex.use(new MIPCallback(false));
//        cplex.use(new Callback());
//    }
//
//    boolean solve() throws IloException {
//        return cplex.solve();
//    }
//
//    void printResults() throws IloException {
//        System.out.println("obj = " + cplex.getObjValue());
//        for (int i = 0; i < vars._A.size(); i++) {
//            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(vars._A.get(i)));
//        }
//        for (int i = 0; i < vars._D.size(); i++) {
//            System.out.println(varNameOf("d", i) + " = " + cplex.getValue(vars._D.get(i)));
//        }
//        for (int i = 0; i < vars._E.size(); i++) {
//            System.out.println(varNameOf("e", i) + " = " + cplex.getValue(vars._E.get(i)));
//        }
//    }
//
//    static IloNumVar[] toArray(List<IloNumVar> arg) {
//        return arg.toArray(new IloNumVar[0]);
//    }
//
//    static String varNameOf(String arg1, int arg2) {
//        return arg1 + arg2;
//    }
//
//    class Callback extends IloCplex.HeuristicCallback {
//
//        @Override
//        protected void main() throws IloException {
//            IloNumVar[] ax = toArray(vars._A);
//            IloNumVar[] dx = toArray(vars._D);
//            IloNumVar[] ex = toArray(vars._E);
//            IloNumVar[] alphax = toArray(vars._ALPHA);
//            IloNumVar[] betax = toArray(vars._BETA);
//
//            Solution solution = new Solution(
//                    new Pair<>(ax, getValues(ax)),
//                    new Pair<>(dx, getValues(dx)),
//                    new Pair<>(ex, getValues(ex)),
//                    new Pair<>(alphax, getValues(alphax)),
//                    new Pair<>(betax, getValues(betax)),
//                    vars.matrix
//            );
//
//            //System.out.println(solution);
//
//            if (!solution.adapt()) {
//                return;
//            }
//
//            //System.out.println("### ADAPT: ###");
//            System.out.println(solution);
//
//            int cnt = 0;
//            IloNumVar[] list1 = new IloNumVar[solution._A_var.length + solution._D_var.length * 4];
//            for (IloNumVar x : solution._A_var) list1[cnt++] = x;
//            for (IloNumVar x : solution._D_var) list1[cnt++] = x;
//            for (IloNumVar x : solution._E_var) list1[cnt++] = x;
//            for (IloNumVar x : solution._ALPHA_var) list1[cnt++] = x;
//            for (IloNumVar x : solution._BETA_var) list1[cnt++] = x;
//
//            cnt = 0;
//            double[] list2 = new double[solution._A.length + solution._D.length * 4];
//            for (double x : solution._A) list2[cnt++] = x;
//            for (double x : solution._D) list2[cnt++] = x;
//            for (double x : solution._E) list2[cnt++] = x;
//            for (double x : solution._ALPHA) list2[cnt++] = x;
//            for (double x : solution._BETA) list2[cnt++] = x;
//
//            if (solution.getObjValue() > getIncumbentObjValue()) {
//                lb.set(solution.getObjValue());
//                setSolution(list1, list2);
//            }
//
//        }
//    }
//
////    class MIPCallback extends IloCplex.IncumbentCallback {
////        final boolean silence;
////
////        MIPCallback(boolean silence) {
////            this.silence = silence;
////        }
////
////        @Override
////        protected void main() throws IloException {
////            while (true) {
////                double currLB = lb.get();
////
////                if (currLB >= getObjValue()) {
////                    break;
////                }
////
////                if (lb.compareAndSet(currLB, getObjValue()) && !silence) {
////                    System.out.println("Found new solution: " + getObjValue());
////                }
////            }
////        }
////    }
//
//    static class Vars {
//        final Matrix matrix;
//
//        final int N;
//        final int D;
//
//        final List<IloNumVar> _A = new ArrayList<>();
//        final List<IloNumVar> _D = new ArrayList<>();
//        final List<IloNumVar> _E = new ArrayList<>();
//        final List<IloNumVar> _ALPHA = new ArrayList<>();
//        final List<IloNumVar> _BETA = new ArrayList<>();
//
//        Vars(Matrix matrix) {
//            this.matrix = matrix;
//            this.N = matrix.numRows();
//            this.D = matrix.numCols();
//        }
//    }
//
//    static class Solution {
//        final static double EPS = 0.0001;
//
//        final Matrix matrix;
//
//        final IloNumVar[] _A_var;
//        final IloNumVar[] _D_var;
//        final IloNumVar[] _E_var;
//        final IloNumVar[] _ALPHA_var;
//        final IloNumVar[] _BETA_var;
//        final double[] _A;
//        final double[] _D;
//        final double[] _E;
//        final double[] _ALPHA;
//        final double[] _BETA;
//
//        Solution(
//                Pair<IloNumVar[], double[]> a,
//                Pair<IloNumVar[], double[]> d,
//                Pair<IloNumVar[], double[]> e,
//                Pair<IloNumVar[], double[]> alpha,
//                Pair<IloNumVar[], double[]> beta,
//                Matrix matrix
//        ) {
//            this._A_var = a.first;
//            this._D_var = d.first;
//            this._E_var = e.first;
//            this._ALPHA_var = alpha.first;
//            this._BETA_var = beta.first;
//
//            this._A = a.second;
//            this._D = d.second;
//            this._E = e.second;
//            this._ALPHA = alpha.second;
//            this._BETA = beta.second;
//
//            this.matrix = matrix;
//
//            if (_A.length != matrix.numCols() ||
//                    _D.length != matrix.numRows() ||
//                    _E.length != matrix.numRows() ||
//                    _ALPHA.length != matrix.numRows() ||
//                    _BETA.length != matrix.numRows()
//            ) {
//                throw new RuntimeException("solution checkSizes failed!");
//            }
//        }
//
//        double getObjValue() {
//            double ans = 0;
//            for (double v : _A) {
//                ans += v * v;
//            }
//            return ans;
//        }
//
//        boolean adapt() {
//            double[] p = matrix.mult(new Matrix(_A).transpose()).transpose().getRow(0);
//
//            //System.out.println("!!! p: " + Arrays.toString(p));
//
//            double l1norm = 0;
//            for (double val : p) {
//                l1norm += Math.abs(val);
//            }
//
//            //System.out.println("!!! l1norm: " + l1norm);
//
//            if (Math.abs(l1norm) < EPS) {
//                return false;
//            }
//
//            double coeff = L1NORM / l1norm;
//
//            for (int i = 0; i < _A.length; i++) {
//                _A[i] *= coeff;
//            }
//
//            double[] new_p = matrix.mult(new Matrix(_A).transpose()).transpose().getRow(0);
//
//            for (int i = 0; i < new_p.length; i++) {
//                if (new_p[i] < 0) {
//                    _D[i] = 0;
//                    _ALPHA[i] = 0;
//
//                    _E[i] = Math.abs(new_p[i]);
//                    _BETA[i] = 1;
//                } else {
//                    _D[i] = Math.abs(new_p[i]);
//                    _ALPHA[i] = 1;
//
//                    _E[i] = 0;
//                    _BETA[i] = 0;
//                }
//            }
//
//            check();
//
//            return true;
//        }
//
//        void check() {
//            double[] new_p = matrix.mult(new Matrix(_A).transpose()).transpose().getRow(0);
//
//            if (new_p.length != _D.length) {
//                throw new RuntimeException("solution check failed: 1");
//            }
//
//            double l1norm = 0;
//            for (int i = 0; i < new_p.length; i++) {
//                if (Math.abs(new_p[i] - (_D[i] - _E[i])) > EPS) {
//                    throw new RuntimeException("solution check failed: 2");
//                }
//                l1norm += Math.abs(new_p[i]);
//            }
//
//            if (Math.abs(l1norm - L1NORM) > EPS) {
//                throw new RuntimeException("solution check failed: 3");
//            }
//
//            for (int i = 0; i < _ALPHA.length; i++) {
//                if (!((_ALPHA[i] == 0 && _BETA[i] == 1) || (_ALPHA[i] == 1 && _BETA[i] == 0))) {
//                    throw new RuntimeException("solution check failed: 4");
//                }
//            }
//        }
//
//        @Override
//        public String toString() {
//            return "### SOLUTION BEGIN: ###\n" +
//                    "| value : " + getObjValue() + "\n" +
//                    "| _A " + " : " + Arrays.toString(_A) + "\n" +
////                    "| _D " + " : " + Arrays.toString(_D) + "\n" +
////                    "| _E " + " : " + Arrays.toString(_E) + "\n" +
////                    "| _ALPHA " + " : " + Arrays.toString(_ALPHA) + "\n" +
////                    "| _BETA " + " : " + Arrays.toString(_BETA) + "\n" +
//                    "### SOLUTION END; ###\n";
//        }
//
//    }
//
//}
