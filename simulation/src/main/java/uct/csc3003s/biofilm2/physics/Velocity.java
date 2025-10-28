package uct.csc3003s.biofilm2.physics;

import java.util.List;
import uct.csc3003s.biofilm2.particle.Bacterium;
import uct.csc3003s.biofilm2.particle.EPS;
import uct.csc3003s.biofilm2.particle.Particle;
import uct.csc3003s.biofilm2.util.Vec2;

/**
 * Utility class for calculating particle velocities in the biofilm simulation.
 * 
 * This class provides methods for computing linear and angular velocities
 * from applied forces and torques, using overdamped dynamics appropriate
 * for biofilm systems.
 */
public final class Velocity {

    /**
     * Functional interface for torque calculations.
     * 
     * This interface defines the contract for torque implementations,
     * similar to the Force interface but returning scalar torque values.
     */
    @FunctionalInterface
    public interface Torque {
        /**
         * Calculates the torque acting on the focal particle.
         * 
         * @param particles Variable-length array where particles[0] is the focal particle
         *                  and particles[1..n] are neighboring particles
         * @return Torque value (scalar) acting on the focal particle
         */
        double calculate(Particle... particles);
    }

    private Velocity() {} 

    /**
     * Computes linear velocity from a set of applied forces using overdamped dynamics.
     * 
     * The velocity is calculated as v = F / (η * L) where:
     * - F is the total force vector
     * - η is the friction coefficient
     * - L is the effective length scale of the particle
     * 
     * @param particle The particle for which to calculate velocity
     * @param eta Friction coefficient (Pa·h)
     * @param forces List of forces acting on the particle
     * @param neighborhood Array of neighboring particles
     * @return Linear velocity vector
     */
    public static Vec2 linearVelocity(
            Particle particle,
            double eta,
            List<Force> forces,
            Particle... neighborhood
    ) {
        if (particle == null || eta <= 0.0 || forces == null || forces.isEmpty()) {
            return new Vec2(0.0, 0.0);
        }

        final double L = effectiveLength(particle);
        if (L <= 0.0) return new Vec2(0.0, 0.0);

        double fx = 0.0, fy = 0.0;
        final Particle[] args = prepend(particle, neighborhood);

        // Sum all forces acting on the particle
        for (Force f : forces) {
            Vec2 Fi = f.calculate(args); 
            fx += Fi.getX();
            fy += Fi.getY();
        }

        // Apply overdamped scaling: v = F / (η * L)
        final double scale = 1.0 / (eta * L);
        return new Vec2(scale * fx, scale * fy);
    }

    /**
     * Computes angular velocity from a set of applied torques using overdamped dynamics.
     * 
     * The angular velocity is calculated as ω = (12 * τ) / (η * L³) where:
     * - τ is the total torque
     * - η is the friction coefficient
     * - L is the effective length scale of the particle
     * 
     * @param particle The particle for which to calculate angular velocity
     * @param eta Friction coefficient (Pa·h)
     * @param torques List of torques acting on the particle
     * @param neighborhood Array of neighboring particles
     * @return Angular velocity (radians per unit time)
     */
    public static double angularVelocity(
            Particle particle,
            double eta,
            List<Torque> torques,
            Particle... neighborhood
    ) {
        if (particle == null || eta <= 0.0 || torques == null || torques.isEmpty()) {
            return 0.0;
        }

        final double L = effectiveLength(particle);
        if (L <= 0.0) return 0.0;

        double tau = 0.0;
        final Particle[] args = prepend(particle, neighborhood);

        // Sum all torques acting on the particle
        for (Torque t : torques) {
            double torque = t.calculate(args);
            tau += torque;
        }

        return (12.0 / (eta * L * L * L)) * tau;
    }


    /**
     * Determines the appropriate geometric length scale L for overdamped dynamics.
     * 
     * Different particle types use different length scales:
     * - Bacteria: Use the full rod length
     * - EPS particles: Use the diameter (2 * radius)
     * 
     * @param p The particle for which to determine the length scale
     * @return Effective length scale for the particle
     */
    private static double effectiveLength(Particle p) {
        if (p instanceof Bacterium b) {
            // Bacterium.getLength() represents the full rod length L
            return b.getLength();
        } else if (p instanceof EPS e) {
            return 2.0 * e.getRadius();
        } else {
            return 0.0;
        }
    }

    /**
     * Prepends the focal particle to the neighbor array for force/torque calculations.
     * 
     * This helper method creates a new array with the focal particle as the first
     * element, followed by all neighboring particles. This format is expected by
     * force and torque calculation methods.
     * 
     * @param head The focal particle
     * @param tail Array of neighboring particles (may be null)
     * @return New array with focal particle first, followed by neighbors
     */
    private static Particle[] prepend(Particle head, Particle[] tail) {
        Particle[] all = new Particle[(tail == null ? 0 : tail.length) + 1];
        all[0] = head;
        if (tail != null) System.arraycopy(tail, 0, all, 1, tail.length);
        return all;
    }
}
