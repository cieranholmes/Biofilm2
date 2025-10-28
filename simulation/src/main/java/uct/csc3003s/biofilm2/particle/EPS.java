package uct.csc3003s.biofilm2.particle;

import uct.csc3003s.biofilm2.util.Vec2;

/**
 * Represents an EPS particle in the biofilm simulation.
 * 
 * EPS particles are produced by bacteria under high density conditions and contribute
 * to biofilm structure and cohesion.
 */
public class EPS extends Particle {
    private double radius;              // Radius of the EPS particle in micrometers

    /**
     * Constructs a new EPS particle with the specified properties.
     * 
     * @param id Unique EPS identifier
     * @param position Initial position in 2D space
     * @param velocity Initial velocity vector
     * @param orientation Initial orientation vector
     * @param radius Radius of the EPS particle in micrometers
     */
    public EPS(int id, Vec2 position, Vec2 velocity, Vec2 orientation, double radius) {
        super(id, radius*2, position, velocity, orientation);
        this.radius = radius;
    }

    /**
     * Gets the radius of the EPS particle.
     * 
     * @return EPS radius in micrometers
     */
    public double getRadius() {
        return radius;
    }

}
