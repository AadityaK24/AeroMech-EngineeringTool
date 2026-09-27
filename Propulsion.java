
public class Propulsion {

    private static final double GRAVITY = 9.80665;

    // Aircraft and aerodynamic data
    private Aircraft aircraft;
    private Aerodynamics aerodynamics;

    // Engine parameters
    private double thrust;
    private double exhaustVelocity;
    private double specificImpulse;

    // Calculated propulsion values
    private double thrustToWeightRatio;
    private double netForce;
    private double acceleration;
    private double propulsivePower;
    private double massFlowRate;

    public Propulsion(
            Aircraft aircraft,
            Aerodynamics aerodynamics,
            double thrust,
            double exhaustVelocity) {

        this.aircraft = aircraft;
        this.aerodynamics = aerodynamics;
        this.thrust = thrust;
        this.exhaustVelocity = exhaustVelocity;

        calculatePropulsion();
    }

    private void calculatePropulsion() {

        double mass = aircraft.getMass();
        double velocity = aircraft.getVelocity();
        double drag = aerodynamics.getDrag();

        double weight = mass * GRAVITY;

        // Thrust-to-weight ratio
        thrustToWeightRatio = thrust / weight;

        // Net force along the direction of flight
        netForce = thrust - drag;

        // Newton's second law: F = ma
        acceleration = netForce / mass;

        // Propulsive power: P = T * V
        propulsivePower = thrust * velocity;

        // Simplified rocket/jet momentum relation: T = mdot * Ve
        if (exhaustVelocity > 0) {
            massFlowRate = thrust / exhaustVelocity;
        } else {
            massFlowRate = 0;
        }

        // Specific impulse: Isp = Ve / g
        if (exhaustVelocity > 0) {
            specificImpulse = exhaustVelocity / GRAVITY;
        } else {
            specificImpulse = 0;
        }
    }

    // Returns thrust in Newtons
    public double getThrust() {
        return thrust;
    }

    // Returns exhaust velocity in m/s
    public double getExhaustVelocity() {
        return exhaustVelocity;
    }

    // Returns thrust-to-weight ratio
    public double getThrustToWeightRatio() {
        return thrustToWeightRatio;
    }

    // Returns net force in Newtons
    public double getNetForce() {
        return netForce;
    }

    // Returns acceleration in m/s^2
    public double getAcceleration() {
        return acceleration;
    }

    // Returns propulsive power in Watts
    public double getPropulsivePower() {
        return propulsivePower;
    }

    // Returns mass flow rate in kg/s
    public double getMassFlowRate() {
        return massFlowRate;
    }

    // Returns specific impulse in seconds
    public double getSpecificImpulse() {
        return specificImpulse;
    }
}

