package uct.csc3003s.biofilm2.physics;

import uct.csc3003s.biofilm2.parser.ConfigParser;
import uct.csc3003s.biofilm2.particle.Particle;
import uct.csc3003s.biofilm2.util.Vec2;
import uct.csc3003s.biofilm2.util.CollisionDetector;

/**
 * Repulsive contact force consistent with the Soft Matter model:
 *   F = E * sqrt(d_eff) * h^(3/2),  where h = d_eff - r_perp and h>0.
 * Returns the TOTAL repulsive force on the FIRST particle due to all others.
 *
 * The elastic modulus E is automatically selected based on particle types:
 * - Cell-Cell interactions: uses ConfigParser.emCellCell
 * - EPS-EPS interactions: uses ConfigParser.emEpsEps  
 * - EPS-Cell interactions: uses ConfigParser.emEpsCell
 *
 * Supports: Bacterium (rod/spherocylinder) and EPS (sphere).
 * Uses CollisionDetector for accurate distance calculations between different particle types.
 */
public class RepulsiveForce implements Force {

    private final double parallelEps;          // Small tolerance for numerical precision

    /**
     * Constructs a repulsive force with the specified numerical tolerance.
     * 
     * @param parallelTolerance Small tolerance value for handling parallel vectors
     */
    public RepulsiveForce(double parallelTolerance) {
        this.parallelEps = parallelTolerance;
    }

    /**
     * Calculates the total repulsive force acting on the focal particle.
     * 
     * @param particles Array where particles[0] is the focal particle and
     *                  particles[1..n] are neighboring particles
     * @return Total repulsive force vector acting on the focal particle
     */
    @Override
    public Vec2 calculate(Particle... particles) {
        if (particles == null || particles.length < 2) {
            return new Vec2(0.0, 0.0);
        }

        Particle a = particles[0]; // Focal particle

        double fx = 0.0, fy = 0.0;

        // Calculate repulsive forces from all neighboring particles
        for (int i = 1; i < particles.length; i++) {
            Particle b = particles[i];

            // Use CollisionDetector to get minimum distance between particles
            double r = CollisionDetector.getMinimumDistance(a, b);

            // Calculate overlap (positive when particles are penetrating)
            double h = ConfigParser.diameter - r;
            if (h > 0.0) {
                // Calculate force magnitude using shared utility
                double mag = RepulsiveForceUtils.calculateForceMagnitude(a, b, h);

                // Calculate normal vector from B -> A using shared utility
                Vec2 normal = RepulsiveForceUtils.calculateNormal(a, b, parallelEps);
                fx += mag * normal.getX();
                fy += mag * normal.getY();
            }
        }

        return new Vec2(fx, fy);
    }
}
