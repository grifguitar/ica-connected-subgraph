import ilog.concert.*;
import ilog.cplex.*;
import utils.Matrix;

import java.io.PrintWriter;
import java.util.*;

public class SimpleSolver {

    // constants:

    private final static float INF = 1000;
    private final static int TIME_LIMIT = 100;
    private final static int L1NORM = 250;

    private final Matrix matrix;

    private final IloCplex cplex;

    private final List<IloNumVar> vars_A;
    private final List<IloNumVar> vars_P;

    public SimpleSolver(Matrix matrix) throws IloException {
        this.matrix = matrix;

        this.cplex = new IloCplex();
        this.cplex.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal);
        this.cplex.setParam(IloCplex.Param.TimeLimit, TIME_LIMIT);

        this.vars_A = new ArrayList<>();
        this.vars_P = new ArrayList<>();

        addVariables();
        addObjective();
        addConstraint();
    }

    private void addVariables() throws IloException {
        for (int i = 0; i < matrix.numCols(); i++) {
            vars_A.add(cplex.numVar(-INF, INF, IloNumVarType.Float, varNameOf("a", i)));
        }
        for (int i = 0; i < matrix.numRows(); i++) {
            vars_P.add(cplex.numVar(-INF, INF, IloNumVarType.Float, varNameOf("p", i)));
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
        }

        IloNumExpr[] l1normP = new IloNumExpr[matrix.numRows()];
        for (int i = 0; i < l1normP.length; i++) {
            l1normP[i] = cplex.abs(vars_P.get(i));
        }
        cplex.addEq(cplex.sum(l1normP), L1NORM);
    }

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
