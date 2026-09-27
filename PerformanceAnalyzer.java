public class PerformanceAnalyzer {

    private static final double GRAVITY = 9.80665;

    // Connected engineering models
    private Aircraft aircraft;
    private Aerodynamics aerodynamics;
    private Propulsion propulsion;

    // Calculated performance values
    private double weight;
    private double excessThrust;
    private double thrustToWeightRatio;
    private double powerRequired;
    private double excessPower;
    private double rateOfClimb;
    private double liftToDragRatio;

    public PerformanceAnalyzer(
            Aircraft aircraft,
            Aerodynamics aerodynamics,
            Propulsion propulsion) {

        this.aircraft = aircraft;
        this.aerodynamics = aerodynamics;
        this.propulsion = propulsion;

        calculatePerformance();
    }

    private void calculatePerformance() {

        double mass = aircraft.getMass();
        double velocity = aircraft.getVelocity();

        double thrust = propulsion.getThrust();
        double drag = aerodynamics.getDrag();

        // Weight: W = mg
        weight = mass * GRAVITY;

        // Excess thrust: T - D
        excessThrust = thrust - drag;

        // Thrust-to-weight ratio
        thrustToWeightRatio = thrust / weight;

        // Power required to overcome drag: P = D * V
        powerRequired = drag * velocity;

        // Excess power: P_excess = (T - D) * V
        excessPower = excessThrust * velocity;

        // Rate of climb: ROC = P_excess / W
        rateOfClimb = excessPower / weight;
      
    }

    // Returns aircraft weight in Newtons
    public double getWeight() {
        return weight;
    }

    // Returns excess thrust in Newtons
    public double getExcessThrust() {
        return excessThrust;
    }

    // Returns thrust-to-weight ratio
    public double getThrustToWeightRatio() {
        return thrustToWeightRatio;
    }

    // Returns power required in Watts
    public double getPowerRequired() {
        return powerRequired;
    }

    // Returns excess power in Watts
    public double getExcessPower() {
        return excessPower;
    }

    // Returns rate of climb in m/s
    public double getRateOfClimb() {
        return rateOfClimb;
    }

}