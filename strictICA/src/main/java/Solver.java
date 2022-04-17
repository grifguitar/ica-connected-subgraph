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
    private final static int TIME_LIMIT = 100;
    private final static int L1NORM = 250;

    private final Matrix matrix;
    private final Graph graph;

    private final List<IloNumVar> r;
    private final List<IloNumVar> d;
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
        this.d = new ArrayList<>();
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
            d.add(cplex.numVar(1, INF, IloNumVarType.Float, varNameOf("d", i)));
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
        IloNumExpr[] s_prod = new IloNumExpr[vars_P.size()];
        for (int i = 0; i < s_prod.length; i++) {
            s_prod[i] = cplex.diff(q.get(i), vars_P.get(i));
            s_prod[i] = cplex.prod(s_prod[i], s_prod[i]);
        }
//        cplex.addMaximize(
//                cplex.diff(
//                        cplex.sum(cplex.sum(squares), cplex.sum(s_prod)),
//                        cplex.sum(toArray(q))
//                )
//        );
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
        connectivity_old();
    }

    private void connectivity1() throws IloException {
        cplex.addEq(cplex.sum(toArray(r)), 1);
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

    private void connectivity_new() throws IloException {
        for (int i = 0; i < graph.getEdges().size(); i++) {
            Pair<Integer, Integer> edge = graph.getEdges().get(i);
            int _from = edge.first;
            int _to = edge.second;
            // d[to] - d[from] >= x[i] - INF * (1 - x[i])
            cplex.addGe(
                    cplex.diff(d.get(_to), d.get(_from)),
                    cplex.diff(x.get(i), cplex.prod(INF, cplex.diff(1, x.get(i))))
            );
        }
    }

    private void connectivity_old() throws IloException {
        for (int node = 0; node < graph.getNodesCount(); node++) {
            cplex.addGe(
                    INF + 1,
                    cplex.sum(d.get(node), cplex.prod(INF, r.get(node)))
            );
        }
        for (int i = 0; i < graph.getEdges().size(); i++) {
            Pair<Integer, Integer> edge = graph.getEdges().get(i);
            int _from = edge.first;
            int _to = edge.second;
            // INF + d[to] - d[from] >= (INF + 1) * x[i]
            cplex.addGe(
                    cplex.sum(INF, cplex.diff(d.get(_to), d.get(_from))),
                    cplex.prod(INF + 1, x.get(i))
            );
            cplex.addGe(
                    cplex.sum(INF, cplex.diff(d.get(_from), d.get(_to))),
                    cplex.prod(INF - 1, x.get(i))
            );
        }
    }

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
        for (int i = 0; i < graph.getNodesCount(); i++) {
            System.out.print(varNameOf("q", i) + " = " + cplex.getValue(q.get(i)) + " ; ");
            System.out.print(varNameOf("d", i) + " = " + cplex.getValue(d.get(i)));
            System.out.println();
            //out.println(cplex.getValue(q.get(i)));
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
