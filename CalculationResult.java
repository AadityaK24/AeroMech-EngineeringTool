public class CalculationResult {

    private final double velocity;
    private final double dynamicPressure;
    private final double liftCoefficient;
    private final double dragCoefficient;
    private final double lift;
    private final double drag;
    private final double liftToDragRatio;

    public CalculationResult(
            double velocity,
            double dynamicPressure,
            double liftCoefficient,
            double dragCoefficient,
            double lift,
            double drag) {

        this.velocity = velocity;
        this.dynamicPressure = dynamicPressure;
        this.liftCoefficient = liftCoefficient;
        this.dragCoefficient = dragCoefficient;
        this.lift = lift;
        this.drag = drag;

        if (drag != 0) {
            this.liftToDragRatio = lift / drag;
        } else {
            this.liftToDragRatio = 0.0;
        }
    }

    // ==============================
    // GETTERS
    // ==============================

    public double getVelocity() {
        return velocity;
    }

    public double getDynamicPressure() {
        return dynamicPressure;
    }

    public double getLiftCoefficient() {
        return liftCoefficient;
    }

    public double getDragCoefficient() {
        return dragCoefficient;
    }

    public double getLift() {
        return lift;
    }

    public double getDrag() {
        return drag;
    }

    public double getLiftToDragRatio() {
        return liftToDragRatio;
    }

    // ==============================
    // DISPLAY
    // ==============================

    public void printResults() {

        System.out.println("=== Calculation Results ===");

        System.out.printf("Velocity          : %.3f m/s%n", velocity);
        System.out.printf("Dynamic Pressure  : %.3f Pa%n", dynamicPressure);
        System.out.printf("Lift Coefficient  : %.6f%n", liftCoefficient);
        System.out.printf("Drag Coefficient  : %.6f%n", dragCoefficient);
        System.out.printf("Lift              : %.3f N%n", lift);
        System.out.printf("Drag              : %.3f N%n", drag);
        System.out.printf("Lift/Drag Ratio   : %.6f%n", liftToDragRatio);
    }
}