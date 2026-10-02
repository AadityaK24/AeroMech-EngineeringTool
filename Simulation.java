public class Simulation {

    private static final double GRAVITY = 9.80665;
    private static final double MIN_LIFT_COEFFICIENT = 0.05;
    private static final double MAX_LIFT_COEFFICIENT = 1.80;

    private static final double MAX_CLIMB_ANGLE = Math.toRadians(14.0);
    private static final double MAX_DESCENT_ANGLE = Math.toRadians(5.5);
    private static final double MAX_ANGLE_RATE = Math.toRadians(7.5);

    private enum FlightMode {
        CUSTOM,
        TAKE_OFF,
        LANDING,
        CRUISE,
        HIGH_ALTITUDE
    }

    private final Aircraft aircraft;
    private final double baseLiftCoefficient;
    private final double zeroLiftDragCoefficient;
    private final double inducedDragFactor;
    private final double thrust;
    private final double exhaustVelocity;
    private final FlightMode flightMode;

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
    private double profileClimbAngle;

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
        this.flightMode = FlightMode.CUSTOM;
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

        this(
                aircraft,
                liftCoefficient,
                zeroLiftDragCoefficient,
                inducedDragFactor,
                thrust,
                exhaustVelocity,
                timeStep,
                simulationTime,
                "CUSTOM"
        );
    }

    /**
     * JavaFX constructor with an explicit flight profile.
     */
    public Simulation(
            Aircraft aircraft,
            double liftCoefficient,
            double zeroLiftDragCoefficient,
            double inducedDragFactor,
            double thrust,
            double exhaustVelocity,
            double timeStep,
            double simulationTime,
            String flightMode) {

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
        this.flightMode = parseFlightMode(flightMode);
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

        // Determine the target flight-path angle from the selected flight
        // profile and the user's actual airspeed, thrust, altitude and
        // aerodynamic inputs. Then move toward it smoothly.
        double previousFlightPathAngle = flightPathAngle;
        double targetFlightPathAngle = calculateTargetFlightPathAngle(
                time,
                previousVelocity,
                previousAltitude
        );

        double maxAngleChange = MAX_ANGLE_RATE * timeStep;
        double angleChange = clamp(
                targetFlightPathAngle - previousFlightPathAngle,
                -maxAngleChange,
                maxAngleChange
        );

        flightPathAngle = previousFlightPathAngle + angleChange;
        flightPathAngleRate = angleChange / Math.max(timeStep, 1.0e-9);

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

            // For landing-capable profiles, treat the final few metres as
            // touchdown so the visual run terminates on the runway rather
            // than hanging slightly above ground because of the finite step.
            if ((flightMode == FlightMode.LANDING
                    || flightMode == FlightMode.CUSTOM)
                    && altitude <= 35.0) {
                altitude = 0.0;
                flightPathAngle = 0.0;
                flightPathAngleRate = 0.0;
                rateOfClimb = 0.0;
            }

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
        flightPathAngle = 0.0;
        flightPathAngleRate = 0.0;
        acceleration = 0.0;
        liftCoefficient = baseLiftCoefficient;
        lift = 0.0;
        drag = 0.0;
        thrustToWeightRatio = 0.0;
        liftToDragRatio = 0.0;
        dynamicPressure = 0.0;
        rateOfClimb = 0.0;
        complete = false;

        profileClimbAngle = estimateClimbAngle(
                velocity,
                altitude
        );

        updateCurrentState();
    }


    /**
     * Returns the target flight-path angle for a future point in the run.
     * This is also used by Main.java to draw the projected path, so the
     * visualization and the physics use the same flight-profile rules.
     */
    public double getProjectedFlightPathAngle(
            double futureTime,
            double projectedVelocity,
            double projectedAltitude) {

        return calculateTargetFlightPathAngle(
                futureTime,
                Math.max(0.1, projectedVelocity),
                Math.max(0.0, projectedAltitude)
        );
    }

    private double calculateTargetFlightPathAngle(
            double queryTime,
            double currentVelocity,
            double currentAltitude) {

        double progress = clamp(
                queryTime / Math.max(simulationTime, 1.0e-9),
                0.0,
                1.0
        );

        switch (flightMode) {
            case TAKE_OFF:
                return takeOffAngle(
                        progress,
                        currentVelocity,
                        currentAltitude
                );

            case LANDING:
                return landingAngle(
                        queryTime,
                        currentVelocity,
                        currentAltitude
                );

            case CRUISE:
                return cruiseAngle(
                        progress
                );

            case HIGH_ALTITUDE:
                return highAltitudeAngle(
                        progress,
                        currentVelocity,
                        currentAltitude
                );

            case CUSTOM:
            default:
                return customMissionAngle(
                        queryTime,
                        progress,
                        currentVelocity,
                        currentAltitude
                );
        }
    }

    private double takeOffAngle(
            double progress,
            double currentVelocity,
            double currentAltitude) {

        // Short runway / rotation phase.
        if (progress < 0.08) {
            return Math.toRadians(0.5);
        }

        // Climb using the user's propulsion and aerodynamic inputs.
        if (progress < 0.30) {
            double climbAngle = profileClimbAngle;
            double local = smoothStep(
                    (progress - 0.08) / 0.22
            );
            return climbAngle * local;
        }

        // Stabilization: smoothly return to level flight.
        if (progress < 0.50) {
            double climbAngle = profileClimbAngle;
            double local = smoothStep(
                    (progress - 0.30) / 0.20
            );
            return climbAngle * (1.0 - local);
        }

        return 0.0;
    }

    private double landingAngle(
            double queryTime,
            double currentVelocity,
            double currentAltitude) {

        if (currentAltitude <= 0.5) {
            return 0.0;
        }

        /*
         * Planned, shallow approach. Instead of tying the descent to a
         * percentage of the total run, calculate the angle required to
         * reach the flare zone during the time that remains. This makes
         * 2, 3, 4 and 5 minute landings use the same natural approach
         * geometry instead of producing an end-of-run dive or stopping
         * above the runway.
         */
        final double flareAltitude = 35.0;
        final double flareTime = 12.0;
        final double cruiseMinimumAngle = Math.toRadians(1.5);

        if (currentAltitude <= flareAltitude) {
            // Hold a small, progressively shallower approach through the
            // flare instead of reducing the descent rate to almost zero
            // hundreds of metres above the runway.
            double altitudeFactor = clamp(
                    currentAltitude / flareAltitude,
                    0.0,
                    1.0
            );

            double flareAngle =
                    Math.toRadians(1.0)
                    + Math.toRadians(3.2) * altitudeFactor;

            return -flareAngle;
        }

        double remainingTime =
                Math.max(1.0, simulationTime - queryTime);

        double usableTime =
                Math.max(1.0, remainingTime - flareTime);

        double velocity = Math.max(
                currentVelocity,
                20.0
        );

        // Required average descent rate to reach the flare altitude.
        double requiredRate =
                Math.max(
                        0.0,
                        (currentAltitude - flareAltitude)
                                / usableTime
                );

        double angleMagnitude =
                Math.asin(
                        clamp(
                                requiredRate / velocity,
                                0.0,
                                Math.sin(MAX_DESCENT_ANGLE)
                        )
                );

        // Prevent an almost-flat descent during very long runs. Once the
        // aircraft is committed to the approach, keep it visibly stable
        // rather than making it appear frozen in level flight.
        if (angleMagnitude > 0.0
                && angleMagnitude < cruiseMinimumAngle
                && currentAltitude < 1500.0) {
            angleMagnitude = cruiseMinimumAngle;
        }

        return -angleMagnitude;
    }

    private double naturalLandingAngle(
            double currentVelocity,
            double currentAltitude) {

        // Keep the approach shallow and stable. User speed and altitude
        // determine when descent begins, while the approach angle itself
        // stays within a natural landing envelope.
        double angleDeg = 4.0;

        return -Math.toRadians(angleDeg);
    }

    private double cruiseAngle(double progress) {
        // Slight leveling transition, then stable cruise.
        if (progress < 0.10) {
            return Math.toRadians(2.0)
                    * (1.0 - smoothStep(progress / 0.10));
        }
        return 0.0;
    }

    private double highAltitudeAngle(
            double progress,
            double currentVelocity,
            double currentAltitude) {

        // High-altitude profile climbs briefly, stabilizes, then remains
        // near level for the remainder of the simulation.
        if (progress < 0.20) {
            double climbAngle = Math.min(
                    profileClimbAngle,
                    Math.toRadians(8.0)
            );
            return climbAngle
                    * smoothStep(progress / 0.20);
        }

        if (progress < 0.35) {
            double climbAngle = Math.min(
                    profileClimbAngle,
                    Math.toRadians(8.0)
            );
            return climbAngle
                    * (1.0 - smoothStep((progress - 0.20) / 0.15));
        }

        return 0.0;
    }

    private double customMissionAngle(
            double queryTime,
            double progress,
            double currentVelocity,
            double currentAltitude) {

        // 1. Take-off / rotation.
        if (progress < 0.07) {
            return Math.toRadians(0.5);
        }

        // 2. User-responsive climb.
        if (progress < 0.28) {
            double climbAngle = profileClimbAngle;

            return climbAngle
                    * smoothStep((progress - 0.07) / 0.21);
        }

        // 3. Stabilize at the top of the climb.
        if (progress < 0.43) {
            double climbAngle = profileClimbAngle;

            return climbAngle
                    * (1.0 - smoothStep((progress - 0.28) / 0.15));
        }

        // 4. Cruise.
        if (progress < 0.60) {
            return 0.0;
        }

        // 5. Use the same planned, shallow landing approach as the
        // dedicated Landing profile. This keeps Custom proportional to
        // the chosen inputs while guaranteeing the visual and physics
        // projections follow the same descent logic.
        if (currentAltitude > 0.5) {
            return landingAngle(
                    queryTime,
                    currentVelocity,
                    currentAltitude
            );
        }

        return 0.0;
    }

    /**
     * Estimates how much climb angle the current user inputs can support.
     * More available thrust produces a larger climb angle; higher drag,
     * larger wing drag and less favorable CL reduce it.
     */
    private double estimateClimbAngle(
            double currentVelocity,
            double currentAltitude) {

        double velocity = Math.max(
                currentVelocity,
                0.1
        );

        double altitude = clamp(
                currentAltitude,
                0.0,
                11000.0
        );

        Atmosphere atmosphere = new Atmosphere(altitude);
        double density = atmosphere.getDensity();
        double wingArea = aircraft.getWingArea();
        double mass = aircraft.getMass();
        double weight = mass * GRAVITY;

        double q =
                0.5
                * density
                * velocity
                * velocity;

        double dragCoefficient =
                zeroLiftDragCoefficient
                + inducedDragFactor
                * baseLiftCoefficient
                * baseLiftCoefficient;

        double drag =
                q
                * wingArea
                * dragCoefficient;

        double excessThrust =
                thrust
                - drag;

        if (excessThrust <= 0.0) {
            return 0.0;
        }

        // Let CL influence the usable climb performance without allowing
        // unrealistic values to dominate the simulation.
        double liftEffect = clamp(
                baseLiftCoefficient / 0.82,
                0.70,
                1.30
        );

        double climbRatio = clamp(
                (excessThrust / weight)
                * liftEffect,
                0.0,
                Math.sin(MAX_CLIMB_ANGLE)
        );

        return Math.asin(climbRatio);
    }

    private double smoothStep(double x) {
        x = clamp(x, 0.0, 1.0);
        return x * x * (3.0 - 2.0 * x);
    }

    private FlightMode parseFlightMode(String mode) {
        if (mode == null) {
            return FlightMode.CUSTOM;
        }

        switch (mode.trim().toUpperCase()) {
            case "TAKE-OFF":
            case "TAKEOFF":
                return FlightMode.TAKE_OFF;

            case "LANDING":
                return FlightMode.LANDING;

            case "CRUISE":
                return FlightMode.CRUISE;

            case "HIGH ALTITUDE":
            case "HIGH_ALTITUDE":
                return FlightMode.HIGH_ALTITUDE;

            case "CUSTOM":
            default:
                return FlightMode.CUSTOM;
        }
    }

    private double clamp(
            double value,
            double min,
            double max) {

        return Math.max(min, Math.min(max, value));
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
    public String getFlightMode() { return flightMode.name(); }

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
