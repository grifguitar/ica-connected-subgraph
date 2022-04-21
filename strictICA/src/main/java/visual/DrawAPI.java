package visual;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import utils.Pair;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DrawAPI extends Application {
    private final static List<Window> windows = new ArrayList<>();

    @Override
    public void start(Stage s) {
        for (Window w : windows) {
            w.run();
        }
    }

    public static void run() {
        launch();
    }

    public static void addWindow(
            String title,
            Map<String, Pair<List<Pair<Number, Number>>, String>> lines,
            Axis xAxisData,
            Axis yAxisData,
            List<Node> otherObjects
    ) {
        windows.add(new Window(title, lines, xAxisData, yAxisData, otherObjects));
    }

    public record Axis(
            String name,
            boolean auto,
            Double lowerBound,
            Double upperBound,
            Double step
    ) {
        // nothing
    }

    private record Window(
            String title,
            Map<String, Pair<List<Pair<Number, Number>>, String>> lines,
            Axis xAxisData,
            Axis yAxisData,
            List<Node> otherObjects
    ) {
        public void run() {
            Stage stage = new Stage();
            stage.setTitle("ICA Connected Subgraph");

            NumberAxis xAxis = new NumberAxis();
            xAxis.setLabel(xAxisData.name);
            xAxis.setAutoRanging(xAxisData.auto);
            if (!xAxisData.auto) {
                xAxis.setLowerBound(xAxisData.lowerBound);
                xAxis.setUpperBound(xAxisData.upperBound);
                xAxis.setTickUnit(xAxisData.step);
            }

            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel(yAxisData.name);
            yAxis.setAutoRanging(yAxisData.auto);
            if (!yAxisData.auto) {
                yAxis.setLowerBound(yAxisData.lowerBound);
                yAxis.setUpperBound(yAxisData.upperBound);
                yAxis.setTickUnit(yAxisData.step);
            }

            LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);

            lineChart.setTitle(title);
            lineChart.setCreateSymbols(false);

            for (String name : lines.keySet()) {
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(name + lines.get(name).second);
                for (Pair<Number, Number> point : lines.get(name).first) {
                    series.getData().add(new XYChart.Data<>(point.first, point.second));
                }
                lineChart.getData().add(series);
            }

            lineChart.setMinSize(700, 750);
            lineChart.setMaxSize(700, 750);

            Group group = new Group(lineChart);

            if (otherObjects != null) {
                group.getChildren().addAll(otherObjects);
            }

            Scene scene = new Scene(group, 900, 900);

            stage.setScene(scene);
            saveToFile(
                    scene,
                    "./graphics/j_" + title.replaceAll("\\s", "_") + ".png"
            );
            stage.show();
        }
    }

    public static void saveToFile(Scene scene, String path) {
        WritableImage image = scene.snapshot(null);
        File file = new File(path);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
