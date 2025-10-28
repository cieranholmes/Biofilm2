package uct.csc3003s.biofilm2.particle;

import uct.csc3003s.biofilm2.util.Vec2;

/**
 * Abstract base class for all particles in the biofilm simulation.
 * 
 * This class defines the common properties and behaviors shared by all particle types
 * in the simulation, including bacteria and EPS.
 * It provides the fundamental interface for position, velocity, orientation, and
 * neighbor detection functionality.
 */
public abstract class Particle {
    protected int id;                    // Unique particle identifier
    protected double diameter;           // Particle diameter in micrometers
    protected Vec2 position;             // Current position in 2D space
    protected Vec2 velocity;             // Current velocity vector
    protected Vec2 orientation;          // Orientation vector (unit vector)

    /**
     * Constructs a new particle with the specified properties.
     * 
     * @param id Unique particle identifier
     * @param diameter Particle diameter in micrometers
     * @param position Initial position in 2D space
     * @param velocity Initial velocity vector
     * @param orientation Initial orientation vector (should be normalized)
     */
    public Particle(int id, double diameter, Vec2 position, Vec2 velocity, Vec2 orientation) {
        this.id = id;
        this.diameter = diameter;
        this.position = position;
        this.velocity = velocity;
        this.orientation = orientation;
    }

    /**
     * Gets the current position of the particle.
     * 
     * @return Current position vector
     */
    public Vec2 getPosition() {
        return position;
    }
    
    /**
     * Sets the position of the particle.
     * 
     * @param position New position vector
     */
    public void setPosition(Vec2 position) {
        this.position = position;
    }
    
    /**
     * Gets the current velocity of the particle.
     * 
     * @return Current velocity vector
     */
    public Vec2 getVelocity() {
        return velocity;
    }
    
    /**
     * Gets the unique particle identifier.
     * 
     * @return Particle ID
     */
    public int getParticleId() {
        return id;
    }

    /**
     * Gets the current orientation of the particle.
     * 
     * @return Current orientation vector (unit vector)
     */
    public Vec2 getOrientation() {
        return orientation;
    }

    /**
     * Sets the orientation of the particle.
     * 
     * @param orientation New orientation vector (should be normalized)
     */
    public void setOrientation(Vec2 orientation) {
        this.orientation = orientation;
    }

    /**
     * Gets the diameter of the particle.
     * 
     * @return Particle diameter in micrometers
     */
    public double getDiameter() {
        return diameter;
    }

    /**
     * Sets the diameter of the particle.
     * 
     * @param diameter New diameter in micrometers
     */
    public void setDiameter(double diameter) {
        this.diameter = diameter;
    }

}
