import graph.Graph;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import utils.Matrix;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class NewSolver {
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

    private final static float INF = 1000;
    private final static int TIME_LIMIT = 20;
    private final static float L1NORM = 250;

    private static PrintWriter out_debug;
    private static int gl_cnt = 0;

    static {
        try {
            out_debug = new PrintWriter("./logs/connect_sol.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * constructor:
     */

    public NewSolver(Matrix matrix, Graph graph) throws IloException {
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

        tuning();
    }

    /**
     * public methods:
     */

    public boolean solve() throws IloException {
        return cplex.solve();
    }

    public void printResults(PrintWriter out) throws IloException {
        out_debug.close();
        System.out.println("obj = " + cplex.getObjValue());
        for (int i = 0; i < D; i++) {
            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(_a.get(i)));
        }
        for (int i = 0; i < N; i++) {
            System.out.print(varNameOf("f", i) + " = " + cplex.getValue(_f.get(i)) + " ; ");
            System.out.print(varNameOf("g", i) + " = " + cplex.getValue(_g.get(i)) + " ; ");
            System.out.println();
            out.println(cplex.getValue(_f.get(i)) - cplex.getValue(_g.get(i)));
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
        }
    }

    private void addObjective() throws IloException {
        IloNumExpr[] squares = new IloNumExpr[D];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = cplex.prod(_a.get(i), _a.get(i));
        }
        cplex.addMaximize(cplex.sum(squares));
    }

    private static double calculateObjective(NewSolution sol) {
        double[] squares = new double[sol.a().length];
        double sum = 0;
        for (int i = 0; i < squares.length; i++) {
            squares[i] = sol.a()[i] * sol.a()[i];
            sum += squares[i];
        }
        return sum;
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

    private void tuning() throws IloException {
        cplex.use(new MyHeuristicCallback());
    }

    private class MyHeuristicCallback extends IloCplex.HeuristicCallback {
        @Override
        protected void main() throws IloException {
            IloNumVar[] old_vars = new IloNumVar[D + 4 * N];
            int cnt = 0;
            for (IloNumVar y : _a) old_vars[cnt++] = y;
            for (IloNumVar y : _f) old_vars[cnt++] = y;
            for (IloNumVar y : _g) old_vars[cnt++] = y;
            for (IloNumVar y : _alpha) old_vars[cnt++] = y;
            for (IloNumVar y : _beta) old_vars[cnt++] = y;

            NewSolution sol = new NewSolution(
                    L1NORM,
                    matrix,
                    graph,
                    getValues(toArray(_a)),
                    getValues(toArray(_f)),
                    getValues(toArray(_g)),
                    getValues(toArray(_alpha)),
                    getValues(toArray(_beta)),
                    new double[N],
                    new double[N],
                    new double[graph.getEdges().size()]
            );

            String stringSolution = sol.toString();

            if (!sol.adapt()) {
                return;
            }

//            System.out.println("Sol:");
//            System.out.println(stringSolution);
//            System.out.println("Adapt:");
//            System.out.println(sol);
//            System.out.println();
            out_debug.println(sol);
            try (PrintWriter out_sol = new PrintWriter("./graphics/p.txt")) {
                for (int i = 0; i < sol.q().length; i++) {
                    out_sol.println(sol.q()[i]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Main.drawing("final_only_dfs" + gl_cnt++);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            double[] new_values = new double[D + 4 * N];
            cnt = 0;
            for (double y : sol.a()) new_values[cnt++] = y;
            for (double y : sol.f()) new_values[cnt++] = y;
            for (double y : sol.g()) new_values[cnt++] = y;
            for (double y : sol.alpha()) new_values[cnt++] = y;
            for (double y : sol.beta()) new_values[cnt++] = y;


            if (calculateObjective(sol) > getIncumbentObjValue()) {
                setSolution(old_vars, new_values);
            }
        }
    }

    private static IloNumVar[] toArray(List<IloNumVar> arg) {
        return arg.toArray(new IloNumVar[0]);
    }

    private static String varNameOf(String arg1, int arg2) {
        return arg1 + arg2;
    }

}
