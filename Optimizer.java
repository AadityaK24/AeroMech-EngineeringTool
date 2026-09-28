public class Optimizer {

    private final double weight;          // Newtons
    private final double wingArea;       // m^2
    private final double airDensity;     // kg/m^3
    private final double cd0;             // Zero-lift drag coefficient
    private final double inducedFactor;   // Induced drag factor k

    private double optimalVelocity;
    private double maximumLDRatio;
    private double optimalCL;
    private double optimalCD;

    public Optimizer(
            double weight,
            double wingArea,
            double airDensity,
            double cd0,
            double inducedFactor) {

        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be greater than zero.");
        }

        if (wingArea <= 0) {
            throw new IllegalArgumentException("Wing area must be greater than zero.");
        }

        if (airDensity <= 0) {
            throw new IllegalArgumentException("Air density must be greater than zero.");
        }

        if (cd0 <= 0) {
            throw new IllegalArgumentException("CD0 must be greater than zero.");
        }

        if (inducedFactor <= 0) {
            throw new IllegalArgumentException("Induced drag factor must be greater than zero.");
        }

        this.weight = weight;
        this.wingArea = wingArea;
        this.airDensity = airDensity;
        this.cd0 = cd0;
        this.inducedFactor = inducedFactor;
    }

    public void optimize() {

        // Theoretical CL for maximum L/D
        optimalCL = Math.sqrt(cd0 / inducedFactor);

        // Corresponding CD
        optimalCD = cd0 + inducedFactor * optimalCL * optimalCL;

        // Maximum L/D
        maximumLDRatio = optimalCL / optimalCD;

        // Velocity at maximum L/D
        optimalVelocity = Math.sqrt(
                (2.0 * weight) /
                (airDensity * wingArea * optimalCL)
        );
    }

    public double getOptimalVelocity() {
        return optimalVelocity;
    }

    public double getMaximumLDRatio() {
        return maximumLDRatio;
    }

    public double getOptimalCL() {
        return optimalCL;
    }

    public double getOptimalCD() {
        return optimalCD;
    }

    public void printResults() {

        System.out.println("=== AeroMech Optimization Results ===");

        System.out.printf("Optimal CL       : %.6f%n", optimalCL);
        System.out.printf("Optimal CD       : %.6f%n", optimalCD);
        System.out.printf("Maximum L/D      : %.6f%n", maximumLDRatio);
        System.out.printf("Optimal Velocity : %.3f m/s%n", optimalVelocity);

        System.out.println("--------------------------------------");

        System.out.println("Input Parameters");
        System.out.printf("Weight           : %.2f N%n", weight);
        System.out.printf("Wing Area        : %.2f m^2%n", wingArea);
        System.out.printf("Air Density      : %.3f kg/m^3%n", airDensity);
        System.out.printf("CD0              : %.6f%n", cd0);
        System.out.printf("Induced Factor k : %.6f%n", inducedFactor);
    }
}