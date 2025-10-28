package uct.csc3003s.biofilm2.physics;

import java.util.concurrent.ThreadLocalRandom;

import uct.csc3003s.biofilm2.particle.Particle;
import uct.csc3003s.biofilm2.util.Vec2;

/**
 * Implements random force for particles in the biofilm simulation.
 * 
 * Random force represents thermal fluctuations and other stochastic effects
 * that cause random motion in particles. The force is applied in random directions
 * with magnitudes within a specified range, simulating Brownian motion and
 * other random environmental influences.
 */
public class RandomForce implements Force {
    // Random force magnitude range in dimensionless units (from research paper: ±10^-3)
    private final double minMagnitude = -0.001;
    private final double maxMagnitude = 0.001;

    /**
     * Calculates a random force vector acting on the focal particle.
     * The force is independent of particle orientation and represents
     * stochastic environmental effects.
     * 
     * @param particles Array where particles[0] is the focal particle (unused)
     * @return Random force vector with components in range [-0.001, 0.001]
     */
    public Vec2 calculate(Particle... particles) {
        double randomX = minMagnitude + ThreadLocalRandom.current().nextDouble() * (maxMagnitude-minMagnitude);
        double randomY = minMagnitude + ThreadLocalRandom.current().nextDouble() * (maxMagnitude-minMagnitude);
        return new Vec2(randomX, randomY);
    }
}
