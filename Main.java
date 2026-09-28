import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    private TextField mass, area, velocity, altitude, cl, cd0, k, thrust, exhaust;
    private TextArea output;

    @Override
    public void start(Stage stage) {

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);

        mass = field("5000");
        area = field("16.2");
        velocity = field("77.22");
        altitude = field("0");
        cl = field("0.828729");
        cd0 = field("0.025");
        k = field("0.0364011111");
        thrust = field("10000");
        exhaust = field("2500");

        add(grid, "Mass (kg)", mass, 0);
        add(grid, "Wing Area (m²)", area, 1);
        add(grid, "Velocity (m/s)", velocity, 2);
        add(grid, "Altitude (m)", altitude, 3);
        add(grid, "Lift Coefficient", cl, 4);
        add(grid, "CD0", cd0, 5);
        add(grid, "Induced Factor", k, 6);
        add(grid, "Thrust (N)", thrust, 7);
        add(grid, "Exhaust Velocity (m/s)", exhaust, 8);

        Button run = new Button("RUN ANALYSIS");
        run.setOnAction(e -> runAnalysis());

        output = new TextArea();
        output.setEditable(false);
        output.setPrefRowCount(22);

        VBox root = new VBox(
                15,
                new Label("AeroMech Engineering Tool"),
                new Label("Aerospace & Mechatronics Engineering Simulator"),
                grid,
                run,
                output
        );

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));

        stage.setTitle("AeroMech Engineering Tool");
        stage.setScene(new Scene(root, 750, 800));
        stage.show();
    }

    private void runAnalysis() {

        try {
            double m = value(mass);
            double s = value(area);
            double v = value(velocity);
            double h = value(altitude);
            double CL = value(cl);
            double CD0 = value(cd0);
            double K = value(k);
            double T = value(thrust);
            double Ve = value(exhaust);

            InputValidator.validateMass(m);
            InputValidator.validateWingArea(s);
            InputValidator.validateAltitude(h);

            Atmosphere atmosphere = new Atmosphere(h);

            Aircraft aircraft =
                    new Aircraft(m, s, v, h);

            Aerodynamics aero =
                    new Aerodynamics(
                            aircraft,
                            atmosphere,
                            CL,
                            CD0,
                            K
                    );

            Propulsion propulsion =
                    new Propulsion(
                            aircraft,
                            aero,
                            T,
                            Ve
                    );

            PerformanceAnalyzer performance =
                    new PerformanceAnalyzer(
                            aircraft,
                            aero,
                            propulsion
                    );

            Optimizer optimizer =
                    new Optimizer(
                            performance.getWeight(),
                            s,
                            atmosphere.getDensity(),
                            CD0,
                            K
                    );

            optimizer.optimize();

            Simulation simulation =
                    new Simulation(
                            aircraft,
                            aero,
                            propulsion,
                            0.1,
                            20
                    );

            simulation.runSimulation();

            output.setText(String.format(
                    """
                    ===== AEROMECH ANALYSIS =====

                    ATMOSPHERE
                    Temperature : %.2f K
                    Pressure    : %.2f Pa
                    Density     : %.4f kg/m³

                    AERODYNAMICS
                    Velocity    : %.2f m/s
                    Lift        : %.2f N
                    Drag        : %.2f N
                    L/D         : %.4f

                    PROPULSION
                    Thrust      : %.2f N
                    T/W         : %.4f
                    Acceleration: %.4f m/s²
                    Power       : %.2f W

                    PERFORMANCE
                    Weight      : %.2f N
                    Excess Thrust: %.2f N
                    Rate of Climb: %.4f m/s

                    OPTIMIZATION
                    Optimal CL  : %.6f
                    Optimal CD  : %.6f
                    Maximum L/D : %.6f
                    Optimal V   : %.3f m/s

                    SIMULATION
                    Time        : %.1f s
                    Final V     : %.3f m/s
                    Distance    : %.3f m
                    """,

                    atmosphere.getTemperature(),
                    atmosphere.getPressure(),
                    atmosphere.getDensity(),

                    v,
                    aero.getLift(),
                    aero.getDrag(),
                    aero.getLiftToDragRatio(),

                    T,
                    propulsion.getThrustToWeightRatio(),
                    propulsion.getAcceleration(),
                    propulsion.getPropulsivePower(),

                    performance.getWeight(),
                    performance.getExcessThrust(),
                    performance.getRateOfClimb(),

                    optimizer.getOptimalCL(),
                    optimizer.getOptimalCD(),
                    optimizer.getMaximumLDRatio(),
                    optimizer.getOptimalVelocity(),

                    simulation.getTime(),
                    simulation.getVelocity(),
                    simulation.getDistance()
            ));

        } catch (Exception e) {

            output.setText(
                    "ERROR: " + e.getMessage()
            );
        }
    }

    private TextField field(String value) {
        return new TextField(value);
    }

    private void add(
            GridPane grid,
            String name,
            TextField field,
            int row) {

        grid.add(new Label(name), 0, row);
        grid.add(field, 1, row);
    }

    private double value(TextField field) {
        return Double.parseDouble(
                field.getText().trim()
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}