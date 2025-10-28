package uct.csc3003s.biofilm2.physics;

import uct.csc3003s.biofilm2.parser.ConfigParser;
import uct.csc3003s.biofilm2.particle.Particle;
import uct.csc3003s.biofilm2.util.Vec2;
import uct.csc3003s.biofilm2.util.CollisionDetector;

/**
 * Repulsive torque calculation for biofilm simulation.
 * 
 * When particles collide, the repulsive force is applied at the contact point,
 * not at the center of mass. This creates a torque τ = F × r_d where:
 * - F is the repulsive force
 * - r_d is the vector from center of mass to contact point
 * 
 * The torque is calculated as a scalar (about the z-axis) for 2D simulation.
 * Positive torque causes counter-clockwise rotation.
 */
public class RepulsiveTorque implements Velocity.Torque {

    private final double parallelEps; // Small tolerance for numerical precision

    /**
     * Constructs a repulsive torque calculator with the specified numerical tolerance.
     * 
     * @param parallelTolerance Small tolerance value for handling parallel vectors
     */
    public RepulsiveTorque(double parallelTolerance) {
        this.parallelEps = parallelTolerance;
    }

    /**
     * Calculates the total repulsive torque acting on the focal particle.
     * 
     * @param particles Array where particles[0] is the focal particle and
     *                  particles[1..n] are neighboring particles
     * @return Total repulsive torque (scalar) acting on the focal particle
     */
    @Override
    public double calculate(Particle... particles) {
        if (particles == null || particles.length < 2) {
            return 0.0;
        }

        Particle a = particles[0]; // Focal particle
        double totalTorque = 0.0;

        // Calculate repulsive torques from all neighboring particles
        for (int i = 1; i < particles.length; i++) {
            Particle b = particles[i];

            // Use CollisionDetector to get minimum distance between particles
            double r = CollisionDetector.getMinimumDistance(a, b);

            // Calculate overlap (positive when particles are penetrating)
            double h = ConfigParser.diameter - r;
            if (h > 0.0) {
                // Calculate force magnitude using shared utility
                double forceMagnitude = RepulsiveForceUtils.calculateForceMagnitude(a, b, h);

                // Calculate contact point using CollisionDetector
                Vec2 contactPoint = CollisionDetector.getContactPoint(a, b, parallelEps);
                Vec2 leverArm = new Vec2(
                    contactPoint.getX() - a.getPosition().getX(),
                    contactPoint.getY() - a.getPosition().getY()
                );

                // Calculate normal vector from B -> A using shared utility
                Vec2 normal = RepulsiveForceUtils.calculateNormal(a, b, parallelEps);
                Vec2 force = new Vec2(forceMagnitude * normal.getX(), forceMagnitude * normal.getY());

                // Calculate torque: τ = r × F (cross product in 2D)
                // In 2D, cross product gives scalar: τ = r_x * F_y - r_y * F_x
                double torque = leverArm.getX() * force.getY() - leverArm.getY() * force.getX();
                totalTorque += torque;
            }
        }

        return totalTorque;
    }
}