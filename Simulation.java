public class Simulation {

    private static final double GRAVITY = 9.80665;

    // Connected engineering models
    private Aircraft aircraft;
    private Aerodynamics aerodynamics;
    private Propulsion propulsion;

    // Simulation settings
    private double timeStep;
    private double simulationTime;

    // Simulation state
    private double time;
    private double velocity;
    private double distance;

    public Simulation(
            Aircraft aircraft,
            Aerodynamics aerodynamics,
            Propulsion propulsion,
            double timeStep,
            double simulationTime) {

        this.aircraft = aircraft;
        this.aerodynamics = aerodynamics;
        this.propulsion = propulsion;
        this.timeStep = timeStep;
        this.simulationTime = simulationTime;

        this.velocity = aircraft.getVelocity();
        this.distance = 0;
        this.time = 0;
    }

    public void runSimulation() {

        double mass = aircraft.getMass();
        double thrust = propulsion.getThrust();
        double drag = aerodynamics.getDrag();

        // Net force along the direction of flight
        double netForce = thrust - drag;

        // Constant acceleration for the first simulation model
        double acceleration = netForce / mass;

        while (time < simulationTime) {

            // Numerical integration
            distance = distance + velocity * timeStep;

            // Numerical integration
            velocity = velocity + acceleration * timeStep;

            time = time + timeStep;
        }
    }

    // Returns final simulation time in seconds
    public double getTime() {
        return time;
    }

    // Returns final velocity in m/s
    public double getVelocity() {
        return velocity;
    }

    // Returns distance travelled in metres
    public double getDistance() {
        return distance;
    }

    // Returns acceleration used in the simulation
    public double getAcceleration() {

        double netForce =
                propulsion.getThrust()
                - aerodynamics.getDrag();

        return netForce / aircraft.getMass();
    }
}