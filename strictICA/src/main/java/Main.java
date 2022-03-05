public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");

        try {
            Solver solver = new Solver();
            if (solver.solve()) {
                solver.printResults();
            } else {
                System.out.println("debug: results not found!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
