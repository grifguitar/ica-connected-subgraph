import ilog.concert.*;
import ilog.cplex.*;
import utils.Matrix;

import java.util.ArrayList;
import java.util.List;

public class Solver {

    // constants:

    private final Matrix matrix;

    private final IloCplex cplex;

    private final List<IloNumVar> vars_A;
    private final List<IloNumVar> vars_P;

    public Solver(Matrix matrix) throws IloException {
        this.matrix = matrix;

        this.cplex = new IloCplex();
        //this.cplex.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal);

        this.vars_A = new ArrayList<>();
        this.vars_P = new ArrayList<>();

        addVariables();
        addObjective();
        addConstraint();
    }

    private void addVariables() throws IloException {
        for (int i = 0; i < matrix.numCols(); i++) {
            vars_A.add(cplex.numVar(Float.MIN_VALUE, Float.MAX_VALUE, IloNumVarType.Float, varNameOf("a", i)));
        }
        for (int i = 0; i < matrix.numRows(); i++) {
            vars_P.add(cplex.numVar(Float.MIN_VALUE, Float.MAX_VALUE, IloNumVarType.Float, varNameOf("p", i)));
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
            cplex.addEq(cplex.scalProd(matrix.data[i], toArray(vars_A)), vars_P.get(i));
        }

        IloNumExpr[] l1normP = new IloNumExpr[matrix.numRows()];
        for (int i = 0; i < l1normP.length; i++) {
            l1normP[i] = cplex.abs(vars_P.get(i));
        }
        cplex.addEq(cplex.sum(l1normP), 1);
    }

    public boolean solve() throws IloException {
        return cplex.solve();
    }

    public void printResults() throws IloException {
        System.out.println("obj = " + cplex.getObjValue());
        for (int i = 0; i < vars_A.size(); i++) {
            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(vars_A.get(i)));
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
