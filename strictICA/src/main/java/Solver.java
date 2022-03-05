import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.*;

public class Solver {
    private final IloCplex cplex;
    private final IloNumVar x;
    private final IloNumVar y;

    public Solver() throws IloException {
        this.cplex = new IloCplex();

        // 0.12 * x + 0.15 * y -> min
        // 60 * x 60 * y >= 300
        // 12 * x  + 6 * y >= 36
        // 10 * x + 30 * y >= 90
        // x >= 0, y >= 0

        // variables
        x = cplex.numVar(0, Double.MAX_VALUE, "x");
        y = cplex.numVar(0, Double.MAX_VALUE, "y");

        // expressions
        IloLinearNumExpr objective = cplex.linearNumExpr();
        objective.addTerm(0.12, x);
        objective.addTerm(0.15, y);

        // define objective
        cplex.addMinimize(objective);

        // define constraints
        cplex.addGe(
                cplex.sum(cplex.prod(60, x), cplex.prod(60, y)),
                300
        );
        cplex.addGe(
                cplex.sum(cplex.prod(12, x), cplex.prod(6, y)),
                36
        );
        cplex.addGe(
                cplex.sum(cplex.prod(10, x), cplex.prod(30, y)),
                90
        );
    }

    public boolean solve() throws IloException {
        return cplex.solve();
    }

    public void printResults() throws IloException {
        System.out.println("obj = " + cplex.getObjValue());
        System.out.println("x = " + cplex.getValue(x));
        System.out.println("y = " + cplex.getValue(y));
    }

}
