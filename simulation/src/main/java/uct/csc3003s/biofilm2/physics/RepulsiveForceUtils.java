package uct.csc3003s.biofilm2.physics;

import uct.csc3003s.biofilm2.parser.ConfigParser;
import uct.csc3003s.biofilm2.particle.Bacterium;
import uct.csc3003s.biofilm2.particle.EPS;
import uct.csc3003s.biofilm2.particle.Particle;
import uct.csc3003s.biofilm2.util.Vec2;

/**
 * Utility class for shared repulsive force calculations.
 * Contains common methods used by both RepulsiveForce and RepulsiveTorque.
 */
public final class RepulsiveForceUtils {
    
    private RepulsiveForceUtils() {} // Utility class - no instantiation
    
    /**
     * Get the appropriate elastic modulus based on the types of interacting particles
     */
    public static double getElasticModulus(Particle a, Particle b) {
        boolean aIsBacterium = a instanceof Bacterium;
        boolean bIsBacterium = b instanceof Bacterium;
        boolean aIsEPS = a instanceof EPS;
        boolean bIsEPS = b instanceof EPS;
        
        if (aIsBacterium && bIsBacterium) {
            return ConfigParser.emCellCell;
        }
        else if (aIsEPS && bIsEPS) {
            return ConfigParser.emEpsEps;
        }
        else if ((aIsEPS && bIsBacterium) || (aIsBacterium && bIsEPS)) {
            return ConfigParser.emEpsCell;
        }
        // Fallback to general repulsion force for unknown particle types
        else {
            return ConfigParser.repulsionForce;
        }
    }
    
    /**
     * Calculate normal vector from particle B to particle A
     */
    public static Vec2 calculateNormal(Particle a, Particle b, double parallelEps) {
        Vec2 posA = a.getPosition();
        Vec2 posB = b.getPosition();
        
        // Vector from B to A
        double dx = posA.getX() - posB.getX();
        double dy = posA.getY() - posB.getY();
        
        double distance = Math.hypot(dx, dy);
        
        // Return unit normal vector, or default if particles are at same position
        if (distance <= parallelEps) {
            return new Vec2(1.0, 0.0);
        }
        
        return new Vec2(dx / distance, dy / distance);
    }
    
    /**
     * Calculate the magnitude of repulsive force between two particles
     */
    public static double calculateForceMagnitude(Particle a, Particle b, double overlap) {
        if (overlap <= 0.0) {
            return 0.0;
        }
        
        double elasticModulus = getElasticModulus(a, b);
        return elasticModulus * Math.sqrt(ConfigParser.diameter) * Math.pow(overlap, 1.5);
    }
}
