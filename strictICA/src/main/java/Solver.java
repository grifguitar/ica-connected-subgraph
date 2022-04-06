import ilog.concert.*;
import ilog.cplex.*;
import utils.Matrix;
import utils.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Solver {

    private final AtomicReference<Double> lb = new AtomicReference<>((double) -INF);

    private final static float INF = 1000;
    private final static float L1NORM = 1200;

    private final IloCplex cplex;
    private final Vars vars;

    public Solver(Matrix matrix) throws IloException {
        this.vars = new Vars(matrix);

        this.cplex = new IloCplex();
        this.cplex.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal);

        addVariables();
        addObjective();
        addConstraint();

        tuning();
    }

    private void addVariables() throws IloException {
        for (int i = 0; i < vars.D; i++) {
            vars._A.add(cplex.numVar(-INF, INF, IloNumVarType.Float, varNameOf("a", i)));
        }
        for (int i = 0; i < vars.N; i++) {
            vars._D.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("d", i)));
        }
        for (int i = 0; i < vars.N; i++) {
            vars._E.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("e", i)));
        }
        for (int i = 0; i < vars.N; i++) {
            vars._ALPHA.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("alpha", i)));
        }
        for (int i = 0; i < vars.N; i++) {
            vars._BETA.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("beta", i)));
        }
    }

    private void addObjective() throws IloException {
        IloNumExpr[] squares = new IloNumExpr[vars.D];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = cplex.prod(vars._A.get(i), vars._A.get(i));
        }
        cplex.addMaximize(cplex.sum(squares));
    }

    private void addConstraint() throws IloException {
        for (int i = 0; i < vars.N; i++) {
            cplex.addEq(cplex.scalProd(vars.matrix.getRow(i), toArray(vars._A)), cplex.diff(vars._D.get(i), vars._E.get(i)));
        }
        for (int i = 0; i < vars.N; i++) {
            cplex.addEq(cplex.sum(vars._ALPHA.get(i), vars._BETA.get(i)), 1);
        }
        for (int i = 0; i < vars.N; i++) {
            cplex.addGe(cplex.prod(vars._ALPHA.get(i), INF), vars._D.get(i));
            cplex.addGe(cplex.prod(vars._BETA.get(i), INF), vars._E.get(i));
        }

        IloNumExpr[] l1normP = new IloNumExpr[vars.N];
        for (int i = 0; i < l1normP.length; i++) {
            l1normP[i] = cplex.sum(vars._D.get(i), vars._E.get(i));
        }
        cplex.addEq(cplex.sum(l1normP), L1NORM);
    }

    private void tuning() throws IloException {
        //cplex.use(new MIPCallback(false));
        cplex.use(new Callback());
    }

    public boolean solve() throws IloException {
        return cplex.solve();
    }

    public void printResults() throws IloException {
        System.out.println("obj = " + cplex.getObjValue());
        for (int i = 0; i < vars._A.size(); i++) {
            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(vars._A.get(i)));
        }
        for (int i = 0; i < vars._D.size(); i++) {
            System.out.println(varNameOf("d", i) + " = " + cplex.getValue(vars._D.get(i)));
        }
        for (int i = 0; i < vars._E.size(); i++) {
            System.out.println(varNameOf("e", i) + " = " + cplex.getValue(vars._E.get(i)));
        }
    }

    private static IloNumVar[] toArray(List<IloNumVar> arg) {
        return arg.toArray(new IloNumVar[0]);
    }

    private static String varNameOf(String arg1, int arg2) {
        return arg1 + arg2;
    }

    private class Callback extends IloCplex.HeuristicCallback {

        @Override
        protected void main() throws IloException {
            IloNumVar[] ax = toArray(vars._A);
            IloNumVar[] dx = toArray(vars._D);
            IloNumVar[] ex = toArray(vars._E);
            IloNumVar[] alphax = toArray(vars._ALPHA);
            IloNumVar[] betax = toArray(vars._BETA);

            Solution solution = new Solution(
                    new Pair<>(ax, getValues(ax)),
                    new Pair<>(dx, getValues(dx)),
                    new Pair<>(ex, getValues(ex)),
                    new Pair<>(alphax, getValues(alphax)),
                    new Pair<>(betax, getValues(betax)),
                    vars.matrix
            );

            //System.out.println(solution);

            if (!solution.adapt()) {
                return;
            }

            //System.out.println("### ADAPT: ###");
            System.out.println(solution);

            int cnt = 0;
            IloNumVar[] list1 = new IloNumVar[solution._A_var.length + solution._D_var.length * 4];
            for (IloNumVar x : solution._A_var) list1[cnt++] = x;
            for (IloNumVar x : solution._D_var) list1[cnt++] = x;
            for (IloNumVar x : solution._E_var) list1[cnt++] = x;
            for (IloNumVar x : solution._ALPHA_var) list1[cnt++] = x;
            for (IloNumVar x : solution._BETA_var) list1[cnt++] = x;

            cnt = 0;
            double[] list2 = new double[solution._A.length + solution._D.length * 4];
            for (double x : solution._A) list2[cnt++] = x;
            for (double x : solution._D) list2[cnt++] = x;
            for (double x : solution._E) list2[cnt++] = x;
            for (double x : solution._ALPHA) list2[cnt++] = x;
            for (double x : solution._BETA) list2[cnt++] = x;

            if (solution.getObjValue() > getIncumbentObjValue()) {
                lb.set(solution.getObjValue());
                setSolution(list1, list2);
            }

        }
    }

//    private class MIPCallback extends IloCplex.IncumbentCallback {
//        private final boolean silence;
//
//        public MIPCallback(boolean silence) {
//            this.silence = silence;
//        }
//
//        @Override
//        protected void main() throws IloException {
//            while (true) {
//                double currLB = lb.get();
//
//                if (currLB >= getObjValue()) {
//                    break;
//                }
//
//                if (lb.compareAndSet(currLB, getObjValue()) && !silence) {
//                    System.out.println("Found new solution: " + getObjValue());
//                }
//            }
//        }
//    }

    private static class Vars {
        private final Matrix matrix;

        public final int N;
        public final int D;

        public final List<IloNumVar> _A = new ArrayList<>();
        public final List<IloNumVar> _D = new ArrayList<>();
        public final List<IloNumVar> _E = new ArrayList<>();
        public final List<IloNumVar> _ALPHA = new ArrayList<>();
        public final List<IloNumVar> _BETA = new ArrayList<>();

        public Vars(Matrix matrix) {
            this.matrix = matrix;
            this.N = matrix.numRows();
            this.D = matrix.numCols();
        }
    }

    private static class Solution {
        private final static double EPS = 0.0001;

        private final Matrix matrix;

        public final IloNumVar[] _A_var;
        public final IloNumVar[] _D_var;
        public final IloNumVar[] _E_var;
        public final IloNumVar[] _ALPHA_var;
        public final IloNumVar[] _BETA_var;
        public final double[] _A;
        public final double[] _D;
        public final double[] _E;
        public final double[] _ALPHA;
        public final double[] _BETA;

        public Solution(
                Pair<IloNumVar[], double[]> a,
                Pair<IloNumVar[], double[]> d,
                Pair<IloNumVar[], double[]> e,
                Pair<IloNumVar[], double[]> alpha,
                Pair<IloNumVar[], double[]> beta,
                Matrix matrix
        ) {
            this._A_var = a.first;
            this._D_var = d.first;
            this._E_var = e.first;
            this._ALPHA_var = alpha.first;
            this._BETA_var = beta.first;

            this._A = a.second;
            this._D = d.second;
            this._E = e.second;
            this._ALPHA = alpha.second;
            this._BETA = beta.second;

            this.matrix = matrix;

            if (_A.length != matrix.numCols() ||
                    _D.length != matrix.numRows() ||
                    _E.length != matrix.numRows() ||
                    _ALPHA.length != matrix.numRows() ||
                    _BETA.length != matrix.numRows()
            ) {
                throw new RuntimeException("solution checkSizes failed!");
            }
        }

        public double getObjValue() {
            double ans = 0;
            for (double v : _A) {
                ans += v * v;
            }
            return ans;
        }

        public boolean adapt() {
            double[] p = matrix.mult(new Matrix(_A).transpose()).transpose().getRow(0);

            //System.out.println("!!! p: " + Arrays.toString(p));

            double l1norm = 0;
            for (double val : p) {
                l1norm += Math.abs(val);
            }

            //System.out.println("!!! l1norm: " + l1norm);

            if (Math.abs(l1norm) < EPS) {
                return false;
            }

            double coeff = L1NORM / l1norm;

            for (int i = 0; i < _A.length; i++) {
                _A[i] *= coeff;
            }

            double[] new_p = matrix.mult(new Matrix(_A).transpose()).transpose().getRow(0);

            for (int i = 0; i < new_p.length; i++) {
                if (new_p[i] < 0) {
                    _D[i] = 0;
                    _ALPHA[i] = 0;

                    _E[i] = Math.abs(new_p[i]);
                    _BETA[i] = 1;
                } else {
                    _D[i] = Math.abs(new_p[i]);
                    _ALPHA[i] = 1;

                    _E[i] = 0;
                    _BETA[i] = 0;
                }
            }

            check();

            return true;
        }

        private void check() {
            double[] new_p = matrix.mult(new Matrix(_A).transpose()).transpose().getRow(0);

            if (new_p.length != _D.length) {
                throw new RuntimeException("solution check failed: 1");
            }

            double l1norm = 0;
            for (int i = 0; i < new_p.length; i++) {
                if (Math.abs(new_p[i] - (_D[i] - _E[i])) > EPS) {
                    throw new RuntimeException("solution check failed: 2");
                }
                l1norm += Math.abs(new_p[i]);
            }

            if (Math.abs(l1norm - L1NORM) > EPS) {
                throw new RuntimeException("solution check failed: 3");
            }

            for (int i = 0; i < _ALPHA.length; i++) {
                if (!((_ALPHA[i] == 0 && _BETA[i] == 1) || (_ALPHA[i] == 1 && _BETA[i] == 0))) {
                    throw new RuntimeException("solution check failed: 4");
                }
            }
        }

        @Override
        public String toString() {
            return "### SOLUTION BEGIN: ###\n" +
                    "| value : " + getObjValue() + "\n" +
                    "| _A " + " : " + Arrays.toString(_A) + "\n" +
//                    "| _D " + " : " + Arrays.toString(_D) + "\n" +
//                    "| _E " + " : " + Arrays.toString(_E) + "\n" +
//                    "| _ALPHA " + " : " + Arrays.toString(_ALPHA) + "\n" +
//                    "| _BETA " + " : " + Arrays.toString(_BETA) + "\n" +
                    "### SOLUTION END; ###\n";
        }

    }

}
