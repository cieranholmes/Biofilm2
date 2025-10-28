package uct.csc3003s.biofilm2.physics;

import uct.csc3003s.biofilm2.particle.Particle;
import uct.csc3003s.biofilm2.util.Vec2;

/**
 * Interface for force calculations in the biofilm simulation.
 * 
 * This interface defines the contract for all force implementations in the simulation.
 * Forces are applied to particles and can depend on the particle's properties and
 * its neighborhood of nearby particles. The first particle in the array is typically
 * the focal particle, while subsequent particles represent its neighbors.
 */
public interface Force {
    /**
     * Calculates the force vector acting on the focal particle.
     * 
     * @param particles Variable-length array where particles[0] is the focal particle
     *                  and particles[1..n] are neighboring particles
     * @return Force vector in 2D space
     */
    public Vec2 calculate(Particle... particles);
}
