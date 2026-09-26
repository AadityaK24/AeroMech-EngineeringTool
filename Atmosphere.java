public class Atmosphere {

    private static final double SEA_LEVEL_TEMPERATURE = 288.15; // K
    private static final double SEA_LEVEL_PRESSURE = 101325.0; // Pa
    private static final double TEMPERATURE_LAPSE_RATE = 0.0065; // K/m
    private static final double GAS_CONSTANT = 287.05; // J/(kg·K)
    private static final double GRAVITY = 9.80665; // m/s²

    private double altitude;
    private double temperature;
    private double pressure;
    private double density;

    public Atmosphere(double altitude) {
        this.altitude = altitude;
        calculateAtmosphere();
    }

    private void calculateAtmosphere() {

        temperature = SEA_LEVEL_TEMPERATURE
                - (TEMPERATURE_LAPSE_RATE * altitude);

        pressure = SEA_LEVEL_PRESSURE
                * Math.pow(
                    temperature / SEA_LEVEL_TEMPERATURE,
                    GRAVITY / (GAS_CONSTANT * TEMPERATURE_LAPSE_RATE)
                  );

        density = pressure / (GAS_CONSTANT * temperature);
    }

    public double getAltitude() {
        return altitude;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getPressure() {
        return pressure;
    }

    public double getDensity() {
        return density;
    }
}