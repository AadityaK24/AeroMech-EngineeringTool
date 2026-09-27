public class ParameterSweep {

    // Existing aircraft model used as the base configuration
    private Aircraft aircraft;

    // Atmospheric conditions used for every sweep point
    private Atmosphere atmosphere;

    // Aerodynamic coefficients used by the aerodynamic model
    private double liftCoefficient;
    private double zeroLiftDragCoefficient;
    private double inducedDragFactor;

    // Velocity range and step size for the sweep
    private double startVelocity;
    private double endVelocity;
    private double velocityStep;

    // Arrays used to store the results of each sweep point
    private double[] velocities;
    private double[] liftValues;
    private double[] dragValues;
    private double[] liftToDragRatios;


    // Creates a parameter sweep using the supplied aircraft and aerodynamic settings
    public ParameterSweep(
            Aircraft aircraft,
            Atmosphere atmosphere,
            double liftCoefficient,
            double zeroLiftDragCoefficient,
            double inducedDragFactor,
            double startVelocity,
            double endVelocity,
            double velocityStep) {

        // Store the aircraft model
        this.aircraft = aircraft;

        // Store the atmospheric model
        this.atmosphere = atmosphere;

        // Store the aerodynamic coefficients
        this.liftCoefficient = liftCoefficient;
        this.zeroLiftDragCoefficient = zeroLiftDragCoefficient;
        this.inducedDragFactor = inducedDragFactor;

        // Store the velocity sweep limits
        this.startVelocity = startVelocity;
        this.endVelocity = endVelocity;
        this.velocityStep = velocityStep;

        // Automatically perform the sweep when the object is created
        runSweep();
    }


    // Performs the aerodynamic calculations for every velocity in the selected range
    private void runSweep() {

        // Calculate how many velocity points will be tested
        int numberOfPoints =
                (int) Math.floor(
                        (endVelocity - startVelocity)
                        / velocityStep
                ) + 1;

        // Create arrays to store the sweep results
        velocities = new double[numberOfPoints];
        liftValues = new double[numberOfPoints];
        dragValues = new double[numberOfPoints];
        liftToDragRatios = new double[numberOfPoints];


        // Repeat the aerodynamic calculation for every velocity point
        for (int i = 0; i < numberOfPoints; i++) {

            // Calculate the velocity for the current sweep point
            double velocity =
                    startVelocity
                    + i * velocityStep;


            // Create a temporary aircraft with the current test velocity
            Aircraft testAircraft =
                    new Aircraft(
                            aircraft.getMass(),
                            aircraft.getWingArea(),
                            velocity,
                            aircraft.getAltitude()
                    );


            // Calculate aerodynamics at the current velocity
            Aerodynamics aerodynamics =
                    new Aerodynamics(
                            testAircraft,
                            atmosphere,
                            liftCoefficient,
                            zeroLiftDragCoefficient,
                            inducedDragFactor
                    );


            // Store the current velocity
            velocities[i] = velocity;

            // Store the calculated lift
            liftValues[i] = aerodynamics.getLift();

            // Store the calculated drag
            dragValues[i] = aerodynamics.getDrag();

            // Store the calculated lift-to-drag ratio
            liftToDragRatios[i] =
                    aerodynamics.getLiftToDragRatio();
        }
    }


    // Returns all velocities tested during the sweep
    public double[] getVelocities() {
        return velocities;
    }


    // Returns the lift calculated at each velocity
    public double[] getLiftValues() {
        return liftValues;
    }


    // Returns the drag calculated at each velocity
    public double[] getDragValues() {
        return dragValues;
    }


    // Returns the lift-to-drag ratio calculated at each velocity
    public double[] getLiftToDragRatios() {
        return liftToDragRatios;
    }


    // Returns the total number of velocity points tested
    public int getNumberOfPoints() {
        return velocities.length;
    }
}