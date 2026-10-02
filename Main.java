import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * AeroMech visual redesign.
 *
 * This class intentionally keeps the existing physics backend intact:
 * Aircraft, Atmosphere, Simulation, Optimizer, etc. remain separate.
 * Main.java is responsible for presentation, controls and visualization.
 */
public class Main extends Application {

    // ------------------------------------------------------------------
    // DEFAULTS
    // ------------------------------------------------------------------
    private static final double DEFAULT_MASS = 5000.0;
    private static final double DEFAULT_WING_AREA = 16.2;
    private static final double DEFAULT_VELOCITY = 77.22;
    private static final double DEFAULT_ALTITUDE = 0.0;
    private static final double DEFAULT_CL = 0.828729;
    private static final double DEFAULT_CD0 = 0.025;
    private static final double DEFAULT_K = 0.036401111111111085;
    private static final double DEFAULT_THRUST = 10000.0;
    private static final double DEFAULT_EXHAUST_VELOCITY = 2500.0;
    private static final double DEFAULT_TIME_STEP = 0.10;
    private static final double DEFAULT_SIMULATION_TIME = 120.0;
    private static final double GRAVITY = 9.80665;
    private static final double SPEED_OF_SOUND = 340.3;
    private static final double VISUAL_MAX_ALTITUDE = 11000.0;

    // ------------------------------------------------------------------
    // ROOT / HEADER
    // ------------------------------------------------------------------
    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();

    private Button simulationNav;
    private Button analysisNav;
    private Label statusLabel;
    private Label footerLabel;

    // ------------------------------------------------------------------
    // CONTROLS
    // ------------------------------------------------------------------
    private ComboBox<String> presetBox;
    private Slider airspeedSlider;
    private Slider altitudeSlider;
    private Slider thrustSlider;
    private Slider wingAreaSlider;
    private Slider clSlider;
    private Slider cd0Slider;
    private Slider kSlider;
    private Slider timeStepSlider;
    private Slider simulationTimeSlider;
    private Slider playbackSpeedSlider;

    private Label airspeedValue;
    private Label altitudeValue;
    private Label thrustValue;
    private Label wingAreaValue;
    private Label clValue;
    private Label cd0Value;
    private Label kValue;
    private Label timeStepValue;
    private Label simulationTimeValue;
    private Label playbackSpeedValue;

    private boolean applyingPreset;
    private boolean autoLandingAltitude = false;
    private boolean needsReset = true;
    private String selectedFlightProfile = "Custom";

    // ------------------------------------------------------------------
    // VIEWPORT
    // ------------------------------------------------------------------
    private PaneProxy flightViewport;
    private Group aircraftGraphic;
    private Polyline trajectory;
    private Polyline intendedPath;
    private final List<Button> durationButtons = new ArrayList<>();

    private Label flightPathLabel;
    private Label altitudeHud;
    private Label rangeHud;
    private Label velocityHud;
    private Label cameraHud;

    private final List<Line> airflowLines = new ArrayList<>();
    private final List<Line> groundLines = new ArrayList<>();
    private final List<Circle> clouds = new ArrayList<>();

    // ------------------------------------------------------------------
    // TELEMETRY
    // ------------------------------------------------------------------
    private Label timeValue;
    private Label distanceValue;
    private Label velocityTelemetry;
    private Label altitudeTelemetry;
    private Label accelerationValue;
    private Label liftTelemetry;
    private Label dragTelemetry;
    private Label thrustTelemetry;
    private Label weightTelemetry;
    private Label twValue;
    private Label ldValue;
    private Label flightAngleValue;
    private Label dynamicPressureValue;
    private Label densityValue;
    private Label machValue;

    // ------------------------------------------------------------------
    // CHARTS
    // ------------------------------------------------------------------
    private LineChart<Number, Number> velocityChart;
    private LineChart<Number, Number> altitudeChart;
    private XYChart.Series<Number, Number> velocitySeries;
    private XYChart.Series<Number, Number> altitudeSeries;
    private double lastChartTime = -1.0;

    // ------------------------------------------------------------------
    // BACKEND / ANIMATION
    // ------------------------------------------------------------------
    private Simulation simulation;
    private AnimationTimer animationTimer;
    private boolean running;
    private boolean finished;
    private long lastFrameNanos = -1L;
    private double timeAccumulator;

    // ------------------------------------------------------------------
    // ANALYSIS
    // ------------------------------------------------------------------
    private javafx.scene.control.TextField massField;
    private javafx.scene.control.TextField wingAreaField;
    private javafx.scene.control.TextField velocityField;
    private javafx.scene.control.TextField altitudeField;
    private javafx.scene.control.TextField clField;
    private javafx.scene.control.TextField cd0Field;
    private javafx.scene.control.TextField kField;
    private javafx.scene.control.TextField thrustField;
    private javafx.scene.control.TextField exhaustField;
    private javafx.scene.control.TextArea analysisOutput;

    @Override
    public void start(Stage stage) {
        root.setTop(buildHeader());
        root.setCenter(content);
        root.setBottom(buildFooter());

        showSimulation();

        Scene scene = new Scene(root, 1600, 950);
        scene.setFill(Color.web("#050b13"));

        stage.setTitle("AeroMech - Aerospace Engineering Simulator");
        stage.setMinWidth(1250);
        stage.setMinHeight(780);
        stage.setScene(scene);
        stage.show();

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!running || simulation == null) {
                    lastFrameNanos = now;
                    return;
                }

                if (lastFrameNanos < 0) {
                    lastFrameNanos = now;
                    return;
                }

                double frameSeconds = (now - lastFrameNanos) / 1_000_000_000.0;
                lastFrameNanos = now;
                frameSeconds = Math.min(frameSeconds, 0.10);
                frameSeconds *= playbackSpeedSlider == null ? 1.0 : playbackSpeedSlider.getValue();
                timeAccumulator += frameSeconds;

                double dt = simulation.getTimeStep();
                int maxSteps = 60;
                int steps = 0;

                while (timeAccumulator >= dt && steps < maxSteps && !simulation.isComplete()) {
                    simulation.step();
                    timeAccumulator -= dt;
                    steps++;
                }

                refreshSimulationUI();

                if (simulation.isComplete()) {
                    running = false;
                    finished = true;
                    statusLabel.setText("* SIMULATION COMPLETE");
                    statusLabel.setTextFill(Color.web("#71e0a0"));
                    footerLabel.setText("PHYSICS ENGINE   * COMPLETE   STANDARD ATMOSPHERE   MODEL STATE PRESERVED");
                }
            }
        };

        animationTimer.start();
    }

    @Override
    public void stop() {
        running = false;
    }

    // ==================================================================
    // HEADER / FOOTER
    // ==================================================================

    private Node buildHeader() {
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(11, 18, 11, 18));
        header.setStyle("-fx-background-color:#07121f;-fx-border-color:#17364f;-fx-border-width:0 0 1 0;");

        Label title = new Label("AEROMECH");
        title.setTextFill(Color.web("#eef7ff"));
        title.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 21));

        Label subtitle = new Label("AEROSPACE ENGINEERING SIMULATOR");
        subtitle.setTextFill(Color.web("#5d829f"));
        subtitle.setFont(Font.font("System", FontWeight.BOLD, 9));

        VBox brand = new VBox(1, title, subtitle);
        brand.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        simulationNav = headerButton("SIMULATION");
        analysisNav = headerButton("ANALYSIS");
        simulationNav.setOnAction(e -> showSimulation());
        analysisNav.setOnAction(e -> showAnalysis());

        statusLabel = new Label("* READY");
        statusLabel.setTextFill(Color.web("#71e0a0"));
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 9));

        header.getChildren().addAll(brand, spacer, simulationNav, analysisNav,
                new Separator(Orientation.VERTICAL), statusLabel);
        return header;
    }

    private Button headerButton(String text) {
        Button b = new Button(text);
        b.setPadding(new Insets(7, 13, 7, 13));
        b.setFont(Font.font("System", FontWeight.BOLD, 9));
        b.setTextFill(Color.web("#90aac0"));
        b.setCursor(Cursor.HAND);
        b.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-background-radius:6;");
        return b;
    }

    private void setActiveNav(Button active) {
        styleNav(simulationNav, active == simulationNav);
        styleNav(analysisNav, active == analysisNav);
    }

    private void styleNav(Button b, boolean active) {
        if (active) {
            b.setStyle("-fx-background-color:#0d2a42;-fx-border-color:#2d7caf;-fx-border-radius:6;-fx-background-radius:6;");
            b.setTextFill(Color.web("#e2f4ff"));
        } else {
            b.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-background-radius:6;");
            b.setTextFill(Color.web("#90aac0"));
        }
    }

    private Node buildFooter() {
        footerLabel = new Label("PHYSICS ENGINE   * READY   STANDARD ATMOSPHERE");
        footerLabel.setTextFill(Color.web("#4f718d"));
        footerLabel.setFont(Font.font("System", FontWeight.MEDIUM, 8.5));
        footerLabel.setPadding(new Insets(6, 18, 7, 18));
        footerLabel.setStyle("-fx-background-color:#050c14;-fx-border-color:#17344c;-fx-border-width:1 0 0 0;");
        return footerLabel;
    }

    // ==================================================================
    // SIMULATION PAGE
    // ==================================================================

    private void showSimulation() {
        running = false;
        finished = false;
        needsReset = true;
        setActiveNav(simulationNav);
        content.getChildren().setAll(buildSimulationPage());
        rebuildSimulation();
    }

    private Node buildSimulationPage() {
        BorderPane page = new BorderPane();
        page.setPadding(new Insets(7));
        page.setStyle("-fx-background-color:#050b13;");

        VBox left = buildControlRail();
        left.setPrefWidth(275);
        left.setMinWidth(250);
        left.setMaxWidth(300);

        flightViewport = new PaneProxy(1000, 600);
        flightViewport.setMinWidth(560);
        flightViewport.setMinHeight(430);
        flightViewport.setStyle("-fx-background-color:#071a2d;-fx-background-radius:10;-fx-border-color:#1b3e59;-fx-border-radius:10;");
        buildFlightScene(flightViewport);

        VBox right = buildTelemetryRail();
        right.setPrefWidth(255);
        right.setMinWidth(230);
        right.setMaxWidth(280);

        HBox charts = buildCharts();
        charts.setPrefHeight(190);
        charts.setMinHeight(170);

        page.setLeft(left);
        page.setCenter(flightViewport);
        page.setRight(right);
        page.setBottom(charts);

        BorderPane.setMargin(left, new Insets(0, 7, 6, 0));
        BorderPane.setMargin(flightViewport, new Insets(0, 7, 6, 0));
        BorderPane.setMargin(right, new Insets(0, 0, 6, 0));
        return page;
    }

    // ==================================================================
    // FLIGHT VIEW
    // ==================================================================

    private void buildFlightScene(PaneProxy pane) {
        pane.clear();
        airflowLines.clear();
        groundLines.clear();
        clouds.clear();

        Rectangle sky = new Rectangle();
        sky.widthProperty().bind(pane.widthProperty());
        sky.heightProperty().bind(pane.heightProperty());
        sky.setFill(new LinearGradient(0, 0, 0, 1, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#061324")),
                new Stop(0.62, Color.web("#0b2740")),
                new Stop(1.00, Color.web("#173a50"))));

        Rectangle ground = new Rectangle();
        ground.widthProperty().bind(pane.widthProperty());
        ground.heightProperty().bind(pane.heightProperty().multiply(0.20));
        ground.yProperty().bind(pane.heightProperty().multiply(0.80));
        ground.setFill(Color.web("#11232f"));

        Line horizon = new Line();
        horizon.endXProperty().bind(pane.widthProperty());
        horizon.startYProperty().bind(pane.heightProperty().multiply(0.80));
        horizon.endYProperty().bind(pane.heightProperty().multiply(0.80));
        horizon.setStroke(Color.web("#35576d"));
        horizon.setStrokeWidth(1.2);

        Circle sun = new Circle(30, Color.web("#d5ab61", 0.65));
        sun.centerXProperty().bind(pane.widthProperty().multiply(0.78));
        sun.centerYProperty().bind(pane.heightProperty().multiply(0.17));

        for (int i = 0; i < 30; i++) {
            Line line = new Line();
            line.setStroke(i % 4 == 0 ? Color.web("#5bcff4", 0.34) : Color.web("#6d9fbd", 0.18));
            line.setStrokeWidth(i % 5 == 0 ? 1.5 : 1.0);
            double y = 75 + (i * 29) % 360;
            double x = 20 + (i * 83) % 950;
            line.setStartX(x);
            line.setEndX(x + 75 + (i % 4) * 25);
            line.setStartY(y);
            line.setEndY(y);
            airflowLines.add(line);
        }

        for (int i = 0; i < 20; i++) {
            Line line = new Line();
            line.setStroke(Color.web("#406274", 0.42));
            line.setStrokeWidth(1.0);
            groundLines.add(line);
        }

        // Soft distant cloud groups.
        for (int i = 0; i < 8; i++) {
            Circle c = new Circle(18 + (i % 3) * 7, Color.web("#d6e4ed", 0.08));
            c.setCenterX(80 + i * 150);
            c.setCenterY(120 + (i % 4) * 62);
            clouds.add(c);
        }

        // Solid line = path already flown.
        trajectory = new Polyline();
        trajectory.setFill(Color.TRANSPARENT);
        trajectory.setStroke(Color.web("#67d8f4", 0.85));
        trajectory.setStrokeWidth(2.2);

        // Dashed curve = projected / intended path ahead of the aircraft.
        intendedPath = new Polyline();
        intendedPath.setFill(Color.TRANSPARENT);
        intendedPath.setStroke(Color.web("#e6c86e", 0.90));
        intendedPath.setStrokeWidth(1.8);
        intendedPath.getStrokeDashArray().addAll(5.0, 8.0);

        aircraftGraphic = buildAircraftGraphic();

        Label title = viewportLabel("LIVE FLIGHT DYNAMICS", 16, 14, Color.web("#7196b2"));
        flightPathLabel = viewportLabel("FLIGHT PATH +0.0 deg  /  DASHED = PROJECTED", 16, 34, Color.web("#72e1a1"));
        cameraHud = viewportLabel("CAMERA TRACKING", 0, 14, Color.web("#63869f"));
        cameraHud.layoutXProperty().bind(pane.widthProperty().subtract(150));

        altitudeHud = viewportLabel("ALTITUDE 0 m", 0, 40, Color.web("#a7def4"));
        altitudeHud.layoutXProperty().bind(pane.widthProperty().subtract(165));

        rangeHud = viewportLabel("RANGE 0 m", 0, 61, Color.web("#a7def4"));
        rangeHud.layoutXProperty().bind(pane.widthProperty().subtract(165));

        velocityHud = viewportLabel("V 0.0 m/s", 0, 82, Color.web("#a7def4"));
        velocityHud.layoutXProperty().bind(pane.widthProperty().subtract(165));

        pane.addAll(sky, ground, horizon, sun);
        pane.getChildren().addAll(clouds);
        pane.getChildren().addAll(airflowLines);
        pane.getChildren().addAll(groundLines);
        pane.addAll(trajectory, intendedPath,
                aircraftGraphic, title, flightPathLabel, cameraHud, altitudeHud, rangeHud, velocityHud);

        pane.widthProperty().addListener((obs, o, n) -> updateWorldState());
        pane.heightProperty().addListener((obs, o, n) -> updateWorldState());
    }

    private Label viewportLabel(String text, double x, double y, Color color) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(Font.font("System", FontWeight.BOLD, 9));
        label.setLayoutX(x);
        label.setLayoutY(y);
        return label;
    }

    private Group buildAircraftGraphic() {
        // Sleek right-facing jet silhouette. The aircraft is deliberately
        // simple enough to remain crisp during animation and rotation.
        Polygon fuselage = new Polygon(
                -64, -6,
                -28, -8,
                4, -6,
                34, -4,
                58, -1.5,
                68, 0,
                58, 1.5,
                34, 4,
                4, 6,
                -28, 8,
                -64, 6,
                -72, 2.2,
                -72, -2.2
        );
        fuselage.setFill(Color.web("#edf4f8"));
        fuselage.setStroke(Color.web("#839baa"));
        fuselage.setStrokeWidth(1.1);

        Polygon mainWingTop = new Polygon(
                -8, -3,
                17, -34,
                42, -34,
                25, -3
        );
        mainWingTop.setFill(Color.web("#cbd9e1"));

        Polygon mainWingBottom = new Polygon(
                -8, 3,
                25, 3,
                42, 34,
                17, 34
        );
        mainWingBottom.setFill(Color.web("#b7c9d4"));

        Polygon tailTop = new Polygon(
                -39, -3,
                -54, -19,
                -41, -19,
                -28, -3
        );
        tailTop.setFill(Color.web("#b6c7d2"));

        Polygon tailBottom = new Polygon(
                -39, 3,
                -28, 3,
                -41, 19,
                -54, 19
        );
        tailBottom.setFill(Color.web("#a9bdc9"));

        Polygon fin = new Polygon(
                -43, -4,
                -54, -25,
                -43, -25,
                -32, -4
        );
        fin.setFill(Color.web("#9caeb9"));

        Polygon canopy = new Polygon(
                20, -4,
                31, -10,
                46, -8,
                52, -2,
                28, -2
        );
        canopy.setFill(Color.web("#466f89"));
        canopy.setStroke(Color.web("#8db8cf"));
        canopy.setStrokeWidth(0.8);

        Polygon intakeTop = new Polygon(
                -8, -7,
                10, -13,
                22, -12,
                12, -7
        );
        intakeTop.setFill(Color.web("#6e8490"));

        Polygon intakeBottom = new Polygon(
                -8, 7,
                12, 7,
                22, 12,
                10, 13
        );
        intakeBottom.setFill(Color.web("#627985"));

        Circle engineGlowTop = new Circle(-27, -9, 3.2, Color.web("#64d6f2", 0.72));
        Circle engineGlowBottom = new Circle(-27, 9, 3.2, Color.web("#64d6f2", 0.72));

        Polygon exhaustTop = new Polygon(
                -49, -6,
                -67, -3,
                -49, -1
        );
        exhaustTop.setFill(Color.web("#54c9ea", 0.45));

        Polygon exhaustBottom = new Polygon(
                -49, 1,
                -67, 3,
                -49, 6
        );
        exhaustBottom.setFill(Color.web("#54c9ea", 0.45));

        Group aircraft = new Group(
                exhaustTop, exhaustBottom,
                mainWingTop, mainWingBottom,
                tailTop, tailBottom, fin,
                intakeTop, intakeBottom,
                fuselage, canopy,
                engineGlowTop, engineGlowBottom
        );
        aircraft.setScaleX(1.12);
        aircraft.setScaleY(1.12);
        return aircraft;
    }

    // ==================================================================
    // WORLD MOVEMENT / AIRCRAFT MOTION
    // ==================================================================

    private void updateWorldState() {
        if (flightViewport == null || simulation == null || aircraftGraphic == null
                || trajectory == null || intendedPath == null) {
            return;
        }

        Simulation.State state = simulation.getState();
        double width = Math.max(flightViewport.getWidth(), 800);
        double height = Math.max(flightViewport.getHeight(), 500);

        // Horizontal camera follow. The aircraft moves initially, then the
        // camera tracks it while the world scrolls underneath.
        final double followDistance = 300.0;
        final double metersPerPixel = 1.65;
        double x;

        if (state.getDistance() < followDistance) {
            x = 125 + state.getDistance() / metersPerPixel;
        } else {
            x = width * 0.58;
        }

        // IMPORTANT: Simulation.State altitude is absolute altitude.
        // Larger physical altitude must therefore map to smaller JavaFX Y.
        double y = altitudeToScreenY(state.getAltitude(), height);

        aircraftGraphic.setLayoutX(x);
        aircraftGraphic.setLayoutY(y);

        // JavaFX positive rotation is clockwise. A positive flight-path
        // angle is a climb, so the nose must rotate counter-clockwise.
        aircraftGraphic.setRotate(-Math.toDegrees(state.getFlightPathAngle()));

        altitudeHud.setText("ALTITUDE  " + format(state.getAltitude(), 0) + " m");
        rangeHud.setText("RANGE  " + format(state.getDistance(), 0) + " m");
        velocityHud.setText("V  " + format(state.getVelocity(), 1) + " m/s");
        flightPathLabel.setText("FLIGHT PATH  "
                + format(Math.toDegrees(state.getFlightPathAngle()), 1)
                + " deg  /  DASHED = PROJECTED");

        // Actual path already travelled.
        trajectory.getPoints().addAll(x, y);
        while (trajectory.getPoints().size() > 1800) {
            trajectory.getPoints().remove(0);
            trajectory.getPoints().remove(0);
        }

        // Project the intended path using the same flight-profile logic
        // as the physics backend. This keeps the dashed prediction responsive
        // to the selected profile, airspeed, thrust and altitude.
        updateIntendedPath(state, x, y, width, height, metersPerPixel);

        double worldOffset = Math.max(0, state.getDistance() - followDistance) / metersPerPixel;
        updateAirflow(worldOffset, width);
        updateGround(worldOffset, width);
        updateClouds(worldOffset, width);
    }

    private double altitudeToScreenY(double altitude, double height) {
        double groundY = height * 0.80;
        double ceilingY = Math.max(78.0, height * 0.14);

        double normalized = clamp(altitude / VISUAL_MAX_ALTITUDE, 0.0, 1.0);
        double y = groundY - 26.0
                - normalized * ((groundY - 26.0) - ceilingY);

        return clamp(y, ceilingY, groundY - 26.0);
    }

    private void updateIntendedPath(
            Simulation.State state,
            double x,
            double y,
            double width,
            double height,
            double horizontalMetersPerPixel) {

        intendedPath.getPoints().clear();

        double remaining = Math.max(0.0, simulation.getSimulationTime() - state.getTime());
        double horizonSeconds = Math.min(18.0, remaining);

        if (horizonSeconds <= 0.01) {
            return;
        }

        int samples = 42;
        double dt = horizonSeconds / samples;
        double px = 0.0;
        double futureAltitude = state.getAltitude();
        double velocity = Math.max(state.getVelocity(), 0.1);
        double previousGamma = state.getFlightPathAngle();
        double altitudePixelsPerMeter =
                (height * 0.80 - 26.0 - Math.max(78.0, height * 0.14))
                        / VISUAL_MAX_ALTITUDE;

        intendedPath.getPoints().addAll(x, y);

        for (int i = 1; i <= samples; i++) {
            double futureTime = state.getTime() + i * dt;
            double gamma = simulation.getProjectedFlightPathAngle(
                    futureTime,
                    velocity,
                    futureAltitude
            );

            double averageGamma = 0.5 * (previousGamma + gamma);
            px += velocity * Math.cos(averageGamma) * dt;
            futureAltitude += velocity * Math.sin(averageGamma) * dt;
            previousGamma = gamma;

            double sx = x + px / horizontalMetersPerPixel;
            double sy = altitudeToScreenY(futureAltitude, height);

            if (sx > width + 40 || sy < 68 || sy > height * 0.80) {
                if (sx > width + 40) break;
                if (sy > height * 0.80) break;
            }

            intendedPath.getPoints().addAll(sx, sy);
        }
    }

    private void updateAirflow(double offset, double width) {
        for (int i = 0; i < airflowLines.size(); i++) {
            Line line = airflowLines.get(i);
            double span = line.getEndX() - line.getStartX();
            double base = i * 97.0 + 25.0;
            double x = base - (offset * 0.75 % (width + 180));
            while (x < -140) x += width + 180;
            line.setStartX(x);
            line.setEndX(x + span);
        }
    }

    private void updateGround(double offset, double width) {
        double spacing = Math.max(70, width / 9.0);
        for (int i = 0; i < groundLines.size(); i++) {
            Line line = groundLines.get(i);
            double x = 20 + i * spacing - (offset % spacing);
            while (x < -80) x += spacing * groundLines.size();
            line.setStartX(x);
            line.setEndX(x + 42);
            line.setStartY(flightViewport.getHeight() * 0.90);
            line.setEndY(flightViewport.getHeight() * 0.90);
        }
    }

    private void updateClouds(double offset, double width) {
        for (int i = 0; i < clouds.size(); i++) {
            Circle cloud = clouds.get(i);
            double baseX = 80 + i * 150;
            double x = baseX - (offset * 0.20 % (width + 300));
            while (x < -100) x += width + 300;
            cloud.setCenterX(x);
        }
    }

    private void resetAircraftVisual() {
        if (aircraftGraphic == null || trajectory == null || intendedPath == null
                || simulation == null) return;

        double width = Math.max(flightViewport.getWidth(), 800);
        double height = Math.max(flightViewport.getHeight(), 500);
        Simulation.State state = simulation.getState();

        double x = 125.0;
        double y = altitudeToScreenY(state.getAltitude(), height);

        aircraftGraphic.setLayoutX(x);
        aircraftGraphic.setLayoutY(y);
        aircraftGraphic.setRotate(-Math.toDegrees(state.getFlightPathAngle()));

        trajectory.getPoints().clear();
        trajectory.getPoints().addAll(x - 26.0, y, x, y);
        intendedPath.getPoints().clear();
        intendedPath.getPoints().addAll(x, y);

        cameraHud.setText("CAMERA TRACKING");
        updateIntendedPath(
                state,
                x,
                y,
                width,
                height,
                1.65
        );
    }

    // ==================================================================
    // CONTROL RAIL
    // ==================================================================

    private VBox buildControlRail() {
        VBox outer = new VBox(6);
        outer.setPadding(new Insets(8));
        outer.setStyle("-fx-background-color:#07131f;-fx-background-radius:10;-fx-border-color:#18374e;-fx-border-radius:10;");

        VBox body = new VBox(7);

        Label heading = new Label("FLIGHT CONFIGURATION");
        heading.setTextFill(Color.web("#dceefa"));
        heading.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 11));

        Label sub = new Label("Choose a flight profile, then freely tune the inputs and observe the response.");
        sub.setWrapText(true);
        sub.setTextFill(Color.web("#547590"));
        sub.setFont(Font.font("System", 8.5));

        presetBox = new ComboBox<>();
        presetBox.getItems().addAll("Custom", "Take-off", "Landing", "Cruise", "High Altitude");
        presetBox.setValue("Custom");
        presetBox.setMaxWidth(Double.MAX_VALUE);
        presetBox.setTooltip(new Tooltip("Flight profile. Input sliders remain editable after selecting a profile."));
        presetBox.setOnAction(e -> applyPreset(presetBox.getValue()));
        presetBox.setStyle("-fx-background-color:#0a1b2a;-fx-border-color:#264861;-fx-border-radius:6;");

        airspeedSlider = slider(20, 250, DEFAULT_VELOCITY);
        altitudeSlider = slider(0, 11000, DEFAULT_ALTITUDE);
        thrustSlider = slider(3000, 30000, DEFAULT_THRUST);
        wingAreaSlider = slider(8, 30, DEFAULT_WING_AREA);
        clSlider = slider(0.20, 1.40, DEFAULT_CL);
        cd0Slider = slider(0.010, 0.080, DEFAULT_CD0);
        kSlider = slider(0.015, 0.080, DEFAULT_K);
        timeStepSlider = slider(0.02, 0.30, DEFAULT_TIME_STEP);
        simulationTimeSlider = slider(30, 300, DEFAULT_SIMULATION_TIME);
        simulationTimeSlider.setMajorTickUnit(30);
        simulationTimeSlider.setMinorTickCount(0);
        simulationTimeSlider.setSnapToTicks(true);
        playbackSpeedSlider = slider(0.25, 8.0, 1.0);

        airspeedValue = valueLabel();
        altitudeValue = valueLabel();
        thrustValue = valueLabel();
        wingAreaValue = valueLabel();
        clValue = valueLabel();
        cd0Value = valueLabel();
        kValue = valueLabel();
        timeStepValue = valueLabel();
        simulationTimeValue = valueLabel();
        playbackSpeedValue = valueLabel();

        airspeedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            airspeedValue.setText(format(newV.doubleValue(), 1) + " m/s");
            if (!applyingPreset
                    && "Landing".equals(selectedFlightProfile)
                    && autoLandingAltitude) {
                setLandingAltitudeForDuration(simulationTimeSlider.getValue());
            }
            if (!applyingPreset) {
                needsReset = true;
            }
        });
        altitudeSlider.valueProperty().addListener((obs, oldV, newV) -> {
            altitudeValue.setText(format(newV.doubleValue(),0) + " m");
            if (!applyingPreset) {
                autoLandingAltitude = false;
                needsReset = true;
            }
        });
        listener(thrustSlider, thrustValue, v -> format(v/1000,2) + " kN");
        listener(wingAreaSlider, wingAreaValue, v -> format(v,2) + " m^2");
        listener(clSlider, clValue, v -> format(v,3));
        listener(cd0Slider, cd0Value, v -> format(v,3));
        listener(kSlider, kValue, v -> format(v,4));
        listener(timeStepSlider, timeStepValue, v -> format(v,2) + " s");
        simulationTimeSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double snapped = Math.round(newV.doubleValue() / 30.0) * 30.0;
            snapped = Math.max(30.0, Math.min(300.0, snapped));
            if (Math.abs(newV.doubleValue() - snapped) > 0.001) {
                simulationTimeSlider.setValue(snapped);
            }
            simulationTimeValue.setText(formatSimulationTime(snapped));
            setActiveDurationButton((int)Math.round(snapped));

            if (!applyingPreset
                    && "Landing".equals(selectedFlightProfile)
                    && autoLandingAltitude) {
                setLandingAltitudeForDuration(snapped);
            }

            if (!applyingPreset) {
                needsReset = true;
            }
        });
        listener(playbackSpeedSlider, playbackSpeedValue, v -> format(v,1) + "x");

        VBox primary = new VBox(2,
                sliderRow("Airspeed", airspeedSlider, airspeedValue, "Initial true airspeed."),
                sliderRow("Altitude", altitudeSlider, altitudeValue, "Initial altitude used by the standard atmosphere model."),
                sliderRow("Thrust", thrustSlider, thrustValue, "Propulsive force. Increasing it can increase acceleration."),
                sliderRow("Wing area", wingAreaSlider, wingAreaValue, "Reference wing area used in lift and drag calculations.")
        );

        VBox aero = new VBox(2,
                sliderRow("Lift coefficient CL", clSlider, clValue, "Dimensionless coefficient controlling lift."),
                sliderRow("Zero-lift CD0", cd0Slider, cd0Value, "Baseline drag coefficient."),
                sliderRow("Induced factor k", kSlider, kValue, "Controls the induced-drag term in the drag polar.")
        );

        HBox durationButtons = new HBox(4);
        durationButtons.setAlignment(Pos.CENTER_LEFT);
        durationButtons.getChildren().addAll(
                durationPresetButton("30s", 30),
                durationPresetButton("1m", 60),
                durationPresetButton("2m", 120),
                durationPresetButton("3m", 180),
                durationPresetButton("4m", 240),
                durationPresetButton("5m", 300)
        );

        VBox timing = new VBox(2,
                sliderRow("Time step", timeStepSlider, timeStepValue, "Numerical integration interval."),
                sliderRow("Duration", simulationTimeSlider, simulationTimeValue, "30 s to 5 min. Use the slider or a preset button."),
                durationButtons,
                sliderRow("Playback", playbackSpeedSlider, playbackSpeedValue, "Changes display speed, not physics time step.")
        );

        setActiveDurationButton((int)Math.round(simulationTimeSlider.getValue()));

        Button play = controlButton("> RUN", "#123b31", "#7be4a5");
        Button pause = controlButton("| PAUSE", "#3a3220", "#e8c869");
        Button reset = controlButton("R RESET", "#172b3d", "#9ed3f4");
        play.setOnAction(e -> playSimulation());
        pause.setOnAction(e -> pauseSimulation());
        reset.setOnAction(e -> rebuildSimulation());

        HBox buttons = new HBox(5, play, pause, reset);
        buttons.setAlignment(Pos.CENTER);

        Label hint = new Label("Profile sets the flight behavior; your airspeed, thrust, altitude and aerodynamic inputs remain active.");
        hint.setWrapText(true);
        hint.setTextFill(Color.web("#55748d"));
        hint.setFont(Font.font("System", 8.3));

        body.getChildren().addAll(
                heading, sub,
                section("SCENARIO PRESET"), presetBox,
                section("PRIMARY FLIGHT INPUTS"), primary,
                separator(),
                section("AERODYNAMIC MODEL"), aero,
                section("SIMULATION SETTINGS"), timing,
                buttons, hint
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        outer.getChildren().add(scroll);
        return outer;
    }

    private Label section(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#5f819b"));
        label.setFont(Font.font("System", FontWeight.BOLD, 8.2));
        return label;
    }

    private Separator separator() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color:#1b3a50;");
        return s;
    }

    private Button durationPresetButton(String text, int seconds) {
        Button b = new Button(text);
        b.setPrefWidth(38);
        b.setPadding(new Insets(6, 3, 6, 3));
        b.setCursor(Cursor.HAND);
        b.setFont(Font.font("System", FontWeight.BOLD, 7.8));
        b.setOnAction(e -> setSimulationDuration(seconds));
        durationButtons.add(b);
        styleDurationButton(b, false);
        return b;
    }

    private void setSimulationDuration(int seconds) {
        seconds = Math.max(30, Math.min(300, seconds));
        applyingPreset = true;
        simulationTimeSlider.setValue(seconds);
        applyingPreset = false;

        simulationTimeValue.setText(formatSimulationTime(seconds));
        setActiveDurationButton(seconds);

        if ("Landing".equals(selectedFlightProfile)
                && autoLandingAltitude) {
            setLandingAltitudeForDuration(seconds);
        }

        needsReset = true;
        rebuildSimulation();
    }

    private void setLandingAltitudeForDuration(double seconds) {
        double speed = Math.max(airspeedSlider.getValue(), 20.0);
        double approachAngle = Math.toRadians(4.0);
        double flareTime = 12.0;
        double transitionReserve = 3.0;
        double usableDescentTime =
                Math.max(8.0, seconds - flareTime - transitionReserve);

        // Choose an altitude that can be reached with a shallow, stable
        // approach within the selected duration. The altitude is derived
        // from the user's current airspeed, so changing airspeed genuinely
        // changes the landing setup.
        double recommendedAltitude =
                speed
                * Math.sin(approachAngle)
                * usableDescentTime;

        recommendedAltitude = clamp(
                recommendedAltitude,
                70.0,
                6000.0
        );

        applyingPreset = true;
        altitudeSlider.setValue(recommendedAltitude);
        applyingPreset = false;
        altitudeValue.setText(format(recommendedAltitude, 0) + " m");
    }

    private void setActiveDurationButton(int seconds) {
        int[] values = {30, 60, 120, 180, 240, 300};
        for (int i = 0; i < durationButtons.size() && i < values.length; i++) {
            styleDurationButton(durationButtons.get(i), values[i] == seconds);
        }
    }

    private void styleDurationButton(Button b, boolean active) {
        if (active) {
            b.setStyle("-fx-background-color:#154565;-fx-border-color:#4cb5df;-fx-border-radius:5;-fx-background-radius:5;");
            b.setTextFill(Color.web("#e7f8ff"));
        } else {
            b.setStyle("-fx-background-color:#0b1e2e;-fx-border-color:#28465c;-fx-border-radius:5;-fx-background-radius:5;");
            b.setTextFill(Color.web("#8aa8bd"));
        }
    }

    private Slider slider(double min, double max, double value) {
        Slider s = new Slider(min, max, value);
        s.setMaxWidth(Double.MAX_VALUE);
        s.setShowTickMarks(false);
        s.setShowTickLabels(false);
        s.setBlockIncrement((max - min) / 50.0);
        return s;
    }

    private void listener(Slider slider, Label label, Formatter f) {
        slider.valueProperty().addListener((obs, oldV, newV) -> {
            label.setText(f.format(newV.doubleValue()));
            if (!applyingPreset) {
                needsReset = true;
            }
        });
    }

    @FunctionalInterface
    private interface Formatter { String format(double v); }

    private HBox sliderRow(String name, Slider slider, Label value, String tip) {
        Label label = new Label(name);
        label.setMinWidth(92);
        label.setTextFill(Color.web("#7996ab"));
        label.setFont(Font.font("System", FontWeight.BOLD, 8.3));
        Tooltip tooltip = new Tooltip(tip);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(260);
        Tooltip.install(label, tooltip);
        Tooltip.install(slider, tooltip);

        value.setMinWidth(59);
        value.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(slider, Priority.ALWAYS);
        HBox row = new HBox(6, label, slider, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label valueLabel() {
        Label l = new Label("-");
        l.setTextFill(Color.web("#dcecff"));
        l.setFont(Font.font("System", FontWeight.BOLD, 8.8));
        return l;
    }

    private Button controlButton(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setPrefWidth(79);
        b.setPadding(new Insets(8, 8, 8, 8));
        b.setCursor(Cursor.HAND);
        b.setTextFill(Color.web(fg));
        b.setFont(Font.font("System", FontWeight.BOLD, 8.5));
        b.setStyle("-fx-background-color:" + bg + ";-fx-border-color:#2c4b61;-fx-border-radius:6;-fx-background-radius:6;");
        return b;
    }

    // ==================================================================
    // PRESETS
    // ==================================================================

    private void applyPreset(String preset) {
        applyingPreset = true;
        selectedFlightProfile = preset;

        switch (preset) {
            case "Take-off" -> {
                airspeedSlider.setValue(72);
                altitudeSlider.setValue(0);
                thrustSlider.setValue(17500);
                wingAreaSlider.setValue(18.0);
                clSlider.setValue(0.95);
                cd0Slider.setValue(0.030);
                kSlider.setValue(0.040);
                timeStepSlider.setValue(0.08);
                simulationTimeSlider.setValue(30);
            }
            case "Landing" -> {
                airspeedSlider.setValue(58);
                thrustSlider.setValue(8500);
                wingAreaSlider.setValue(19.5);
                clSlider.setValue(1.05);
                cd0Slider.setValue(0.035);
                kSlider.setValue(0.042);
                timeStepSlider.setValue(0.08);
                simulationTimeSlider.setValue(60);

                autoLandingAltitude = true;
                setLandingAltitudeForDuration(simulationTimeSlider.getValue());
            }
            case "Cruise" -> {
                airspeedSlider.setValue(185);
                altitudeSlider.setValue(8000);
                thrustSlider.setValue(12000);
                wingAreaSlider.setValue(16.2);
                clSlider.setValue(0.65);
                cd0Slider.setValue(0.024);
                kSlider.setValue(0.036);
                timeStepSlider.setValue(0.10);
                simulationTimeSlider.setValue(180);
            }
            case "High Altitude" -> {
                airspeedSlider.setValue(210);
                altitudeSlider.setValue(10500);
                thrustSlider.setValue(11500);
                wingAreaSlider.setValue(16.2);
                clSlider.setValue(0.62);
                cd0Slider.setValue(0.026);
                kSlider.setValue(0.037);
                timeStepSlider.setValue(0.10);
                simulationTimeSlider.setValue(300);
            }
            default -> { }
        }

        if (!"Landing".equals(preset)) {
            autoLandingAltitude = false;
        }

        applyingPreset = false;
        refreshSliderLabels();
        needsReset = true;
        rebuildSimulation();
    }

    private String formatSimulationTime(double seconds) {
        int totalSeconds = (int)Math.round(seconds);
        if (totalSeconds < 60) {
            return totalSeconds + " s";
        }
        return (totalSeconds / 60) + " min";
    }

    private void refreshSliderLabels() {
        if (airspeedSlider == null) return;
        airspeedValue.setText(format(airspeedSlider.getValue(),1) + " m/s");
        altitudeValue.setText(format(altitudeSlider.getValue(),0) + " m");
        thrustValue.setText(format(thrustSlider.getValue()/1000,2) + " kN");
        wingAreaValue.setText(format(wingAreaSlider.getValue(),2) + " m^2");
        clValue.setText(format(clSlider.getValue(),3));
        cd0Value.setText(format(cd0Slider.getValue(),3));
        kValue.setText(format(kSlider.getValue(),4));
        timeStepValue.setText(format(timeStepSlider.getValue(),2) + " s");
        simulationTimeValue.setText(formatSimulationTime(simulationTimeSlider.getValue()));
        setActiveDurationButton((int)Math.round(simulationTimeSlider.getValue()));
        playbackSpeedValue.setText(format(playbackSpeedSlider.getValue(),1) + "x");
    }

    // ==================================================================
    // TELEMETRY RAIL
    // ==================================================================

    private VBox buildTelemetryRail() {
        VBox outer = new VBox(6);
        outer.setPadding(new Insets(8));
        outer.setStyle("-fx-background-color:#07131f;-fx-background-radius:10;-fx-border-color:#18374e;-fx-border-radius:10;");

        Label h = new Label("REAL-TIME TELEMETRY");
        h.setTextFill(Color.web("#dceefa"));
        h.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 11));

        Label desc = new Label("Values update directly from Simulation.State.");
        desc.setTextFill(Color.web("#547590"));
        desc.setWrapText(true);
        desc.setFont(Font.font("System", 8));

        timeValue = telemetryValue();
        distanceValue = telemetryValue();
        velocityTelemetry = telemetryValue();
        altitudeTelemetry = telemetryValue();
        accelerationValue = telemetryValue();
        liftTelemetry = telemetryValue();
        dragTelemetry = telemetryValue();
        thrustTelemetry = telemetryValue();
        weightTelemetry = telemetryValue();
        twValue = telemetryValue();
        ldValue = telemetryValue();
        flightAngleValue = telemetryValue();
        dynamicPressureValue = telemetryValue();
        densityValue = telemetryValue();
        machValue = telemetryValue();

        VBox cards = new VBox(4,
                telemetryCard("TIME", timeValue, "Simulation time."),
                telemetryCard("DISTANCE", distanceValue, "Horizontal distance travelled."),
                telemetryCard("VELOCITY", velocityTelemetry, "Current true airspeed."),
                telemetryCard("ALTITUDE", altitudeTelemetry, "Current aircraft altitude."),
                telemetryCard("ACCELERATION", accelerationValue, "Longitudinal acceleration."),
                telemetryCard("LIFT", liftTelemetry, "Aerodynamic lift force."),
                telemetryCard("DRAG", dragTelemetry, "Aerodynamic drag force."),
                telemetryCard("THRUST", thrustTelemetry, "Propulsive force."),
                telemetryCard("WEIGHT", weightTelemetry, "Aircraft weight."),
                telemetryCard("T / W", twValue, "Thrust-to-weight ratio."),
                telemetryCard("L / D", ldValue, "Lift-to-drag ratio."),
                telemetryCard("FLIGHT PATH", flightAngleValue, "Current flight-path angle."),
                telemetryCard("DYNAMIC PRESSURE", dynamicPressureValue, "q = 1/2 rho V^2."),
                telemetryCard("AIR DENSITY", densityValue, "Atmospheric density."),
                telemetryCard("MACH", machValue, "Approximate Mach number.")
        );

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        outer.getChildren().addAll(h, desc, scroll);
        return outer;
    }

    private Label telemetryValue() {
        Label l = new Label("-");
        l.setTextFill(Color.web("#eef7ff"));
        l.setFont(Font.font("System", FontWeight.BOLD, 12.5));
        return l;
    }

    private VBox telemetryCard(String title, Label value, String tip) {
        Label t = new Label(title);
        t.setTextFill(Color.web("#5e7f98"));
        t.setFont(Font.font("System", FontWeight.BOLD, 7.2));
        Tooltip.install(value, new Tooltip(tip));
        VBox b = new VBox(2, t, value);
        b.setPadding(new Insets(6, 8, 6, 8));
        b.setStyle("-fx-background-color:#091a29;-fx-border-color:#193850;-fx-border-radius:7;-fx-background-radius:7;");
        return b;
    }

    // ==================================================================
    // CHARTS
    // ==================================================================

    private HBox buildCharts() {
        NumberAxis vx = new NumberAxis();
        NumberAxis vy = new NumberAxis();
        vx.setLabel("Time (s)");
        vy.setLabel("Velocity (m/s)");
        velocityChart = new LineChart<>(vx, vy);
        velocityChart.setTitle("VELOCITY RESPONSE");
        velocityChart.setLegendVisible(false);
        velocityChart.setAnimated(false);
        velocityChart.setCreateSymbols(false);

        NumberAxis ax = new NumberAxis();
        NumberAxis ay = new NumberAxis();
        ax.setLabel("Time (s)");
        ay.setLabel("Altitude (m)");
        altitudeChart = new LineChart<>(ax, ay);
        altitudeChart.setTitle("ALTITUDE RESPONSE");
        altitudeChart.setLegendVisible(false);
        altitudeChart.setAnimated(false);
        altitudeChart.setCreateSymbols(false);

        velocitySeries = new XYChart.Series<>();
        altitudeSeries = new XYChart.Series<>();
        velocityChart.getData().add(velocitySeries);
        altitudeChart.getData().add(altitudeSeries);
        velocityChart.setStyle("-fx-background-color:#07131f;");
        altitudeChart.setStyle("-fx-background-color:#07131f;");

        VBox a = new VBox(velocityChart);
        VBox b = new VBox(altitudeChart);
        a.setPadding(new Insets(4));
        b.setPadding(new Insets(4));
        a.setStyle("-fx-background-color:#07131f;-fx-border-color:#18364d;-fx-border-radius:9;-fx-background-radius:9;");
        b.setStyle("-fx-background-color:#07131f;-fx-border-color:#18364d;-fx-border-radius:9;-fx-background-radius:9;");
        HBox.setHgrow(a, Priority.ALWAYS);
        HBox.setHgrow(b, Priority.ALWAYS);

        HBox row = new HBox(7, a, b);
        return row;
    }

    private void clearCharts() {
        velocitySeries.getData().clear();
        altitudeSeries.getData().clear();
        lastChartTime = -1.0;
    }

    private void recordChartState(Simulation.State state) {
        if (lastChartTime >= 0 && state.getTime() - lastChartTime < 0.15) return;
        lastChartTime = state.getTime();

        velocitySeries.getData().add(new XYChart.Data<>(state.getTime(), state.getVelocity()));
        altitudeSeries.getData().add(new XYChart.Data<>(state.getTime(), state.getAltitude()));

        while (velocitySeries.getData().size() > 2000) velocitySeries.getData().remove(0);
        while (altitudeSeries.getData().size() > 2000) altitudeSeries.getData().remove(0);
    }

    // ==================================================================
    // SIMULATION CONTROL
    // ==================================================================

    private void rebuildSimulation() {
        if (airspeedSlider == null) return;

        running = false;
        finished = false;
        lastFrameNanos = -1L;
        timeAccumulator = 0;

        refreshSliderLabels();

        Aircraft aircraft = new Aircraft(
                DEFAULT_MASS,
                wingAreaSlider.getValue(),
                airspeedSlider.getValue(),
                altitudeSlider.getValue()
        );

        simulation = new Simulation(
                aircraft,
                clSlider.getValue(),
                cd0Slider.getValue(),
                kSlider.getValue(),
                thrustSlider.getValue(),
                DEFAULT_EXHAUST_VELOCITY,
                timeStepSlider.getValue(),
                simulationTimeSlider.getValue(),
                selectedFlightProfile
        );

        setActiveDurationButton((int)Math.round(simulation.getSimulationTime()));
        needsReset = false;
        clearCharts();
        resetAircraftVisual();
        refreshSimulationUI();

        statusLabel.setText("* READY");
        statusLabel.setTextFill(Color.web("#71e0a0"));
        footerLabel.setText("PHYSICS ENGINE   * READY   STANDARD ATMOSPHERE   STEP " + format(simulation.getTimeStep(), 2) + " s");
    }

    private void playSimulation() {
        if (needsReset || simulation == null || finished) rebuildSimulation();
        running = true;
        finished = false;
        lastFrameNanos = -1L;
        statusLabel.setText("* RUNNING");
        statusLabel.setTextFill(Color.web("#6dd6ef"));
        footerLabel.setText("PHYSICS ENGINE   * RUNNING   STANDARD ATMOSPHERE   LIVE VIEW");
    }

    private void pauseSimulation() {
        running = false;
        statusLabel.setText("* PAUSED");
        statusLabel.setTextFill(Color.web("#e4c66a"));
        footerLabel.setText("PHYSICS ENGINE   * PAUSED   SIMULATION STATE PRESERVED");
    }

    private void refreshSimulationUI() {
        if (simulation == null) return;
        Simulation.State s = simulation.getState();

        timeValue.setText(format(s.getTime(), 2) + " s");
        distanceValue.setText(format(s.getDistance(), 1) + " m");
        velocityTelemetry.setText(format(s.getVelocity(), 2) + " m/s");
        altitudeTelemetry.setText(format(s.getAltitude(), 1) + " m");
        accelerationValue.setText(format(s.getAcceleration(), 2) + " m/s^2");
        liftTelemetry.setText(formatForce(s.getLift()));
        dragTelemetry.setText(formatForce(s.getDrag()));
        thrustTelemetry.setText(formatForce(s.getThrust()));
        weightTelemetry.setText(formatForce(DEFAULT_MASS * GRAVITY));
        twValue.setText(format(s.getThrustToWeightRatio(), 3));
        ldValue.setText(format(s.getLiftToDragRatio(), 2));
        flightAngleValue.setText(format(Math.toDegrees(s.getFlightPathAngle()), 2) + " deg");
        dynamicPressureValue.setText(format(s.getDynamicPressure(), 1) + " Pa");

        Atmosphere atmosphere = new Atmosphere(Math.max(0, s.getAltitude()));
        densityValue.setText(format(atmosphere.getDensity(), 4) + " kg/m^3");
        machValue.setText(format(s.getVelocity() / SPEED_OF_SOUND, 3));

        updateWorldState();
        recordChartState(s);
    }

    // ==================================================================
    // ANALYSIS PAGE
    // ==================================================================

    private void showAnalysis() {
        running = false;
        setActiveNav(analysisNav);
        content.getChildren().setAll(buildAnalysisPage());
    }

    private Node buildAnalysisPage() {
        BorderPane page = new BorderPane();
        page.setPadding(new Insets(14));
        page.setStyle("-fx-background-color:#050b13;");

        VBox outer = new VBox(9);
        Label heading = new Label("ENGINEERING ANALYSIS");
        heading.setTextFill(Color.web("#e3effa"));
        heading.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 18));

        Label description = new Label("Run the analytical model using the same physics classes as the live simulator.");
        description.setTextFill(Color.web("#66829b"));
        description.setFont(Font.font("System", 10));
        description.setWrapText(true);

        VBox inputs = new VBox(9);
        HBox r1 = new HBox(9);
        HBox r2 = new HBox(9);
        HBox r3 = new HBox(9);

        massField = analysisField("5000");
        wingAreaField = analysisField("16.2");
        velocityField = analysisField("77.22");
        altitudeField = analysisField("0");
        clField = analysisField("0.828729");
        cd0Field = analysisField("0.025");
        kField = analysisField("0.0364011111");
        thrustField = analysisField("10000");
        exhaustField = analysisField("2500");

        r1.getChildren().addAll(input("Mass (kg)", massField), input("Wing area (m^2)", wingAreaField), input("Velocity (m/s)", velocityField));
        r2.getChildren().addAll(input("Altitude (m)", altitudeField), input("Lift coefficient CL", clField), input("Zero-lift CD0", cd0Field));
        r3.getChildren().addAll(input("Induced factor k", kField), input("Thrust (N)", thrustField), input("Exhaust velocity (m/s)", exhaustField));
        HBox.setHgrow(r1, Priority.ALWAYS);
        inputs.getChildren().addAll(r1, r2, r3);

        Button calculate = controlButton("CALCULATE", "#12344d", "#93dfff");
        calculate.setPrefWidth(120);
        calculate.setOnAction(e -> runAnalysis());

        VBox inputCard = new VBox(10, inputs, calculate);
        inputCard.setPadding(new Insets(13));
        inputCard.setStyle("-fx-background-color:#091a2a;-fx-border-color:#19364f;-fx-border-radius:10;-fx-background-radius:10;");

        analysisOutput = new javafx.scene.control.TextArea();
        analysisOutput.setEditable(false);
        analysisOutput.setStyle("-fx-control-inner-background:#071724;-fx-text-fill:#d9e9f7;-fx-font-family:'Consolas';-fx-font-size:11px;-fx-border-color:#19364f;");

        VBox outputCard = new VBox(7, section("RESULTS"), analysisOutput);
        outputCard.setPadding(new Insets(13));
        outputCard.setStyle("-fx-background-color:#091a2a;-fx-border-color:#19364f;-fx-border-radius:10;-fx-background-radius:10;");
        VBox.setVgrow(analysisOutput, Priority.ALWAYS);
        VBox.setVgrow(outputCard, Priority.ALWAYS);
        VBox.setVgrow(outer, Priority.ALWAYS);

        outer.getChildren().addAll(heading, description, inputCard, outputCard);
        page.setCenter(outer);
        runAnalysis();
        return page;
    }

    private VBox input(String label, javafx.scene.control.TextField field) {
        VBox box = new VBox(4, section(label), field);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private javafx.scene.control.TextField analysisField(String value) {
        javafx.scene.control.TextField field = new javafx.scene.control.TextField(value);
        field.setPrefWidth(160);
        field.setStyle("-fx-background-color:#071724;-fx-text-fill:#e2eef8;-fx-border-color:#21435c;-fx-border-radius:6;-fx-background-radius:6;");
        return field;
    }

    private void runAnalysis() {
        if (analysisOutput == null) return;
        try {
            double mass = read(massField);
            double wingArea = read(wingAreaField);
            double velocity = read(velocityField);
            double altitude = read(altitudeField);
            double cl = read(clField);
            double cd0 = read(cd0Field);
            double k = read(kField);
            double thrust = read(thrustField);
            double exhaust = read(exhaustField);

            InputValidator.validateMass(mass);
            InputValidator.validateWingArea(wingArea);
            InputValidator.validateVelocity(velocity);
            InputValidator.validateAltitude(altitude);
            InputValidator.validateLiftCoefficient(cl);
            InputValidator.validateCd0(cd0);
            InputValidator.validateInducedDragFactor(k);
            InputValidator.validatePositive(thrust, "Thrust");
            InputValidator.validatePositive(exhaust, "Exhaust velocity");

            Aircraft aircraft = new Aircraft(mass, wingArea, velocity, altitude);
            Atmosphere atmosphere = new Atmosphere(altitude);
            Aerodynamics aero = new Aerodynamics(aircraft, atmosphere, cl, cd0, k);
            Propulsion propulsion = new Propulsion(aircraft, aero, thrust, exhaust);
            PerformanceAnalyzer performance = new PerformanceAnalyzer(aircraft, aero, propulsion);
            Optimizer optimizer = new Optimizer(mass * GRAVITY, wingArea, atmosphere.getDensity(), cd0, k);
            optimizer.optimize();

            StringBuilder out = new StringBuilder();
            out.append("===== AEROMECH ANALYSIS =====\n\n");
            out.append("ATMOSPHERE\n");
            out.append("Temperature      : ").append(format(atmosphere.getTemperature(), 2)).append(" K\n");
            out.append("Pressure         : ").append(format(atmosphere.getPressure(), 2)).append(" Pa\n");
            out.append("Density          : ").append(format(atmosphere.getDensity(), 4)).append(" kg/m^3\n\n");
            out.append("AERODYNAMICS\n");
            out.append("Velocity         : ").append(format(velocity, 2)).append(" m/s\n");
            out.append("Dynamic pressure : ").append(format(aero.getDynamicPressure(), 2)).append(" Pa\n");
            out.append("Lift             : ").append(format(aero.getLift(), 2)).append(" N\n");
            out.append("Drag             : ").append(format(aero.getDrag(), 2)).append(" N\n");
            out.append("L/D              : ").append(format(aero.getLiftToDragRatio(), 4)).append("\n\n");
            out.append("PROPULSION\n");
            out.append("Thrust           : ").append(format(thrust, 2)).append(" N\n");
            out.append("T/W              : ").append(format(propulsion.getThrustToWeightRatio(), 4)).append("\n");
            out.append("Acceleration     : ").append(format(propulsion.getAcceleration(), 4)).append(" m/s^2\n");
            out.append("Power            : ").append(format(propulsion.getPropulsivePower(), 2)).append(" W\n");
            out.append("Mass flow        : ").append(format(propulsion.getMassFlowRate(), 4)).append(" kg/s\n");
            out.append("Specific impulse : ").append(format(propulsion.getSpecificImpulse(), 2)).append(" s\n\n");
            out.append("PERFORMANCE\n");
            out.append("Weight           : ").append(format(performance.getWeight(), 2)).append(" N\n");
            out.append("Excess thrust    : ").append(format(performance.getExcessThrust(), 2)).append(" N\n");
            out.append("Power required   : ").append(format(performance.getPowerRequired(), 2)).append(" W\n");
            out.append("Excess power     : ").append(format(performance.getExcessPower(), 2)).append(" W\n");
            out.append("Rate of climb    : ").append(format(performance.getExcessPower() / performance.getWeight(), 4)).append(" m/s\n\n");
            out.append("OPTIMIZATION\n");
            out.append("Optimal CL       : ").append(format(optimizer.getOptimalCL(), 6)).append("\n");
            out.append("Optimal CD       : ").append(format(optimizer.getOptimalCD(), 6)).append("\n");
            out.append("Maximum L/D      : ").append(format(optimizer.getMaximumLDRatio(), 6)).append("\n");
            out.append("Optimal velocity : ").append(format(optimizer.getOptimalVelocity(), 3)).append(" m/s\n");

            analysisOutput.setText(out.toString());
            statusLabel.setText("* ANALYSIS READY");
            statusLabel.setTextFill(Color.web("#71e0a0"));
            footerLabel.setText("PHYSICS ENGINE   * ANALYSIS READY   STANDARD ATMOSPHERE");
        } catch (Exception ex) {
            analysisOutput.setText("INPUT ERROR\n\n" + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            statusLabel.setText("* INPUT ERROR");
            statusLabel.setTextFill(Color.web("#ef737b"));
        }
    }

    private double read(javafx.scene.control.TextField field) {
        return Double.parseDouble(field.getText().trim());
    }

    // ==================================================================
    // FORMATTING
    // ==================================================================

    private String format(double value, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    private String formatForce(double value) {
        if (Math.abs(value) >= 1000) return format(value / 1000.0, 2) + " kN";
        return format(value, 1) + " N";
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // ------------------------------------------------------------------
    // Resizable pane helper
    // ------------------------------------------------------------------
    private static class PaneProxy extends javafx.scene.layout.Pane {
        PaneProxy(double prefWidth, double prefHeight) {
            setPrefWidth(prefWidth);
            setPrefHeight(prefHeight);
        }
        void clear() { getChildren().clear(); }
        void addAll(Node... nodes) { getChildren().addAll(nodes); }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
