public class UnitConverter {

    // ==============================
    // SPEED
    // ==============================

    public static double metersPerSecondToKilometersPerHour(double velocity) {
        return velocity * 3.6;
    }

    public static double kilometersPerHourToMetersPerSecond(double velocity) {
        return velocity / 3.6;
    }

    public static double metersPerSecondToKnots(double velocity) {
        return velocity * 1.94384449;
    }

    public static double knotsToMetersPerSecond(double velocity) {
        return velocity / 1.94384449;
    }


    // ==============================
    // LENGTH
    // ==============================

    public static double metersToFeet(double length) {
        return length * 3.280839895;
    }

    public static double feetToMeters(double length) {
        return length / 3.280839895;
    }


    // ==============================
    // MASS
    // ==============================

    public static double kilogramsToPounds(double mass) {
        return mass * 2.2046226218;
    }

    public static double poundsToKilograms(double mass) {
        return mass / 2.2046226218;
    }


    // ==============================
    // FORCE
    // ==============================

    public static double newtonsToPoundForce(double force) {
        return force * 0.2248089431;
    }

    public static double poundForceToNewtons(double force) {
        return force / 0.2248089431;
    }


    // ==============================
    // PRESSURE
    // ==============================

    public static double pascalsToPsi(double pressure) {
        return pressure * 0.0001450377377;
    }

    public static double psiToPascals(double pressure) {
        return pressure / 0.0001450377377;
    }


    // ==============================
    // DENSITY
    // ==============================

    public static double kilogramsPerCubicMeterToPoundsPerCubicFoot(double density) {
        return density * 0.0624279606;
    }

    public static double poundsPerCubicFootToKilogramsPerCubicMeter(double density) {
        return density / 0.0624279606;
    }
}