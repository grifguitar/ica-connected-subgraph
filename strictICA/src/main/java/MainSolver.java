import graph.Graph;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import utils.Matrix;
import utils.Pair;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class MainSolver {
    //private final AtomicReference<Double> lower_bound = new AtomicReference<>((double) -INF);

    /**
     * variables:
     */

    private final IloCplex cplex;
    private final Matrix matrix;
    private final Graph graph;

    private final int N;
    private final int D;

    private final List<IloNumVar> _a = new ArrayList<>();
    private final List<IloNumVar> _f = new ArrayList<>();
    private final List<IloNumVar> _g = new ArrayList<>();
    private final List<IloNumVar> _alpha = new ArrayList<>();
    private final List<IloNumVar> _beta = new ArrayList<>();

    private final List<IloNumVar> _r = new ArrayList<>();
    //private final List<IloNumVar> _d = new ArrayList<>();
    private final List<IloNumVar> _q = new ArrayList<>();
    private final List<IloNumVar> _x = new ArrayList<>();

    /**
     * static:
     */

    private final static boolean PRINT_DEBUG = true;
    private final static float INF = 1000;
    private final static int TIME_LIMIT = 20;
    private final static float L1NORM = 250;
    public final static double SMALL = 0.01;

    public static PrintWriter out_debug;
    public static int gl_cnt = 0;

    static {
        try {
            out_debug = new PrintWriter("./logs/connect_sol.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static IloNumVar[] toArray(List<IloNumVar> arg) {
        return arg.toArray(new IloNumVar[0]);
    }

    private static String varNameOf(String arg1, int arg2) {
        return arg1 + arg2;
    }

    /**
     * constructor:
     */

    public MainSolver(Matrix matrix, Graph graph) throws IloException {
        this.graph = graph;
        this.matrix = matrix;

        this.N = matrix.numRows();
        this.D = matrix.numCols();

        if (N != graph.getNodesCount()) {
            throw new RuntimeException("unexpected");
        }

        this.cplex = new IloCplex();
        this.cplex.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal);
        this.cplex.setParam(IloCplex.Param.TimeLimit, TIME_LIMIT);

        addVariables();
        addObjective();
        addConstraint();
        addConnectivityConstraint();

        tuning();
    }

    /**
     * public methods:
     */

    public boolean solve() throws IloException {
        return cplex.solve();
    }

    public void printResults(PrintWriter out) throws IloException {
        System.out.println("obj = " + cplex.getObjValue());
        for (int i = 0; i < D; i++) {
            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(_a.get(i)));
        }
        for (int i = 0; i < N; i++) {
            System.out.print(varNameOf("f", i) + " = " + cplex.getValue(_f.get(i)) + " ; ");
            System.out.print(varNameOf("g", i) + " = " + cplex.getValue(_g.get(i)));
            System.out.println();
            //out.println(cplex.getValue(_f.get(i)) - cplex.getValue(_g.get(i)));
        }
        for (int i = 0; i < N; i++) {
            System.out.print(varNameOf("r", i) + " = " + cplex.getValue(_r.get(i)) + " ; ");
            System.out.print(varNameOf("q", i) + " = " + cplex.getValue(_q.get(i)) + " ; ");
            //System.out.print(varNameOf("d", i) + " = " + cplex.getValue(_d.get(i)));
            System.out.println();
            //out.println(cplex.getValue(_f.get(i)) - cplex.getValue(_g.get(i)));
        }
    }

    /**
     * private methods:
     */

    private void addVariables() throws IloException {
        for (int i = 0; i < D; i++) {
            _a.add(cplex.numVar(-INF, INF, IloNumVarType.Float, varNameOf("a", i)));
        }
        for (int i = 0; i < N; i++) {
            _f.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("f", i)));
            _g.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("g", i)));
            _alpha.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("alpha", i)));
            _beta.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("beta", i)));

            //_d.add(cplex.numVar(1, INF, IloNumVarType.Float, varNameOf("d", i)));
            _r.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("r", i)));
            _q.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("q", i)));
        }
        for (Pair<Integer, Integer> edge : graph.getEdges()) {
            _x.add(cplex.numVar(0, 1, IloNumVarType.Int, "x_" + edge.first + "_" + edge.second));
        }
    }

    private void addObjective() throws IloException {
        IloNumExpr[] squares = new IloNumExpr[D];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = cplex.prod(_a.get(i), _a.get(i));
        }
        IloNumExpr[] q_prod = new IloNumExpr[N];
        IloNumExpr[] r_prod = new IloNumExpr[N];
        for (int i = 0; i < N; i++) {
            q_prod[i] = cplex.prod(cplex.diff(_f.get(i), _q.get(i)), (double) 1 / (double) N);
            q_prod[i] = cplex.prod(q_prod[i], q_prod[i]);
            r_prod[i] = cplex.prod(cplex.prod(_f.get(i), _r.get(i)), INF);
        }
        cplex.addMaximize(cplex.diff(cplex.sum(cplex.sum(squares), cplex.sum(r_prod)), cplex.sum(q_prod)));
    }

    private static double calculateObjective(Solution sol) {
        double objectVal = 0;
        for (int i = 0; i < sol.a().length; i++) {
            objectVal += (sol.a()[i] * sol.a()[i]);
        }
        for (int i = 0; i < sol.f().length; i++) {
            double y = (sol.f()[i] - sol.q()[i]) / (double) sol.f().length;
            objectVal -= (y * y);
            objectVal += (sol.f()[i] * sol.r()[i] * INF);
        }
        return objectVal;
    }

    private void addConstraint() throws IloException {
        for (int i = 0; i < N; i++) {
            cplex.addEq(cplex.scalProd(matrix.getRow(i), toArray(_a)), cplex.diff(_f.get(i), _g.get(i)));
        }
        for (int i = 0; i < N; i++) {
            cplex.addEq(cplex.sum(_alpha.get(i), _beta.get(i)), 1);
            cplex.addGe(cplex.prod(_alpha.get(i), INF), _f.get(i));
            cplex.addGe(cplex.prod(_beta.get(i), INF), _g.get(i));
        }

        IloNumExpr[] l1normP = new IloNumExpr[N];
        for (int i = 0; i < l1normP.length; i++) {
            l1normP[i] = cplex.sum(_f.get(i), _g.get(i));
        }
        cplex.addEq(cplex.sum(l1normP), L1NORM);
    }

    private void addConnectivityConstraint() throws IloException {
        connectivity1();
        connectivity3();
        connectivity4();
        connectivity_old();
    }

    private void connectivity1() throws IloException {
        cplex.addEq(cplex.sum(toArray(_r)), 1);
    }

    private void connectivity3() throws IloException {
        for (int node = 0; node < graph.getNodesCount(); node++) {
            IloNumVar[] x_from_node = new IloNumVar[graph.edgesOf(node).size()];
            IloNumVar[] x_to_node = new IloNumVar[graph.edgesOf(node).size()];
            int cnt = 0;
            for (Pair<Integer, Long> edge : graph.edgesOf(node)) {
                int num = edge.second.intValue();
                int back_num = Graph.companionEdge(num);
                x_from_node[cnt] = _x.get(num);
                x_to_node[cnt] = _x.get(back_num);
                cnt++;
            }
            // x[from][to] + r[to] = q[to]
            // cplex.addEq(cplex.sum(cplex.sum(x_to_node), _r.get(node)), _q.get(node));
            cplex.addEq(cplex.sum(cplex.sum(x_to_node), _r.get(node)), 1);
            // x[from][to] <= inf * q[from]
            // cplex.addGe(cplex.prod(INF, _q.get(node)), cplex.sum(x_from_node));
        }
    }

    private void connectivity4() throws IloException {
        for (int i = 0; i < graph.getEdges().size(); i += 2) {
            cplex.addGe(1, cplex.sum(_x.get(i), _x.get(i + 1)));
            Pair<Integer, Integer> edge = graph.getEdges().get(i);
            cplex.addGe(_q.get(edge.first), cplex.prod(_x.get(i), SMALL));
            cplex.addGe(_q.get(edge.second), cplex.prod(_x.get(i), SMALL));
            cplex.addGe(_q.get(edge.first), cplex.prod(_x.get(i + 1), SMALL));
            cplex.addGe(_q.get(edge.second), cplex.prod(_x.get(i + 1), SMALL));
        }
    }

    private void connectivity_old() throws IloException {
//        for (int node = 0; node < graph.getNodesCount(); node++) {
//            cplex.addGe(
//                    INF + 1,
//                    cplex.sum(_d.get(node), cplex.prod(INF, _r.get(node)))
//            );
//        }
//        for (int i = 0; i < graph.getEdges().size(); i++) {
//            Pair<Integer, Integer> edge = graph.getEdges().get(i);
//            int _from = edge.first;
//            int _to = edge.second;
//            // INF + d[to] - d[from] >= (INF + 1) * x[i]
//            cplex.addGe(
//                    cplex.sum(INF, cplex.diff(_d.get(_to), _d.get(_from))),
//                    cplex.prod(INF + 1, _x.get(i))
//            );
//            cplex.addGe(
//                    cplex.sum(INF, cplex.diff(_d.get(_from), _d.get(_to))),
//                    cplex.prod(INF - 1, _x.get(i))
//            );
//        }
        for (int i = 0; i < graph.getEdges().size(); i++) {
            Pair<Integer, Integer> edge = graph.getEdges().get(i);
            int _from = edge.first;
            int _to = edge.second;
            // INF + q[from] - q[to] >= (INF) * x[i] + 0.1
            cplex.addGe(
                    cplex.sum(INF, cplex.diff(_q.get(_from), _q.get(_to))),
                    cplex.sum(cplex.prod(INF, _x.get(i)), SMALL)
            );
        }
    }

    private void tuning() throws IloException {
        //cplex.use(new MIPCallback(false));
        cplex.use(new MyHeuristicCallback());
    }

    private IloNumVar[] getAllCopy() {
        int cnt = 0;
        int new_size = _a.size() + _f.size() + _g.size() + _alpha.size() + _beta.size() +
                _r.size() + /*_d.size()*/ +_q.size() + _x.size();
        IloNumVar[] res = new IloNumVar[new_size];
        for (IloNumVar y : _a) res[cnt++] = y;
        for (IloNumVar y : _f) res[cnt++] = y;
        for (IloNumVar y : _g) res[cnt++] = y;
        for (IloNumVar y : _alpha) res[cnt++] = y;
        for (IloNumVar y : _beta) res[cnt++] = y;
        for (IloNumVar y : _r) res[cnt++] = y;
        //for (IloNumVar y : _d) res[cnt++] = y;
        for (IloNumVar y : _q) res[cnt++] = y;
        for (IloNumVar y : _x) res[cnt++] = y;
        return res;
    }

    private class MyHeuristicCallback extends IloCplex.HeuristicCallback {
        @Override
        protected void main() throws IloException {
            IloNumVar[] list_vars = getAllCopy();

            Solution sol = new Solution(
                    MainSolver.L1NORM,
                    matrix,
                    graph,
                    getValues(toArray(_a)),
                    getValues(toArray(_f)),
                    getValues(toArray(_g)),
                    getValues(toArray(_alpha)),
                    getValues(toArray(_beta)),
                    getValues(toArray(_r)),
                    //getValues(toArray(_d)),
                    getValues(toArray(_q)),
                    getValues(toArray(_x))
            );

            String stringSolution = sol.toString();

            if (!sol.adapt()) {
                return;
            }

            if (PRINT_DEBUG) {
                System.out.println("Sol:");
                System.out.println(stringSolution);
                System.out.println("Adapt:");
                System.out.println(sol);
                System.out.println();
                //out_debug.println(calculateObjective(sol) + ":");
                out_debug.println(sol);
                try (PrintWriter out_sol = new PrintWriter("./graphics/p.txt")) {
                    for (int i = 0; i < sol.f().length; i++) {
                        out_sol.println(sol.q()[i]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Main.drawing("roc" + gl_cnt++);
            }

            if (calculateObjective(sol) > getIncumbentObjValue()) {
                //lower_bound.set(sol.getObjValue());
                setSolution(list_vars, sol.getAllCopy());
            }
        }
    }

//    class MIPCallback extends IloCplex.IncumbentCallback {
//        final boolean silence;
//
//        MIPCallback(boolean silence) {
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

}
