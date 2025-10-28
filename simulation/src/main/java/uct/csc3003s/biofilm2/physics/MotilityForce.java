package uct.csc3003s.biofilm2.physics;

import uct.csc3003s.biofilm2.particle.Particle;
import uct.csc3003s.biofilm2.util.Vec2;

/**
 * Implements motility force for bacterial cells in the biofilm simulation.
 * 
 * Motility force represents the self-propulsion force that bacteria generate
 * to move in their preferred direction. The force is applied along the particle's
 * orientation vector with a constant magnitude.
 */
public class MotilityForce implements Force {
    private final double magnitude;              // Force magnitude in Pa·μm²

    /**
     * Constructs a motility force with the specified magnitude.
     * 
     * @param magnitude Force magnitude in Pa·μm^2
     */
    public MotilityForce(double magnitude) {
        this.magnitude = magnitude;
    }

    /**
     * Calculates the motility force vector acting on the focal particle.
     * The force is applied along the particle's orientation direction.
     * 
     * @param particles Array where particles[0] is the focal particle
     * @return Motility force vector in the direction of particle orientation
     */
    public Vec2 calculate(Particle... particles) {
        if (particles.length == 0) {
            return new Vec2(0, 0);
        }
        Particle particle = particles[0];
        Vec2 orientation = particle.getOrientation();
        Vec2 forceVector = new Vec2(magnitude * orientation.getX(), magnitude * orientation.getY());
        return forceVector;
    }
}
