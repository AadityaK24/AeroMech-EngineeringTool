public class Aircraft 
{

    private double mass;
    private double wingArea;
    private double velocity;
    private double altitude;

    public Aircraft(double mass, double wingArea, double velocity, double altitude) {
        this.mass = mass;
        this.wingArea = wingArea;
        this.velocity = velocity;
        this.altitude = altitude;
    }

    // Getters

    public double getMass() {
        return mass;
    }

    public double getWingArea() {
        return wingArea;
    }

    public double getVelocity() {
        return velocity;
    }

    public double getAltitude() {
        return altitude;
    }

    // Setters

    public void setMass(double mass) {
        this.mass = mass;
    }

    public void setWingArea(double wingArea) {
        this.wingArea = wingArea;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }
}