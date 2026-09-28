public class Simulation {

    private final double GRAVITY = 9.80665;

    private final Aircraft aircraft;
    private final double liftCoefficient;
    private final double zeroLiftDragCoefficient;
    private final double inducedDragFactor;
    private final double thrust;
    private final double exhaustVelocity;

    private double timeStep;
    private double simulationTime;

    private double time;
    private double velocity;
    private double distance;
    private double altitude;

    private double acceleration;
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
        if (timeStep <= 0) {
            throw new IllegalArgumentException("Time step must be greater than zero.");
        }
        if (simulationTime <= 0) {
            throw new IllegalArgumentException("Simulation time must be greater than zero.");
        }

        this.aircraft = aircraft;
        this.liftCoefficient = aerodynamics.getLiftCoefficient();

        // Preserve the exact aerodynamic model used to construct the simulation.
        this.zeroLiftDragCoefficient = aerodynamics.getZeroLiftDragCoefficient();
        this.inducedDragFactor = aerodynamics.getInducedDragFactor();
        this.thrust = propulsion.getThrust();
        this.exhaustVelocity = propulsion.getExhaustVelocity();

        this.timeStep = timeStep;
        this.simulationTime = simulationTime;

        reset();
    }

    /**
     * Convenience constructor for a simulation whose aerodynamic parameters
     * are explicitly supplied. This is the constructor recommended by the GUI.
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
        if (timeStep <= 0) {
            throw new IllegalArgumentException("Time step must be greater than zero.");
        }
        if (simulationTime <= 0) {
            throw new IllegalArgumentException("Simulation time must be greater than zero.");
        }

        this.aircraft = aircraft;
        this.liftCoefficient = liftCoefficient;
        this.zeroLiftDragCoefficient = zeroLiftDragCoefficient;
        this.inducedDragFactor = inducedDragFactor;
        this.thrust = thrust;
        this.exhaustVelocity = exhaustVelocity;
        this.timeStep = timeStep;
        this.simulationTime = simulationTime;

        reset();
    }

    /**
     * Runs the simulation to completion using the current timestep.
     */
    public void runSimulation() {
        while (!complete) {
            step();
        }
    }

    /**
     * Advances the simulation by one timestep and recalculates the
     * atmosphere, aerodynamic forces and propulsion quantities at that state.
     */
    public State step() {

        if (complete) {
            return getState();
        }

        double currentVelocity = Math.max(velocity, 0.1);

        aircraft.setVelocity(currentVelocity);
        aircraft.setAltitude(altitude);

        Atmosphere atmosphere = new Atmosphere(altitude);

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

        double mass = aircraft.getMass();
        double weight = mass * GRAVITY;

        dynamicPressure = aerodynamics.getDynamicPressure();
        lift = aerodynamics.getLift();
        drag = aerodynamics.getDrag();
        liftToDragRatio = aerodynamics.getLiftToDragRatio();
        thrustToWeightRatio = propulsion.getThrustToWeightRatio();

        double netForce = thrust - drag;
        acceleration = netForce / mass;
        rateOfClimb = (lift - weight) * currentVelocity / weight;

        double nextVelocity = currentVelocity + acceleration * timeStep;
        nextVelocity = Math.max(0.0, nextVelocity);

        distance += 0.5 * (currentVelocity + nextVelocity) * timeStep;
        velocity = nextVelocity;
        time += timeStep;

        if (time >= simulationTime) {
            time = simulationTime;
            complete = true;
        }

        aircraft.setVelocity(velocity);
        aircraft.setAltitude(altitude);

        return getState();
    }

    public void reset() {
        time = 0.0;
        distance = 0.0;
        velocity = aircraft.getVelocity();
        altitude = aircraft.getAltitude();
        acceleration = 0.0;
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
        double safeVelocity = Math.max(velocity, 0.1);

        aircraft.setVelocity(safeVelocity);
        aircraft.setAltitude(altitude);

        Atmosphere atmosphere = new Atmosphere(altitude);
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

        double mass = aircraft.getMass();
        double weight = mass * GRAVITY;

        dynamicPressure = aerodynamics.getDynamicPressure();
        lift = aerodynamics.getLift();
        drag = aerodynamics.getDrag();
        liftToDragRatio = aerodynamics.getLiftToDragRatio();
        thrustToWeightRatio = propulsion.getThrustToWeightRatio();
        acceleration = (thrust - drag) / mass;
        rateOfClimb = (lift - weight) * safeVelocity / weight;
    }

    public boolean isComplete() {
        return complete;
    }

    public double getTime() {
        return time;
    }

    public double getVelocity() {
        return velocity;
    }

    public double getDistance() {
        return distance;
    }

    public double getAltitude() {
        return altitude;
    }

    public double getAcceleration() {
        return acceleration;
    }

    public double getLift() {
        return lift;
    }

    public double getDrag() {
        return drag;
    }

    public double getThrust() {
        return thrust;
    }

    public double getThrustToWeightRatio() {
        return thrustToWeightRatio;
    }

    public double getLiftToDragRatio() {
        return liftToDragRatio;
    }

    public double getDynamicPressure() {
        return dynamicPressure;
    }

    public double getRateOfClimb() {
        return rateOfClimb;
    }

    public double getTimeStep() {
        return timeStep;
    }

    public double getSimulationTime() {
        return simulationTime;
    }

    public State getState() {
        return new State(
                time,
                distance,
                velocity,
                altitude,
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
