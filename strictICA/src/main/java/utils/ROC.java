package utils;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import visual.DrawAPI;

import java.util.*;

public class ROC {
    private static boolean eq(Double x, Double y) {
        if (x == null || y == null) {
            throw new RuntimeException("null in equals double!");
        }
        return Math.abs(x - y) < 0.000001;
    }

    public static Pair<List<Pair<Number, Number>>, String> getLine(Double[] predictions, Boolean[] labels) {
        if (predictions.length != labels.length) {
            throw new RuntimeException("invalid data #1 in Roc.draw method");
        }

        int h = 0;
        int w = 0;
        List<Pair<Double, Boolean>> list = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            if (predictions[i] == null || labels[i] == null) {
                throw new RuntimeException("invalid data #2 in Roc.draw method");
            }
            list.add(new Pair<>(predictions[i], labels[i]));
            if (labels[i]) {
                h++;
            } else {
                w++;
            }
        }
        list.sort(Comparator.comparingDouble(x -> x.first));
        Collections.reverse(list);

        double x = 0;
        double y = 0;

        double stepX = 1.0 / (double) w;
        double stepY = 1.0 / (double) h;

        List<Pair<Number, Number>> points = new ArrayList<>();
        points.add(new Pair<>(x, y));

        for (int i = 0; i < list.size(); i++) {
            int cnt = i;
            while ((cnt + 1) < list.size() && eq(list.get(cnt).first, list.get(cnt + 1).first)) {
                cnt++;
            }
            int a = 0;
            int b = 0;
            for (int j = i; j <= cnt; j++) {
                if (list.get(j).second) {
                    a++;
                } else {
                    b++;
                }
            }
            y += stepY * a;
            x += stepX * b;
            points.add(new Pair<>(x, y));
            i = cnt;
        }

        double numerator = 0;
        double denominator = 0;
        for (Pair<Double, Boolean> el1 : list) {
            for (Pair<Double, Boolean> el2 : list) {
                double b = (!el1.second && el2.second) ? 1 : 0;
                double a;
                if (el1.first > el2.first) {
                    a = 0;
                } else {
                    if (el1.first < el2.first) {
                        a = 1;
                    } else {
                        a = 0.5;
                    }
                }
                numerator += b * a;
                denominator += b;
            }
        }

        if (denominator < 0.0001) {
            throw new RuntimeException("divide by zero!");
        }

        return new Pair<>(points, String.format(" %.2f ", numerator / denominator));
    }

    public static void draw(String title, Map<String, Pair<List<Pair<Number, Number>>, String>> lines) {
        Line diagonal = new Line(63, 665, 685, 40);
        diagonal.setStroke(Color.GRAY);

        DrawAPI.Axis xAxisData = new DrawAPI.Axis(
                "False Positive Rate",
                false,
                0.0,
                1.0,
                0.1
        );

        DrawAPI.Axis yAxisData = new DrawAPI.Axis(
                "True Positive Rate",
                false,
                0.0,
                1.0,
                0.1
        );

        DrawAPI.addWindow(title,
                lines,
                xAxisData,
                yAxisData,
                List.of(diagonal)
        );
    }
}
