public class Simulation {

    private static final double GRAVITY = 9.80665;
    private static final double MAX_FLIGHT_PATH_ANGLE = Math.toRadians(8.0);
    private static final double FINAL_FLIGHT_PATH_ANGLE = Math.toRadians(-6.0);
    private static final double MIN_LIFT_COEFFICIENT = 0.05;
    private static final double MAX_LIFT_COEFFICIENT = 1.80;

    private final Aircraft aircraft;
    private final double baseLiftCoefficient;
    private final double zeroLiftDragCoefficient;
    private final double inducedDragFactor;
    private final double thrust;
    private final double exhaustVelocity;

    private final double initialAltitude;

    private double timeStep;
    private double simulationTime;

    private double time;
    private double velocity;
    private double distance;
    private double altitude;
    private double flightPathAngle;
    private double flightPathAngleRate;

    private double acceleration;
    private double liftCoefficient;
    private double lift;
    private double drag;
    private double thrustToWeightRatio;
    private double liftToDragRatio;
    private double dynamicPressure;
    private double rateOfClimb;

    private boolean complete;

    public Simulation(
            Aircraft aircraft,
            Aerodynamics aerodynamics,
            Propulsion propulsion,
            double timeStep,
            double simulationTime) {

        if (aircraft == null || aerodynamics == null || propulsion == null) {
            throw new IllegalArgumentException("Simulation inputs cannot be null.");
        }
        validateTiming(timeStep, simulationTime);

        this.aircraft = aircraft;
        this.baseLiftCoefficient = aerodynamics.getLiftCoefficient();
        this.zeroLiftDragCoefficient = aerodynamics.getZeroLiftDragCoefficient();
        this.inducedDragFactor = aerodynamics.getInducedDragFactor();
        this.thrust = propulsion.getThrust();
        this.exhaustVelocity = propulsion.getExhaustVelocity();
        this.initialAltitude = Math.max(0.0, aircraft.getAltitude());
        this.timeStep = timeStep;
        this.simulationTime = simulationTime;

        reset();
    }

    /**
     * Convenience constructor used by the JavaFX simulator.
     */
    public Simulation(
            Aircraft aircraft,
            double liftCoefficient,
            double zeroLiftDragCoefficient,
            double inducedDragFactor,
            double thrust,
            double exhaustVelocity,
            double timeStep,
            double simulationTime) {

        if (aircraft == null) {
            throw new IllegalArgumentException("Aircraft cannot be null.");
        }
        validateTiming(timeStep, simulationTime);

        this.aircraft = aircraft;
        this.baseLiftCoefficient = liftCoefficient;
        this.zeroLiftDragCoefficient = zeroLiftDragCoefficient;
        this.inducedDragFactor = inducedDragFactor;
        this.thrust = thrust;
        this.exhaustVelocity = exhaustVelocity;
        this.initialAltitude = Math.max(0.0, aircraft.getAltitude());
        this.timeStep = timeStep;
        this.simulationTime = simulationTime;

        reset();
    }

    private static void validateTiming(double timeStep, double simulationTime) {
        if (timeStep <= 0.0 || Double.isNaN(timeStep) || Double.isInfinite(timeStep)) {
            throw new IllegalArgumentException("Time step must be greater than zero.");
        }
        if (simulationTime <= 0.0 || Double.isNaN(simulationTime) || Double.isInfinite(simulationTime)) {
            throw new IllegalArgumentException("Simulation time must be greater than zero.");
        }
    }

    public void runSimulation() {
        while (!complete) {
            step();
        }
    }

    /**
     * Advances a 2D point-mass flight model by one timestep.
     *
     * The flight-path angle is a smooth commanded flight profile. Lift is not
     * artificially reduced for display; instead the normal-force equation
     * determines the CL required to follow that path:
     *
     *     L = m V d(gamma)/dt + W cos(gamma)
     *
     * The longitudinal equation then determines the speed change:
     *
     *     m dV/dt = T - D - W sin(gamma)
     *
     * Altitude and horizontal distance are integrated from the resulting
     * flight-path angle and velocity.
     */
    public State step() {

        if (complete) {
            return getState();
        }

        double previousVelocity = Math.max(velocity, 0.1);
        double previousAltitude = altitude;

        // Smoothly move from a positive climb angle to a negative descent angle.
        // This gives the simulator a real, continuous flight path rather than
        // a purely visual arc.
        double progress = simulationTime <= 0.0
                ? 1.0
                : Math.min(1.0, time / simulationTime);

        flightPathAngle = MAX_FLIGHT_PATH_ANGLE
                + (FINAL_FLIGHT_PATH_ANGLE - MAX_FLIGHT_PATH_ANGLE) * progress;

        flightPathAngleRate =
                (FINAL_FLIGHT_PATH_ANGLE - MAX_FLIGHT_PATH_ANGLE)
                / simulationTime;

        // Recalculate the atmosphere at the current altitude.
        aircraft.setVelocity(previousVelocity);
        aircraft.setAltitude(previousAltitude);
        Atmosphere atmosphere = new Atmosphere(Math.max(0.0, Math.min(11000.0, previousAltitude)));

        double mass = aircraft.getMass();
        double wingArea = aircraft.getWingArea();
        double weight = mass * GRAVITY;
        double density = atmosphere.getDensity();
        double dynamicPressureAtCurrentState =
                0.5 * density * previousVelocity * previousVelocity;

        // Required normal force to follow the commanded flight path.
        double requiredLift =
                mass * previousVelocity * flightPathAngleRate
                + weight * Math.cos(flightPathAngle);

        requiredLift = Math.max(0.0, requiredLift);

        double requiredCL;
        if (dynamicPressureAtCurrentState > 1.0e-9 && wingArea > 0.0) {
            requiredCL = requiredLift
                    / (dynamicPressureAtCurrentState * wingArea);
        } else {
            requiredCL = baseLiftCoefficient;
        }

        // Keep the model inside a broad, usable aerodynamic envelope.
        // The default case remains well inside these limits.
        liftCoefficient = Math.max(
                MIN_LIFT_COEFFICIENT,
                Math.min(MAX_LIFT_COEFFICIENT, requiredCL)
        );

        aircraft.setVelocity(previousVelocity);
        Aerodynamics currentAerodynamics = new Aerodynamics(
                aircraft,
                atmosphere,
                liftCoefficient,
                zeroLiftDragCoefficient,
                inducedDragFactor
        );

        Propulsion currentPropulsion = new Propulsion(
                aircraft,
                currentAerodynamics,
                thrust,
                exhaustVelocity
        );

        dynamicPressure = currentAerodynamics.getDynamicPressure();
        lift = currentAerodynamics.getLift();
        drag = currentAerodynamics.getDrag();
        liftToDragRatio = currentAerodynamics.getLiftToDragRatio();
        thrustToWeightRatio = currentPropulsion.getThrustToWeightRatio();

        // Longitudinal point-mass equation of motion.
        acceleration =
                (thrust
                - drag
                - weight * Math.sin(flightPathAngle))
                / mass;

        double nextVelocity =
                Math.max(0.1, previousVelocity + acceleration * timeStep);

        // Integrate along the flight path.
        distance +=
                0.5
                * (previousVelocity + nextVelocity)
                * Math.cos(flightPathAngle)
                * timeStep;

        altitude +=
                0.5
                * (previousVelocity + nextVelocity)
                * Math.sin(flightPathAngle)
                * timeStep;

        // Do not allow the aircraft to travel below the ground plane.
        if (altitude < 0.0) {
            altitude = 0.0;
        }

        velocity = nextVelocity;
        time += timeStep;

        if (time >= simulationTime) {
            time = simulationTime;
            complete = true;
        }

        rateOfClimb = velocity * Math.sin(flightPathAngle);

        aircraft.setVelocity(velocity);
        aircraft.setAltitude(altitude);

        return getState();
    }

    public void reset() {
        time = 0.0;
        distance = 0.0;
        velocity = Math.max(0.1, aircraft.getVelocity());
        altitude = initialAltitude;
        flightPathAngle = MAX_FLIGHT_PATH_ANGLE;
        flightPathAngleRate =
                (FINAL_FLIGHT_PATH_ANGLE - MAX_FLIGHT_PATH_ANGLE)
                / simulationTime;
        acceleration = 0.0;
        liftCoefficient = baseLiftCoefficient;
        lift = 0.0;
        drag = 0.0;
        thrustToWeightRatio = 0.0;
        liftToDragRatio = 0.0;
        dynamicPressure = 0.0;
        rateOfClimb = 0.0;
        complete = false;

        updateCurrentState();
    }

    private void updateCurrentState() {
        aircraft.setVelocity(Math.max(0.1, velocity));
        aircraft.setAltitude(altitude);

        Atmosphere atmosphere = new Atmosphere(Math.max(0.0, Math.min(11000.0, altitude)));
        double mass = aircraft.getMass();
        double wingArea = aircraft.getWingArea();
        double weight = mass * GRAVITY;
        double q = 0.5 * atmosphere.getDensity() * velocity * velocity;

        double requiredLift =
                mass * velocity * flightPathAngleRate
                + weight * Math.cos(flightPathAngle);

        requiredLift = Math.max(0.0, requiredLift);

        if (q > 1.0e-9 && wingArea > 0.0) {
            liftCoefficient = Math.max(
                    MIN_LIFT_COEFFICIENT,
                    Math.min(MAX_LIFT_COEFFICIENT,
                            requiredLift / (q * wingArea))
            );
        } else {
            liftCoefficient = baseLiftCoefficient;
        }

        Aerodynamics aerodynamics = new Aerodynamics(
                aircraft,
                atmosphere,
                liftCoefficient,
                zeroLiftDragCoefficient,
                inducedDragFactor
        );

        Propulsion propulsion = new Propulsion(
                aircraft,
                aerodynamics,
                thrust,
                exhaustVelocity
        );

        dynamicPressure = aerodynamics.getDynamicPressure();
        lift = aerodynamics.getLift();
        drag = aerodynamics.getDrag();
        liftToDragRatio = aerodynamics.getLiftToDragRatio();
        thrustToWeightRatio = propulsion.getThrustToWeightRatio();

        acceleration =
                (thrust
                - drag
                - weight * Math.sin(flightPathAngle))
                / mass;

        rateOfClimb = velocity * Math.sin(flightPathAngle);
    }

    public boolean isComplete() { return complete; }
    public double getTime() { return time; }
    public double getVelocity() { return velocity; }
    public double getDistance() { return distance; }
    public double getAltitude() { return altitude; }
    public double getAcceleration() { return acceleration; }
    public double getLift() { return lift; }
    public double getDrag() { return drag; }
    public double getThrust() { return thrust; }
    public double getThrustToWeightRatio() { return thrustToWeightRatio; }
    public double getLiftToDragRatio() { return liftToDragRatio; }
    public double getDynamicPressure() { return dynamicPressure; }
    public double getRateOfClimb() { return rateOfClimb; }
    public double getTimeStep() { return timeStep; }
    public double getSimulationTime() { return simulationTime; }
    public double getFlightPathAngle() { return flightPathAngle; }
    public double getLiftCoefficient() { return liftCoefficient; }

    public State getState() {
        return new State(
                time,
                distance,
                velocity,
                altitude,
                flightPathAngle,
                liftCoefficient,
                acceleration,
                lift,
                drag,
                thrust,
                thrustToWeightRatio,
                liftToDragRatio,
                dynamicPressure,
                rateOfClimb
        );
    }

    public static class State {

        private final double time;
        private final double distance;
        private final double velocity;
        private final double altitude;
        private final double flightPathAngle;
        private final double liftCoefficient;
        private final double acceleration;
        private final double lift;
        private final double drag;
        private final double thrust;
        private final double thrustToWeightRatio;
        private final double liftToDragRatio;
        private final double dynamicPressure;
        private final double rateOfClimb;

        public State(
                double time,
                double distance,
                double velocity,
                double altitude,
                double flightPathAngle,
                double liftCoefficient,
                double acceleration,
                double lift,
                double drag,
                double thrust,
                double thrustToWeightRatio,
                double liftToDragRatio,
                double dynamicPressure,
                double rateOfClimb) {

            this.time = time;
            this.distance = distance;
            this.velocity = velocity;
            this.altitude = altitude;
            this.flightPathAngle = flightPathAngle;
            this.liftCoefficient = liftCoefficient;
            this.acceleration = acceleration;
            this.lift = lift;
            this.drag = drag;
            this.thrust = thrust;
            this.thrustToWeightRatio = thrustToWeightRatio;
            this.liftToDragRatio = liftToDragRatio;
            this.dynamicPressure = dynamicPressure;
            this.rateOfClimb = rateOfClimb;
        }

        public double getTime() { return time; }
        public double getDistance() { return distance; }
        public double getVelocity() { return velocity; }
        public double getAltitude() { return altitude; }
        public double getFlightPathAngle() { return flightPathAngle; }
        public double getLiftCoefficient() { return liftCoefficient; }
        public double getAcceleration() { return acceleration; }
        public double getLift() { return lift; }
        public double getDrag() { return drag; }
        public double getThrust() { return thrust; }
        public double getThrustToWeightRatio() { return thrustToWeightRatio; }
        public double getLiftToDragRatio() { return liftToDragRatio; }
        public double getDynamicPressure() { return dynamicPressure; }
        public double getRateOfClimb() { return rateOfClimb; }
    }
}
