public class InputValidator {

    private InputValidator() {
        // Utility class - no objects required.
    }

    // ==============================
    // GENERAL VALIDATION
    // ==============================

    public static void validatePositive(
            double value,
            String parameterName) {

        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(
                    parameterName + " must be a finite number."
            );
        }

        if (value <= 0) {
            throw new IllegalArgumentException(
                    parameterName + " must be greater than zero."
            );
        }
    }


    // ==============================
    // AIRCRAFT PARAMETERS
    // ==============================

    public static void validateMass(double mass) {
        validatePositive(mass, "Mass");
    }

    public static void validateWingArea(double wingArea) {
        validatePositive(wingArea, "Wing area");
    }

    public static void validateVelocity(double velocity) {
        if (Double.isNaN(velocity) || Double.isInfinite(velocity)) {
            throw new IllegalArgumentException(
                    "Velocity must be a finite number."
            );
        }

        if (velocity < 0) {
            throw new IllegalArgumentException(
                    "Velocity cannot be negative."
            );
        }
    }


    // ==============================
    // ATMOSPHERIC PARAMETERS
    // ==============================

    public static void validateAltitude(double altitude) {

        if (Double.isNaN(altitude) || Double.isInfinite(altitude)) {
            throw new IllegalArgumentException(
                    "Altitude must be a finite number."
            );
        }

        /*
         * Current Atmosphere model uses the standard
         * tropospheric lapse-rate equation.
         *
         * This model is valid up to approximately
         * 11,000 metres.
         */
        if (altitude < 0) {
            throw new IllegalArgumentException(
                    "Altitude cannot be below sea level."
            );
        }

        if (altitude > 11000) {
            throw new IllegalArgumentException(
                    "Altitude exceeds the current atmospheric model limit of 11,000 m."
            );
        }
    }

    public static void validateAirDensity(double airDensity) {
        validatePositive(airDensity, "Air density");
    }


    // ==============================
    // AERODYNAMIC PARAMETERS
    // ==============================

    public static void validateCd0(double cd0) {
        validatePositive(cd0, "Zero-lift drag coefficient (CD0)");
    }

    public static void validateInducedDragFactor(double inducedFactor) {
        validatePositive(
                inducedFactor,
                "Induced drag factor (k)"
        );
    }

    public static void validateLiftCoefficient(double liftCoefficient) {

        if (Double.isNaN(liftCoefficient)
                || Double.isInfinite(liftCoefficient)) {

            throw new IllegalArgumentException(
                    "Lift coefficient must be a finite number."
            );
        }
    }


    // ==============================
    // WEIGHT
    // ==============================

    public static void validateWeight(double weight) {
        validatePositive(weight, "Weight");
    }
}