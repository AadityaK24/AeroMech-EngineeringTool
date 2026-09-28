public class Aerodynamics 
{
    private Aircraft aircraft;
    private Atmosphere atmosphere;

    private double liftCoefficient;
    private double dragCoefficient;
    private double zeroLiftDragCoefficient;
    private double inducedDragFactor;

    private double dynamicPressure;
    private double lift;
    private double drag;

    public Aerodynamics(
            Aircraft aircraft,
            Atmosphere atmosphere,
            double liftCoefficient,
            double zeroLiftDragCoefficient,
            double inducedDragFactor) {

        this.aircraft = aircraft;
        this.atmosphere = atmosphere;
        this.liftCoefficient = liftCoefficient;
        this.zeroLiftDragCoefficient = zeroLiftDragCoefficient;
        this.inducedDragFactor = inducedDragFactor;

        calculateAerodynamics();
    }

    private void calculateAerodynamics()
     {
        double density = atmosphere.getDensity();
        double velocity = aircraft.getVelocity();
        double wingArea = aircraft.getWingArea();

        dynamicPressure = 0.5 * density * velocity * velocity;

        dragCoefficient = zeroLiftDragCoefficient + inducedDragFactor * liftCoefficient * liftCoefficient;
        
        lift = dynamicPressure * wingArea * liftCoefficient;

        drag = dynamicPressure * wingArea * dragCoefficient;
    }

    public double getDynamicPressure() { return dynamicPressure; }
    public double getLiftCoefficient() { return liftCoefficient; }
    public double getDragCoefficient() { return dragCoefficient; }
    public double getZeroLiftDragCoefficient() { return zeroLiftDragCoefficient; }
    public double getInducedDragFactor() { return inducedDragFactor; }
    public double getLift() { return lift; }
    public double getDrag() { return drag; }

    public double getLiftToDragRatio()
    {
        if (drag == 0) 
            return 0;
        return lift / drag;
    }
}
