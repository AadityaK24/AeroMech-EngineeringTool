import java.util.Locale;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class Main extends Application {

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
    private static final double DEFAULT_SIMULATION_TIME = 20.0;

    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();

    private Button analysisNav;
    private Button simulationNav;

    private TextField massField;
    private TextField wingAreaField;
    private TextField velocityField;
    private TextField altitudeField;
    private TextField clField;
    private TextField cd0Field;
    private TextField kField;
    private TextField thrustField;
    private TextField exhaustField;
    private TextArea analysisOutput;

    private Slider thrustSlider;
    private Slider wingAreaSlider;
    private Slider altitudeSlider;
    private Slider timeStepSlider;
    private Slider simulationTimeSlider;

    private Label thrustSliderValue;
    private Label wingAreaSliderValue;
    private Label altitudeSliderValue;
    private Label timeStepSliderValue;
    private Label simulationTimeSliderValue;

    private Label statusLabel;
    private Label footerLabel;

    private Label timeValue;
    private Label distanceValue;
    private Label velocityValue;
    private Label accelerationValue;
    private Label liftValue;
    private Label dragValue;
    private Label thrustValue;
    private Label twValue;
    private Label ldValue;
    private Label altitudeHud;

    private PaneProxy flightViewport;
    private Group aircraftGraphic;
    private Polyline trajectory;
    private double lastAircraftX;
    private double lastAircraftY;
    private double visualAngle;

    private static final String VIEWPORT_BACKGROUND = "viewportBackground";

    private Simulation simulation;
    private AnimationTimer animationTimer;
    private boolean running;
    private boolean finished;

    private long lastFrameNanos = -1L;
    private double timeAccumulator = 0.0;
    private boolean needsReset = true;

    @Override
    public void start(Stage stage) {
        root.setTop(buildHeader());
        root.setLeft(buildNavigation());
        root.setCenter(content);
        root.setBottom(buildFooter());

        showSimulation();

        Scene scene = new Scene(root, 1500, 920);
        scene.setFill(Color.web("#06111f"));

        stage.setTitle("AeroMech — Aerospace Engineering Simulator");
        stage.setMinWidth(1200);
        stage.setMinHeight(760);
        stage.setScene(scene);
        stage.show();

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!running) {
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
                timeAccumulator += frameSeconds;

                double dt = simulation.getTimeStep();
                int maxStepsThisFrame = 12;
                int steps = 0;

                while (timeAccumulator >= dt
                        && steps < maxStepsThisFrame
                        && !simulation.isComplete()) {

                    simulation.step();
                    timeAccumulator -= dt;
                    steps++;
                }

                refreshSimulationUI();

                if (simulation.isComplete()) {
                    running = false;
                    finished = true;

                    statusLabel.setText("● SIMULATION COMPLETE");
                    statusLabel.setTextFill(Color.web("#6ee7a5"));

                    footerLabel.setText(
                            "PHYSICS ENGINE   ● COMPLETE   MODEL: STANDARD ATMOSPHERE"
                    );
                }
            }
        };

        animationTimer.start();
    }

    @Override
    public void stop() {
        running = false;
    }

    private Node buildHeader() {
        HBox header = new HBox(18);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(17, 24, 17, 24));
        header.setStyle(
                "-fx-background-color: #081625; "
                + "-fx-border-color: #18314a; "
                + "-fx-border-width: 0 0 1 0;"
        );

        Label title = new Label("AEROMECH");
        title.setTextFill(Color.web("#edf5ff"));
        title.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 23));

        Label subtitle = new Label(
                "AEROSPACE / MECHATRONICS ENGINEERING PLATFORM"
        );
        subtitle.setTextFill(Color.web("#6887a5"));
        subtitle.setFont(Font.font("System", FontWeight.MEDIUM, 11));

        HBox brand = new HBox(12, title, subtitle);
        brand.setAlignment(Pos.BASELINE_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label section = new Label("FLIGHT SIMULATION");
        section.setTextFill(Color.web("#8ea9c2"));
        section.setFont(Font.font("System", FontWeight.BOLD, 11));

        statusLabel = new Label("● READY");
        statusLabel.setTextFill(Color.web("#6ee7a5"));
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 11));

        header.getChildren().addAll(
                brand,
                spacer,
                section,
                statusLabel
        );

        return header;
    }

    private Node buildNavigation() {
        VBox nav = new VBox(8);
        nav.setPrefWidth(180);
        nav.setPadding(new Insets(22, 12, 20, 12));
        nav.setStyle(
                "-fx-background-color: #071321; "
                + "-fx-border-color: #18314a; "
                + "-fx-border-width: 0 1 0 0;"
        );

        Label navTitle = new Label("WORKSPACE");
        navTitle.setTextFill(Color.web("#4e769b"));
        navTitle.setFont(Font.font("System", FontWeight.BOLD, 10));
        navTitle.setPadding(new Insets(0, 0, 8, 10));

        analysisNav = navButton("ANALYSIS");
        simulationNav = navButton("SIMULATION");

        analysisNav.setOnAction(e -> showAnalysis());
        simulationNav.setOnAction(e -> showSimulation());

        nav.getChildren().addAll(
                navTitle,
                analysisNav,
                simulationNav
        );

        return nav;
    }

    private Button navButton(String text) {
        Button button = new Button(text);

        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(12, 13, 12, 13));
        button.setTextFill(Color.web("#8da8c1"));
        button.setFont(Font.font("System", FontWeight.BOLD, 11));
        button.setCursor(Cursor.HAND);

        button.setStyle(
                "-fx-background-color: transparent; "
                + "-fx-background-radius: 7; "
                + "-fx-border-color: transparent;"
        );

        button.setOnMouseEntered(e -> {
            if (button != analysisNav
                    || button.getStyle().contains("#12273b") == false) {

                button.setStyle(
                        "-fx-background-color: #0d2033; "
                        + "-fx-background-radius: 7;"
                );
            }
        });

        button.setOnMouseExited(e -> {
            if (button == analysisNav
                    && analysisNav.getStyle().contains("#12273b")) {
                return;
            }

            if (button == simulationNav
                    && simulationNav.getStyle().contains("#12273b")) {
                return;
            }

            button.setStyle(
                    "-fx-background-color: transparent; "
                    + "-fx-background-radius: 7;"
            );
        });

        return button;
    }

    private Node buildFooter() {
        footerLabel = new Label(
                "PHYSICS ENGINE   ● READY   MODEL: STANDARD ATMOSPHERE"
        );

        footerLabel.setTextFill(Color.web("#52718f"));
        footerLabel.setFont(Font.font("System", FontWeight.MEDIUM, 10));
        footerLabel.setPadding(new Insets(10, 22, 11, 22));
        footerLabel.setStyle(
                "-fx-background-color: #06111e; "
                + "-fx-border-color: #17304a; "
                + "-fx-border-width: 1 0 0 0;"
        );

        return footerLabel;
    }

    private void showSimulation() {
        setActiveNav(simulationNav);

        content.getChildren().setAll(
                buildSimulationPage()
        );

        needsReset = true;
        rebuildSimulation();
    }

    private void showAnalysis() {
        running = false;

        setActiveNav(analysisNav);

        content.getChildren().setAll(
                buildAnalysisPage()
        );
    }

    private void setActiveNav(Button active) {
        if (analysisNav != null) {
            styleNavButton(
                    analysisNav,
                    active == analysisNav
            );
        }

        if (simulationNav != null) {
            styleNavButton(
                    simulationNav,
                    active == simulationNav
            );
        }
    }

    private void styleNavButton(
            Button button,
            boolean active) {

        if (active) {
            button.setStyle(
                    "-fx-background-color: #12273b; "
                    + "-fx-background-radius: 7; "
                    + "-fx-border-color: #1f5d86; "
                    + "-fx-border-radius: 7;"
            );

            button.setTextFill(
                    Color.web("#d7ecff")
            );

        } else {
            button.setStyle(
                    "-fx-background-color: transparent; "
                    + "-fx-background-radius: 7;"
            );

            button.setTextFill(
                    Color.web("#8da8c1")
            );
        }
    }

    private Node buildSimulationPage() {
        BorderPane page = new BorderPane();

        page.setPadding(
                new Insets(16, 18, 16, 18)
        );

        page.setStyle(
                "-fx-background-color: #06111f;"
        );

        VBox center = new VBox(12);
        center.setFillWidth(true);

        flightViewport = new PaneProxy(
                1280,
                455
        );

        flightViewport.setStyle(
                "-fx-background-color: #071d33; "
                + "-fx-background-radius: 12; "
                + "-fx-border-color: #183a56; "
                + "-fx-border-radius: 12;"
        );

        buildFlightScene(
                flightViewport
        );

        VBox telemetry = buildTelemetryPanel();
        VBox controls = buildControlsPanel();

        center.getChildren().addAll(
                flightViewport,
                telemetry,
                controls
        );

        VBox.setVgrow(
                flightViewport,
                Priority.ALWAYS
        );

        page.setCenter(center);

        return page;
    }

    private void buildFlightScene(
            PaneProxy pane) {

        pane.clear();

        Rectangle sky = new Rectangle();

        sky.setFill(
                Color.web("#061a2e")
        );

        sky.widthProperty().bind(
                pane.widthProperty()
        );

        sky.heightProperty().bind(
                pane.heightProperty()
        );

        sky.getProperties().put(
                VIEWPORT_BACKGROUND,
                Boolean.TRUE
        );

        Rectangle atmosphericGlow =
                new Rectangle();

        atmosphericGlow.setFill(
                Color.web("#0a2941")
        );

        atmosphericGlow.widthProperty().bind(
                pane.widthProperty()
        );

        atmosphericGlow.yProperty().bind(
                pane.heightProperty().multiply(0.69)
        );

        atmosphericGlow.setHeight(28);

        atmosphericGlow.getProperties().put(
                VIEWPORT_BACKGROUND,
                Boolean.TRUE
        );

        Rectangle ground = new Rectangle();

        ground.setFill(
                Color.web("#c7aa79")
        );

        ground.widthProperty().bind(
                pane.widthProperty()
        );

        ground.yProperty().bind(
                pane.heightProperty().multiply(0.76)
        );

        ground.heightProperty().bind(
                pane.heightProperty().multiply(0.24)
        );

        ground.getProperties().put(
                VIEWPORT_BACKGROUND,
                Boolean.TRUE
        );

        Rectangle farGround =
                new Rectangle();

        farGround.setFill(
                Color.web("#a98d62")
        );

        farGround.widthProperty().bind(
                pane.widthProperty()
        );

        farGround.yProperty().bind(
                pane.heightProperty().multiply(0.74)
        );

        farGround.heightProperty().bind(
                pane.heightProperty().multiply(0.04)
        );

        farGround.getProperties().put(
                VIEWPORT_BACKGROUND,
                Boolean.TRUE
        );

        Line horizon = new Line();

        horizon.setStroke(
                Color.web("#d5bf96")
        );

        horizon.setStrokeWidth(1.5);

        horizon.endXProperty().bind(
                pane.widthProperty()
        );

        horizon.startYProperty().bind(
                pane.heightProperty().multiply(0.76)
        );

        horizon.endYProperty().bind(
                pane.heightProperty().multiply(0.76)
        );

        horizon.getProperties().put(
                VIEWPORT_BACKGROUND,
                Boolean.TRUE
        );

        Circle sun =
                new Circle(31);

        sun.setFill(
                Color.web("#f3b94f")
        );

        sun.setEffect(null);

        sun.centerXProperty().bind(
                pane.widthProperty().multiply(0.82)
        );

        sun.centerYProperty().bind(
                pane.heightProperty().multiply(0.20)
        );

        sun.getProperties().put(
                VIEWPORT_BACKGROUND,
                Boolean.TRUE
        );

        Label viewportLabel =
                new Label(
                        "2D FLIGHT DYNAMICS  /  LIVE TELEMETRY"
                );

        viewportLabel.setTextFill(
                Color.web("#597891")
        );

        viewportLabel.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        10
                )
        );

        viewportLabel.setLayoutX(22);
        viewportLabel.setLayoutY(18);

        Label modeLabel =
                new Label(
                        "NOMINAL FLIGHT ENVELOPE"
                );

        modeLabel.setTextFill(
                Color.web("#6ee7a5")
        );

        modeLabel.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        10
                )
        );

        modeLabel.setLayoutX(22);
        modeLabel.setLayoutY(39);

        trajectory = new Polyline();

        trajectory.setStroke(
                Color.web("#41c9e8")
        );

        trajectory.setStrokeWidth(2.0);

        trajectory.getStrokeDashArray().addAll(
                6.0,
                8.0
        );

        trajectory.setFill(
                Color.TRANSPARENT
        );

        trajectory.setOpacity(0.65);

        aircraftGraphic =
                buildAircraftGraphic();

        aircraftGraphic.setLayoutX(145);
        aircraftGraphic.setLayoutY(195);

        Label distanceAxis =
                new Label(
                        "DISTANCE →"
                );

        distanceAxis.setTextFill(
                Color.web("#55748e")
        );

        distanceAxis.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        9
                )
        );

        distanceAxis.setLayoutX(26);

        distanceAxis.layoutYProperty().bind(
                pane.heightProperty().subtract(36)
        );

        altitudeHud =
                new Label(
                        "ALTITUDE   0 m"
                );

        altitudeHud.setTextFill(
                Color.web("#8feaff")
        );

        altitudeHud.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        11
                )
        );

        altitudeHud.layoutXProperty().bind(
                pane.widthProperty().subtract(180)
        );

        altitudeHud.setLayoutY(30);

        Rectangle hudPanel =
                new Rectangle(
                        1020,
                        62,
                        230,
                        82
                );

        hudPanel.layoutXProperty().bind(
                pane.widthProperty().subtract(250)
        );

        hudPanel.setFill(
                Color.web(
                        "#071525",
                        0.84
                )
        );

        hudPanel.setStroke(
                Color.web("#21445f")
        );

        hudPanel.setArcWidth(10);
        hudPanel.setArcHeight(10);

        Label flightHud =
                new Label(
                        "FLIGHT STATUS\n\nNOMINAL"
                );

        flightHud.setTextFill(
                Color.web("#d1e6f8")
        );

        flightHud.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        10
                )
        );

        flightHud.layoutXProperty().bind(
                pane.widthProperty().subtract(230)
        );

        flightHud.setLayoutY(77);

        pane.addAll(
                sky,
                atmosphericGlow,
                farGround,
                ground,
                horizon,
                sun,
                trajectory,
                hudPanel,
                viewportLabel,
                modeLabel,
                aircraftGraphic,
                distanceAxis,
                altitudeHud,
                flightHud
        );
    }

    private Group buildAircraftGraphic() {

        Polygon body =
                new Polygon(
                        0, 0,
                        58, 0,
                        76, -5,
                        58, -10,
                        0, -10
                );

        body.setFill(
                Color.web("#e7edf1")
        );

        body.setStroke(
                Color.web("#aebbc4")
        );

        body.setStrokeWidth(1.0);

        Polygon wing =
                new Polygon(
                        25, -3,
                        48, -28,
                        55, -28,
                        42, -3,

                        55, 3,
                        48, 3,
                        26, 3
                );

        wing.setFill(
                Color.web("#d7dee4")
        );

        Polygon tail =
                new Polygon(
                        9, -4,
                        0, -20,
                        7, -20,
                        18, -4
                );

        tail.setFill(
                Color.web("#cfd8df")
        );

        Circle cockpit =
                new Circle(
                        55,
                        -5,
                        3.5
                );

        cockpit.setFill(
                Color.web("#7f9bab")
        );

        Group aircraft =
                new Group(
                        body,
                        wing,
                        tail,
                        cockpit
                );

        aircraft.setScaleX(1.12);
        aircraft.setScaleY(1.12);

        return aircraft;
    }

    private VBox buildTelemetryPanel() {

        VBox wrapper =
                new VBox(9);

        wrapper.setPadding(
                new Insets(0)
        );

        GridPane grid =
                new GridPane();

        grid.setHgap(9);
        grid.setVgap(9);

        timeValue = createValueLabel();
        distanceValue = createValueLabel();
        velocityValue = createValueLabel();
        accelerationValue = createValueLabel();
        liftValue = createValueLabel();
        dragValue = createValueLabel();
        thrustValue = createValueLabel();
        twValue = createValueLabel();
        ldValue = createValueLabel();

        grid.add(
                metricCard("TIME", timeValue),
                0,
                0
        );

        grid.add(
                metricCard("DISTANCE", distanceValue),
                1,
                0
        );

        grid.add(
                metricCard("VELOCITY", velocityValue),
                2,
                0
        );

        grid.add(
                metricCard("ACCELERATION", accelerationValue),
                3,
                0
        );

        grid.add(
                metricCard("LIFT", liftValue),
                4,
                0
        );

        grid.add(
                metricCard("DRAG", dragValue),
                0,
                1
        );

        grid.add(
                metricCard("THRUST", thrustValue),
                1,
                1
        );

        grid.add(
                metricCard("T / W", twValue),
                2,
                1
        );

        grid.add(
                metricCard("L / D", ldValue),
                3,
                1
        );

        for (int i = 0; i < 5; i++) {

            ColumnConstraints cc =
                    new ColumnConstraints();

            cc.setPercentWidth(20);

            grid.getColumnConstraints().add(cc);
        }

        wrapper.getChildren().add(grid);

        return wrapper;
    }

    private Label createValueLabel() {

        Label label =
                new Label("—");

        label.setTextFill(
                Color.web("#e9f4ff")
        );

        label.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        17
                )
        );

        return label;
    }

    private VBox metricCard(
            String title,
            Label value) {

        Label titleLabel =
                new Label(title);

        titleLabel.setTextFill(
                Color.web("#5e7d98")
        );

        titleLabel.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        9
                )
        );

        VBox box =
                new VBox(
                        5,
                        titleLabel,
                        value
                );

        box.setPadding(
                new Insets(
                        11,
                        13,
                        11,
                        13
                )
        );

        box.setMinHeight(60);

        box.setStyle(
                "-fx-background-color: #091a2a; "
                + "-fx-border-color: #19364f; "
                + "-fx-border-radius: 8; "
                + "-fx-background-radius: 8;"
        );

        return box;
    }

    private VBox buildControlsPanel() {

        VBox wrapper =
                new VBox(9);

        wrapper.setPadding(
                new Insets(
                        13,
                        15,
                        12,
                        15
                )
        );

        wrapper.setStyle(
                "-fx-background-color: #081827; "
                + "-fx-border-color: #19364f; "
                + "-fx-border-radius: 10; "
                + "-fx-background-radius: 10;"
        );

        Label heading =
                new Label(
                        "SIMULATION PARAMETERS"
                );

        heading.setTextFill(
                Color.web("#d8e8f6")
        );

        heading.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        10
                )
        );

        thrustSlider =
                createSlider(
                        2000,
                        30000,
                        DEFAULT_THRUST
                );

        wingAreaSlider =
                createSlider(
                        8,
                        30,
                        DEFAULT_WING_AREA
                );

        altitudeSlider =
                createSlider(
                        0,
                        11000,
                        DEFAULT_ALTITUDE
                );

        timeStepSlider =
                createSlider(
                        0.02,
                        0.50,
                        DEFAULT_TIME_STEP
                );

        simulationTimeSlider =
                createSlider(
                        5,
                        60,
                        DEFAULT_SIMULATION_TIME
                );

        thrustSliderValue =
                sliderValueLabel();

        wingAreaSliderValue =
                sliderValueLabel();

        altitudeSliderValue =
                sliderValueLabel();

        timeStepSliderValue =
                sliderValueLabel();

        simulationTimeSliderValue =
                sliderValueLabel();

        thrustSlider.valueProperty().addListener(
                (obs, oldV, newV) -> {

                    thrustSliderValue.setText(
                            formatKn(
                                    newV.doubleValue()
                            )
                    );

                    needsReset = true;
                }
        );

        wingAreaSlider.valueProperty().addListener(
                (obs, oldV, newV) -> {

                    wingAreaSliderValue.setText(
                            format(
                                    newV.doubleValue(),
                                    2
                            ) + " m²"
                    );

                    needsReset = true;
                }
        );

        altitudeSlider.valueProperty().addListener(
                (obs, oldV, newV) -> {

                    altitudeSliderValue.setText(
                            format(
                                    newV.doubleValue(),
                                    0
                            ) + " m"
                    );

                    needsReset = true;
                }
        );

        timeStepSlider.valueProperty().addListener(
                (obs, oldV, newV) -> {

                    timeStepSliderValue.setText(
                            format(
                                    newV.doubleValue(),
                                    2
                            ) + " s"
                    );

                    needsReset = true;
                }
        );

        simulationTimeSlider.valueProperty().addListener(
                (obs, oldV, newV) -> {

                    simulationTimeSliderValue.setText(
                            format(
                                    newV.doubleValue(),
                                    1
                            ) + " s"
                    );

                    needsReset = true;
                }
        );

        HBox row1 =
                parameterRow(
                        "THRUST",
                        thrustSlider,
                        thrustSliderValue
                );

        HBox row2 =
                parameterRow(
                        "WING AREA",
                        wingAreaSlider,
                        wingAreaSliderValue
                );

        HBox row3 =
                parameterRow(
                        "ALTITUDE",
                        altitudeSlider,
                        altitudeSliderValue
                );

        HBox row4 =
                parameterRow(
                        "TIME STEP",
                        timeStepSlider,
                        timeStepSliderValue
                );

        HBox row5 =
                parameterRow(
                        "SIMULATION TIME",
                        simulationTimeSlider,
                        simulationTimeSliderValue
                );

        Button play =
                controlButton(
                        "▶ PLAY",
                        "#123b2f",
                        "#6ee7a5"
                );

        Button pause =
                controlButton(
                        "❚ PAUSE",
                        "#2f2b1e",
                        "#e9c46a"
                );

        Button reset =
                controlButton(
                        "↻ RESET",
                        "#172b3c",
                        "#9bc6ea"
                );

        play.setOnAction(
                e -> playSimulation()
        );

        pause.setOnAction(
                e -> pauseSimulation()
        );

        reset.setOnAction(
                e -> resetSimulation()
        );

        HBox controls =
                new HBox(
                        10,
                        play,
                        pause,
                        reset
                );

        controls.setAlignment(
                Pos.CENTER
        );

        controls.setPadding(
                new Insets(
                        4,
                        0,
                        0,
                        0
                )
        );

        HBox status =
                new HBox();

        Label hint =
                new Label(
                        "Adjust a parameter, then PLAY or RESET to apply it."
                );

        hint.setTextFill(
                Color.web("#55748e")
        );

        hint.setFont(
                Font.font(
                        "System",
                        FontWeight.NORMAL,
                        9
                )
        );

        status.getChildren().add(hint);

        wrapper.getChildren().addAll(
                heading,
                row1,
                row2,
                row3,
                row4,
                row5,
                controls,
                status
        );

        return wrapper;
    }

    private Slider createSlider(
            double min,
            double max,
            double value) {

        Slider slider =
                new Slider(
                        min,
                        max,
                        value
                );

        slider.setMaxWidth(
                Double.MAX_VALUE
        );

        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);

        slider.setMajorTickUnit(
                (max - min) / 4.0
        );

        slider.setStyle(
                "-fx-control-inner-background: #1a344b;"
        );

        return slider;
    }

    private HBox parameterRow(
            String title,
            Slider slider,
            Label valueLabel) {

        Label titleLabel =
                new Label(title);

        titleLabel.setTextFill(
                Color.web("#7895ae")
        );

        titleLabel.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        9
                )
        );

        titleLabel.setMinWidth(105);

        HBox.setHgrow(
                slider,
                Priority.ALWAYS
        );

        valueLabel.setMinWidth(94);

        valueLabel.setAlignment(
                Pos.CENTER_RIGHT
        );

        HBox row =
                new HBox(
                        10,
                        titleLabel,
                        slider,
                        valueLabel
                );

        row.setAlignment(
                Pos.CENTER_LEFT
        );

        return row;
    }

    private Label sliderValueLabel() {

        Label label =
                new Label();

        label.setTextFill(
                Color.web("#dcecff")
        );

        label.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        10
                )
        );

        label.setAlignment(
                Pos.CENTER_RIGHT
        );

        return label;
    }

    private Button controlButton(
            String text,
            String background,
            String foreground) {

        Button button =
                new Button(text);

        button.setPrefWidth(115);

        button.setPadding(
                new Insets(
                        9,
                        16,
                        9,
                        16
                )
        );

        button.setTextFill(
                Color.web(foreground)
        );

        button.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        10
                )
        );

        button.setCursor(
                Cursor.HAND
        );

        button.setStyle(
                "-fx-background-color: " + background + "; "
                + "-fx-border-color: #284760; "
                + "-fx-border-radius: 7; "
                + "-fx-background-radius: 7;"
        );

        return button;
    }

    private Node buildAnalysisPage() {

        BorderPane page =
                new BorderPane();

        page.setPadding(
                new Insets(18)
        );

        page.setStyle(
                "-fx-background-color: #06111f;"
        );

        VBox outer =
                new VBox(14);

        Label heading =
                new Label(
                        "ENGINEERING ANALYSIS"
                );

        heading.setTextFill(
                Color.web("#e3effa")
        );

        heading.setFont(
                Font.font(
                        "System",
                        FontWeight.EXTRA_BOLD,
                        18
                )
        );

        Label description =
                new Label(
                        "Evaluate atmosphere, aerodynamics, propulsion, performance and aerodynamic optimization using the same backend models as the simulator."
                );

        description.setWrapText(true);

        description.setTextFill(
                Color.web("#66829b")
        );

        description.setFont(
                Font.font(
                        "System",
                        11
                )
        );

        GridPane inputGrid =
                new GridPane();

        inputGrid.setHgap(10);
        inputGrid.setVgap(10);

        massField =
                analysisField("5000");

        wingAreaField =
                analysisField("16.2");

        velocityField =
                analysisField("77.22");

        altitudeField =
                analysisField("0");

        clField =
                analysisField("0.828729");

        cd0Field =
                analysisField("0.025");

        kField =
                analysisField("0.0364011111");

        thrustField =
                analysisField("10000");

        exhaustField =
                analysisField("2500");

        addAnalysisInput(
                inputGrid,
                "Mass (kg)",
                massField,
                0,
                0
        );

        addAnalysisInput(
                inputGrid,
                "Wing area (m²)",
                wingAreaField,
                1,
                0
        );

        addAnalysisInput(
                inputGrid,
                "Velocity (m/s)",
                velocityField,
                2,
                0
        );

        addAnalysisInput(
                inputGrid,
                "Altitude (m)",
                altitudeField,
                0,
                1
        );

        addAnalysisInput(
                inputGrid,
                "Lift coefficient (CL)",
                clField,
                1,
                1
        );

        addAnalysisInput(
                inputGrid,
                "Zero-lift CD (CD0)",
                cd0Field,
                2,
                1
        );

        addAnalysisInput(
                inputGrid,
                "Induced factor (k)",
                kField,
                0,
                2
        );

        addAnalysisInput(
                inputGrid,
                "Thrust (N)",
                thrustField,
                1,
                2
        );

        addAnalysisInput(
                inputGrid,
                "Exhaust velocity (m/s)",
                exhaustField,
                2,
                2
        );

        Button calculate =
                controlButton(
                        "CALCULATE",
                        "#12344d",
                        "#92dfff"
                );

        calculate.setPrefWidth(135);

        calculate.setOnAction(
                e -> runAnalysis()
        );

        analysisOutput =
                new TextArea();

        analysisOutput.setEditable(false);
        analysisOutput.setWrapText(false);
        analysisOutput.setPrefRowCount(19);

        analysisOutput.setStyle(
                "-fx-control-inner-background: #071724; "
                + "-fx-text-fill: #d9e9f7; "
                + "-fx-font-family: 'Consolas'; "
                + "-fx-font-size: 12px; "
                + "-fx-border-color: #19364f; "
                + "-fx-border-radius: 8; "
                + "-fx-background-radius: 8;"
        );

        VBox inputCard =
                new VBox(
                        12,
                        inputGrid,
                        calculate
                );

        inputCard.setPadding(
                new Insets(15)
        );

        inputCard.setStyle(
                "-fx-background-color: #091a2a; "
                + "-fx-border-color: #19364f; "
                + "-fx-border-radius: 10; "
                + "-fx-background-radius: 10;"
        );

        VBox outputCard =
                new VBox(
                        10,
                        sectionLabel("RESULTS"),
                        analysisOutput
                );

        outputCard.setPadding(
                new Insets(15)
        );

        outputCard.setStyle(
                "-fx-background-color: #091a2a; "
                + "-fx-border-color: #19364f; "
                + "-fx-border-radius: 10; "
                + "-fx-background-radius: 10;"
        );

        VBox.setVgrow(
                analysisOutput,
                Priority.ALWAYS
        );

        outer.getChildren().addAll(
                heading,
                description,
                inputCard,
                outputCard
        );

        VBox.setVgrow(
                outputCard,
                Priority.ALWAYS
        );

        page.setCenter(outer);

        runAnalysis();

        return page;
    }

    private TextField analysisField(
            String value) {

        TextField field =
                new TextField(value);

        field.setPrefWidth(170);

        field.setStyle(
                "-fx-background-color: #071724; "
                + "-fx-text-fill: #e2eef8; "
                + "-fx-prompt-text-fill: #54718a; "
                + "-fx-border-color: #21435c; "
                + "-fx-border-radius: 6; "
                + "-fx-background-radius: 6;"
        );

        return field;
    }

    private void addAnalysisInput(
            GridPane grid,
            String labelText,
            TextField field,
            int col,
            int row) {

        VBox box =
                new VBox(5);

        Label label =
                sectionLabel(labelText);

        box.getChildren().addAll(
                label,
                field
        );

        grid.add(
                box,
                col,
                row
        );
    }

    private Label sectionLabel(
            String text) {

        Label label =
                new Label(text);

        label.setTextFill(
                Color.web("#6b8aa4")
        );

        label.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        9
                )
        );

        return label;
    }

    private void runAnalysis() {

        if (analysisOutput == null) {
            return;
        }

        try {

            double mass =
                    read(massField);

            double wingArea =
                    read(wingAreaField);

            double velocity =
                    read(velocityField);

            double altitude =
                    read(altitudeField);

            double cl =
                    read(clField);

            double cd0 =
                    read(cd0Field);

            double k =
                    read(kField);

            double thrust =
                    read(thrustField);

            double exhaustVelocity =
                    read(exhaustField);

            InputValidator.validateMass(
                    mass
            );

            InputValidator.validateWingArea(
                    wingArea
            );

            InputValidator.validateVelocity(
                    velocity
            );

            InputValidator.validateAltitude(
                    altitude
            );

            InputValidator.validateLiftCoefficient(
                    cl
            );

            InputValidator.validateCd0(
                    cd0
            );

            InputValidator.validateInducedDragFactor(
                    k
            );

            InputValidator.validatePositive(
                    thrust,
                    "Thrust"
            );

            InputValidator.validatePositive(
                    exhaustVelocity,
                    "Exhaust velocity"
            );

            Aircraft aircraft =
                    new Aircraft(
                            mass,
                            wingArea,
                            velocity,
                            altitude
                    );

            Atmosphere atmosphere =
                    new Atmosphere(
                            altitude
                    );

            Aerodynamics aero =
                    new Aerodynamics(
                            aircraft,
                            atmosphere,
                            cl,
                            cd0,
                            k
                    );

            Propulsion propulsion =
                    new Propulsion(
                            aircraft,
                            aero,
                            thrust,
                            exhaustVelocity
                    );

            PerformanceAnalyzer performance =
                    new PerformanceAnalyzer(
                            aircraft,
                            aero,
                            propulsion
                    );

            Optimizer optimizer =
                    new Optimizer(
                            mass * 9.80665,
                            wingArea,
                            atmosphere.getDensity(),
                            cd0,
                            k
                    );

            optimizer.optimize();

            StringBuilder out =
                    new StringBuilder();

            out.append(
                    "===== AEROMECH ANALYSIS =====\n\n"
            );

            out.append(
                    "ATMOSPHERE\n"
            );

            out.append(
                    "Temperature      : "
            )
            .append(
                    format(
                            atmosphere.getTemperature(),
                            2
                    )
            )
            .append(" K\n");

            out.append(
                    "Pressure         : "
            )
            .append(
                    format(
                            atmosphere.getPressure(),
                            2
                    )
            )
            .append(" Pa\n");

            out.append(
                    "Density          : "
            )
            .append(
                    format(
                            atmosphere.getDensity(),
                            4
                    )
            )
            .append(
                    " kg/m³\n\n"
            );

            out.append(
                    "AERODYNAMICS\n"
            );

            out.append(
                    "Velocity         : "
            )
            .append(
                    format(
                            velocity,
                            2
                    )
            )
            .append(
                    " m/s\n"
            );

            out.append(
                    "Dynamic pressure : "
            )
            .append(
                    format(
                            aero.getDynamicPressure(),
                            2
                    )
            )
            .append(
                    " Pa\n"
            );

            out.append(
                    "Lift             : "
            )
            .append(
                    format(
                            aero.getLift(),
                            2
                    )
            )
            .append(
                    " N\n"
            );

            out.append(
                    "Drag             : "
            )
            .append(
                    format(
                            aero.getDrag(),
                            2
                    )
            )
            .append(
                    " N\n"
            );

            out.append(
                    "L/D              : "
            )
            .append(
                    format(
                            aero.getLiftToDragRatio(),
                            4
                    )
            )
            .append(
                    "\n\n"
            );

            out.append(
                    "PROPULSION\n"
            );

            out.append(
                    "Thrust           : "
            )
            .append(
                    format(
                            thrust,
                            2
                    )
            )
            .append(
                    " N\n"
            );

            out.append(
                    "T/W              : "
            )
            .append(
                    format(
                            propulsion.getThrustToWeightRatio(),
                            4
                    )
            )
            .append(
                    "\n"
            );

            out.append(
                    "Acceleration     : "
            )
            .append(
                    format(
                            propulsion.getAcceleration(),
                            4
                    )
            )
            .append(
                    " m/s²\n"
            );

            out.append(
                    "Power            : "
            )
            .append(
                    format(
                            propulsion.getPropulsivePower(),
                            2
                    )
            )
            .append(
                    " W\n"
            );

            out.append(
                    "Mass flow        : "
            )
            .append(
                    format(
                            propulsion.getMassFlowRate(),
                            4
                    )
            )
            .append(
                    " kg/s\n"
            );

            out.append(
                    "Specific impulse : "
            )
            .append(
                    format(
                            propulsion.getSpecificImpulse(),
                            2
                    )
            )
            .append(
                    " s\n\n"
            );

            out.append(
                    "PERFORMANCE\n"
            );

            out.append(
                    "Weight           : "
            )
            .append(
                    format(
                            performance.getWeight(),
                            2
                    )
            )
            .append(
                    " N\n"
            );

            out.append(
                    "Excess thrust    : "
            )
            .append(
                    format(
                            performance.getExcessThrust(),
                            2
                    )
            )
            .append(
                    " N\n"
            );

            out.append(
                    "Power required   : "
            )
            .append(
                    format(
                            performance.getPowerRequired(),
                            2
                    )
            )
            .append(
                    " W\n"
            );

            out.append(
                    "Excess power     : "
            )
            .append(
                    format(
                            performance.getExcessPower(),
                            2
                    )
            )
            .append(
                    " W\n"
            );

            out.append(
                    "Rate of climb    : "
            )
            .append(
                    format(
                            performance.getExcessPower()
                                    / performance.getWeight(),
                            4
                    )
            )
            .append(
                    " m/s\n\n"
            );

            out.append(
                    "OPTIMIZATION\n"
            );

            out.append(
                    "Optimal CL       : "
            )
            .append(
                    format(
                            optimizer.getOptimalCL(),
                            6
                    )
            )
            .append(
                    "\n"
            );

            out.append(
                    "Optimal CD       : "
            )
            .append(
                    format(
                            optimizer.getOptimalCD(),
                            6
                    )
            )
            .append(
                    "\n"
            );

            out.append(
                    "Maximum L/D      : "
            )
            .append(
                    format(
                            optimizer.getMaximumLDRatio(),
                            6
                    )
            )
            .append(
                    "\n"
            );

            out.append(
                    "Optimal velocity : "
            )
            .append(
                    format(
                            optimizer.getOptimalVelocity(),
                            3
                    )
            )
            .append(
                    " m/s\n"
            );

            analysisOutput.setText(
                    out.toString()
            );

            statusLabel.setText(
                    "● ANALYSIS READY"
            );

            statusLabel.setTextFill(
                    Color.web("#6ee7a5")
            );

            footerLabel.setText(
                    "PHYSICS ENGINE   ● READY   MODEL: STANDARD ATMOSPHERE"
            );

        } catch (Exception ex) {

            analysisOutput.setText(
                    "INPUT ERROR\n\n"
                            + ex.getMessage()
            );

            statusLabel.setText(
                    "● INPUT ERROR"
            );

            statusLabel.setTextFill(
                    Color.web("#ef6b73")
            );
        }
    }

    private double read(
            TextField field) {

        return Double.parseDouble(
                field.getText().trim()
        );
    }

    private void rebuildSimulation() {

        if (thrustSlider == null) {
            return;
        }

        running = false;
        finished = false;

        lastFrameNanos = -1L;
        timeAccumulator = 0.0;

        double wingArea =
                wingAreaSlider.getValue();

        double altitude =
                altitudeSlider.getValue();

        double thrust =
                thrustSlider.getValue();

        double dt =
                timeStepSlider.getValue();

        double simTime =
                simulationTimeSlider.getValue();

        Aircraft aircraft =
                new Aircraft(
                        DEFAULT_MASS,
                        wingArea,
                        DEFAULT_VELOCITY,
                        altitude
                );

        simulation =
                new Simulation(
                        aircraft,
                        DEFAULT_CL,
                        DEFAULT_CD0,
                        DEFAULT_K,
                        thrust,
                        DEFAULT_EXHAUST_VELOCITY,
                        dt,
                        simTime
                );

        needsReset = false;

        refreshSliderLabels();

        /*
         * IMPORTANT FIX:
         *
         * Reset the visual first.
         * Then refreshSimulationUI(), which draws the aircraft
         * according to the actual starting altitude.
         *
         * Previously the order was reversed, so the correct
         * altitude position was immediately overwritten by
         * resetAircraftVisual().
         */
        resetAircraftVisual();

        refreshSimulationUI();

        statusLabel.setText(
                "● READY"
        );

        statusLabel.setTextFill(
                Color.web("#6ee7a5")
        );

        footerLabel.setText(
                "PHYSICS ENGINE   ● READY   MODEL: STANDARD ATMOSPHERE   STEP: "
                        + format(dt, 2)
                        + " s"
        );
    }

    private void playSimulation() {

        if (needsReset
                || simulation == null
                || finished) {

            rebuildSimulation();
        }

        running = true;
        finished = false;

        lastFrameNanos = -1L;

        statusLabel.setText(
                "● RUNNING"
        );

        statusLabel.setTextFill(
                Color.web("#69d7f0")
        );

        footerLabel.setText(
                "PHYSICS ENGINE   ● RUNNING   MODEL: STANDARD ATMOSPHERE   STEP: "
                        + format(
                                simulation.getTimeStep(),
                                2
                        )
                        + " s"
        );
    }

    private void pauseSimulation() {

        running = false;

        statusLabel.setText(
                "● PAUSED"
        );

        statusLabel.setTextFill(
                Color.web("#e9c46a")
        );

        footerLabel.setText(
                "PHYSICS ENGINE   ● PAUSED   MODEL: STANDARD ATMOSPHERE"
        );
    }

    private void resetSimulation() {
        rebuildSimulation();
    }

    private void refreshSliderLabels() {

        thrustSliderValue.setText(
                formatKn(
                        thrustSlider.getValue()
                )
        );

        wingAreaSliderValue.setText(
                format(
                        wingAreaSlider.getValue(),
                        2
                )
                + " m²"
        );

        altitudeSliderValue.setText(
                format(
                        altitudeSlider.getValue(),
                        0
                )
                + " m"
        );

        timeStepSliderValue.setText(
                format(
                        timeStepSlider.getValue(),
                        2
                )
                + " s"
        );

        simulationTimeSliderValue.setText(
                format(
                        simulationTimeSlider.getValue(),
                        1
                )
                + " s"
        );
    }

    private void refreshSimulationUI() {

        if (simulation == null) {
            return;
        }

        Simulation.State state =
                simulation.getState();

        timeValue.setText(
                format(
                        state.getTime(),
                        2
                )
                + " s"
        );

        distanceValue.setText(
                format(
                        state.getDistance(),
                        1
                )
                + " m"
        );

        velocityValue.setText(
                format(
                        state.getVelocity(),
                        2
                )
                + " m/s"
        );

        accelerationValue.setText(
                format(
                        state.getAcceleration(),
                        2
                )
                + " m/s²"
        );

        liftValue.setText(
                formatKn(
                        state.getLift()
                )
        );

        dragValue.setText(
                formatKn(
                        state.getDrag()
                )
        );

        thrustValue.setText(
                formatKn(
                        state.getThrust()
                )
        );

        twValue.setText(
                format(
                        state.getThrustToWeightRatio(),
                        3
                )
        );

        ldValue.setText(
                format(
                        state.getLiftToDragRatio(),
                        2
                )
        );

        altitudeHud.setText(
                "ALTITUDE   "
                        + format(
                                state.getAltitude(),
                                0
                        )
                        + " m"
        );

        updateAircraftVisual(
                state
        );
    }

    private void resetAircraftVisual() {

        if (aircraftGraphic == null
                || trajectory == null) {
            return;
        }

        lastAircraftX = 145;
        lastAircraftY = 195;
        visualAngle = 0.0;

        aircraftGraphic.setLayoutX(
                lastAircraftX
        );

        aircraftGraphic.setLayoutY(
                lastAircraftY
        );

        aircraftGraphic.setRotate(0);

        trajectory.getPoints().clear();

        trajectory.getPoints().addAll(
                lastAircraftX,
                lastAircraftY,
                lastAircraftX + 36,
                lastAircraftY
        );
    }

    private void updateAircraftVisual(
            Simulation.State state) {

        if (flightViewport == null
                || aircraftGraphic == null
                || trajectory == null) {

            return;
        }

        double width =
                flightViewport.getWidth();

        double height =
                flightViewport.getHeight();

        if (width <= 0) {
            width = 1280;
        }

        if (height <= 0) {
            height = 455;
        }

        double totalTime =
                Math.max(
                        simulation.getSimulationTime(),
                        1.0
                );

        double expectedDistance =
                Math.max(
                        900.0,
                        DEFAULT_VELOCITY
                                * totalTime
                                * 1.45
                );

        double normalizedDistance =
                Math.min(
                        1.0,
                        state.getDistance()
                                / expectedDistance
                );

        double travelWidth =
                Math.max(
                        500.0,
                        width - 260.0
                );

        double x =
                130
                        + normalizedDistance
                        * travelWidth;

        /*
         * =========================================================
         * ALTITUDE SLIDER / VISUAL FIX
         * =========================================================
         *
         * JavaFX screen coordinates work like this:
         *
         *       smaller Y = higher on screen
         *       larger Y  = lower on screen
         *
         * Therefore:
         *
         *       physical altitude increases
         *                  ↓
         *       screen Y must decrease
         *
         * The old code used:
         *
         *     state.getAltitude()
         *          - altitudeSlider.getValue()
         *
         * This made the aircraft position relative to its starting
         * slider altitude rather than its real absolute altitude.
         *
         * Now the actual simulation altitude is used directly.
         */

        double minAltitude = 0.0;
        double maxAltitude = 11000.0;

        double clampedAltitude =
                Math.max(
                        minAltitude,
                        Math.min(
                                maxAltitude,
                                state.getAltitude()
                        )
                );

        double altitudeProgress =
                (
                        clampedAltitude
                                - minAltitude
                )
                / (
                        maxAltitude
                                - minAltitude
                );

        /*
         * At 0 m:
         *     aircraft is near the horizon.
         *
         * At 11,000 m:
         *     aircraft is high in the sky.
         */

        double lowY =
                Math.min(
                        height - 115.0,
                        height * 0.70
                );

        double highY =
                Math.max(
                        88.0,
                        height * 0.18
                );

        double y =
                lowY
                        - altitudeProgress
                        * (lowY - highY);

        y =
                Math.max(
                        88.0,
                        Math.min(
                                height - 100.0,
                                y
                        )
                );

        /*
         * Convert rate of climb and velocity into
         * instantaneous flight-path angle.
         */

        double targetAngle =
                -Math.toDegrees(
                        Math.atan2(
                                state.getRateOfClimb(),
                                Math.max(
                                        state.getVelocity(),
                                        0.1
                                )
                        )
                );

        targetAngle =
                Math.max(
                        -18.0,
                        Math.min(
                                18.0,
                                targetAngle
                        )
                );

        double smoothing =
                1.0
                        - Math.exp(
                                -12.0
                                        * Math.max(
                                                simulation.getTimeStep(),
                                                0.01
                                        )
                        );

        visualAngle +=
                (
                        targetAngle
                                - visualAngle
                )
                * smoothing;

        aircraftGraphic.setLayoutX(x);
        aircraftGraphic.setLayoutY(y);
        aircraftGraphic.setRotate(
                visualAngle
        );

        trajectory.getPoints().addAll(
                x,
                y
        );

        lastAircraftX = x;
        lastAircraftY = y;
    }

    private String format(
            double value,
            int decimals) {

        return String.format(
                Locale.US,
                "%."
                        + decimals
                        + "f",
                value
        );
    }

    private String formatKn(
            double newtons) {

        return format(
                newtons / 1000.0,
                2
        )
                + " kN";
    }

    public static void main(
            String[] args) {

        launch(args);
    }

    /**
     * Small Pane wrapper used so the viewport
     * can be resized and scaled safely.
     */
    private static class PaneProxy
            extends javafx.scene.layout.Pane {

        private final double baseWidth;
        private final double baseHeight;

        PaneProxy(
                double baseWidth,
                double baseHeight) {

            this.baseWidth = baseWidth;
            this.baseHeight = baseHeight;

            setPrefHeight(
                    baseHeight
            );

            setMinHeight(350);
        }

        void clear() {
            getChildren().clear();
        }

        void addAll(
                Node... nodes) {

            getChildren().addAll(
                    nodes
            );
        }

        @Override
        protected void layoutChildren() {

            double w = getWidth();
            double h = getHeight();

            double sx =
                    w <= 0
                            ? 1.0
                            : w / baseWidth;

            double sy =
                    h <= 0
                            ? 1.0
                            : h / baseHeight;

            double s =
                    Math.min(
                            sx,
                            sy
                    );

            for (Node node :
                    getChildren()) {

                if (node instanceof Label
                        || Boolean.TRUE.equals(
                                node.getProperties().get(
                                        VIEWPORT_BACKGROUND
                                )
                        )) {

                    continue;
                }

                node.setScaleX(s);
                node.setScaleY(s);
            }
        }
    }
}