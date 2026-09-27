
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

        // Dynamic pressure:
        // q = 1/2 * rho * V^2
 dynamicPressure = 0.5 * density * velocity * velocity;

        // Drag coefficient using a simplified drag polar:
        // Cd = Cd0 + k * Cl^2
        dragCoefficient = zeroLiftDragCoefficient + inducedDragFactor* liftCoefficient * liftCoefficient;
        
        // Lift:
        // L = q * S * Cl
        lift = dynamicPressure * wingArea * liftCoefficient;

        // Drag:
        // D = q * S * Cd
        drag = dynamicPressure * wingArea* dragCoefficient;
    }

    public double getDynamicPressure() 
    {
        return dynamicPressure;
    }

    public double getLiftCoefficient() 
    {
        return liftCoefficient;
    }

    public double getDragCoefficient() 
    {
        return dragCoefficient;
    }

    public double getLift()
    {
        return lift;
    }

    public double getDrag()
    {
        return drag;
    }

    public double getLiftToDragRatio()
    {
        if (drag == 0) 
            return 0;
         return lift / drag;
    }
}

