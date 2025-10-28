package uct.csc3003s.biofilm2.util;

import uct.csc3003s.biofilm2.particle.Bacterium;
import uct.csc3003s.biofilm2.particle.EPS;
import uct.csc3003s.biofilm2.particle.Particle;

/**
 * Collision detection utility for the biofilm simulation.
 * 
 * This class provides comprehensive collision detection between different particle types:
 * - Bacterium-Bacterium collisions (rod-rod with hemispherical caps)
 * - EPS-EPS collisions (sphere-sphere)
 * - EPS-Bacterium collisions (sphere-rod with hemispherical caps)
 * 
 * The collision detection handles the complex geometry of bacterial cells, which are
 * modeled as spherocylinders (cylindrical bodies with hemispherical end caps).
 * 
 */
public class CollisionDetector {
    
    /**
     * Checks if two particles are colliding or overlapping.
     * 
     * This method determines collision by comparing the minimum distance between
     * particles with their combined radii. Different collision detection algorithms
     * are used based on particle types to handle their specific geometries.
     * 
     * @param p1 First particle
     * @param p2 Second particle
     * @return True if particles are colliding, false otherwise
     */
    public static boolean isColliding(Particle p1, Particle p2) {
        // Prevent self-collision detection
        if (p1 == p2 || p1.getParticleId() == p2.getParticleId()) {
            return false;
        }
        
        if (p1 instanceof Bacterium && p2 instanceof Bacterium) {
            return bacteriumBacteriumCollision((Bacterium) p1, (Bacterium) p2);
        } else if (p1 instanceof EPS && p2 instanceof EPS) {
            return epsEpsCollision((EPS) p1, (EPS) p2);
        } else if ((p1 instanceof Bacterium && p2 instanceof EPS) || 
                   (p1 instanceof EPS && p2 instanceof Bacterium)) {
            Bacterium bacterium = (p1 instanceof Bacterium) ? (Bacterium) p1 : (Bacterium) p2;
            EPS eps = (p1 instanceof EPS) ? (EPS) p1 : (EPS) p2;
            return epsBacteriumCollision(eps, bacterium);
        }
        return false;
    }
    
    /**
     * Calculates the minimum distance between two particles (edge-to-edge).
     * 
     * This method computes the shortest distance between the surfaces of two particles.
     * For overlapping particles, the distance is 0. Different distance calculation
     * algorithms are used based on particle types to handle their specific geometries.
     * 
     * @param p1 First particle
     * @param p2 Second particle
     * @return Minimum distance between particle surfaces
     */
    public static double getMinimumDistance(Particle p1, Particle p2) {
        if (p1 instanceof Bacterium && p2 instanceof Bacterium) {
            return bacteriumBacteriumDistance((Bacterium) p1, (Bacterium) p2);
        } else if (p1 instanceof EPS && p2 instanceof EPS) {
            return epsEpsDistance((EPS) p1, (EPS) p2);
        } else if ((p1 instanceof Bacterium && p2 instanceof EPS) || 
                   (p1 instanceof EPS && p2 instanceof Bacterium)) {
            Bacterium bacterium = (p1 instanceof Bacterium) ? (Bacterium) p1 : (Bacterium) p2;
            EPS eps = (p1 instanceof EPS) ? (EPS) p1 : (EPS) p2;
            return epsBacteriumDistance(eps, bacterium);
        }
        return Double.MAX_VALUE;
    }
    
    /**
     * EPS-EPS collision detection (sphere-sphere).
     * 
     * @param eps1 First EPS particle
     * @param eps2 Second EPS particle
     * @return True if spheres are overlapping
     */
    private static boolean epsEpsCollision(EPS eps1, EPS eps2) {
        double distance = distance(eps1.getPosition(), eps2.getPosition());
        double radiusSum = eps1.getRadius() + eps2.getRadius();
        return distance < radiusSum;
    }
    
    /**
     * EPS-EPS minimum distance calculation (sphere-sphere).
     * 
     * @param eps1 First EPS particle
     * @param eps2 Second EPS particle
     * @return Minimum distance between sphere surfaces
     */
    private static double epsEpsDistance(EPS eps1, EPS eps2) {
        double centerDistance = distance(eps1.getPosition(), eps2.getPosition());
        double radiusSum = eps1.getRadius() + eps2.getRadius();
        return Math.max(0.0, centerDistance - radiusSum);
    }
    
    /**
     * EPS-Bacterium collision detection (sphere-spherocylinder).
     * 
     * @param eps EPS particle (sphere)
     * @param bacterium Bacterium particle (spherocylinder)
     * @return True if sphere overlaps with spherocylinder
     */
    private static boolean epsBacteriumCollision(EPS eps, Bacterium bacterium) {
        double distance = pointToBacteriumDistance(eps.getPosition(), bacterium);
        return distance < eps.getRadius();
    }
    
    /**
     * EPS-Bacterium minimum distance calculation (sphere-spherocylinder).
     * 
     * @param eps EPS particle (sphere)
     * @param bacterium Bacterium particle (spherocylinder)
     * @return Minimum distance between sphere and spherocylinder surfaces
     */
    private static double epsBacteriumDistance(EPS eps, Bacterium bacterium) {
        double distance = pointToBacteriumDistance(eps.getPosition(), bacterium);
        return Math.max(0.0, distance - eps.getRadius());
    }
    
    /**
     * Bacterium-Bacterium collision detection (spherocylinder-spherocylinder).
     * Handles the complex geometry of bacterial cells with hemispherical end caps.
     * 
     * @param b1 First bacterium (spherocylinder)
     * @param b2 Second bacterium (spherocylinder)
     * @return True if spherocylinders are overlapping
     */
    private static boolean bacteriumBacteriumCollision(Bacterium b1, Bacterium b2) {
        double distance = bacteriumToBacteriumDistance(b1, b2);
        double radiusSum = (b1.getDiameter() + b2.getDiameter()) / 2.0;
        double tolerance = 1e-10; // Small tolerance for numerical precision
        return distance < (radiusSum + tolerance);
    }
    
    /**
     * Bacterium-Bacterium minimum distance calculation (spherocylinder-spherocylinder).
     * 
     * @param b1 First bacterium (spherocylinder)
     * @param b2 Second bacterium (spherocylinder)
     * @return Minimum distance between spherocylinder surfaces
     */
    private static double bacteriumBacteriumDistance(Bacterium b1, Bacterium b2) {
        double distance = bacteriumToBacteriumDistance(b1, b2);
        double radiusSum = (b1.getDiameter() + b2.getDiameter()) / 2.0;
        return Math.max(0.0, distance - radiusSum);
    }
    
    /**
     * Calculates distance from a point to a bacterium surface with hemispherical end caps.
     * 
     * @param point Point to measure distance from
     * @param bacterium Bacterium to measure distance to
     * @return Minimum distance from point to bacterium surface
     */
    private static double pointToBacteriumDistance(Vec2 point, Bacterium bacterium) {
        // Get bacterium geometry
        Vec2[] endpoints = getBacteriumEndpoints(bacterium);
        Vec2 start = endpoints[0];
        Vec2 end = endpoints[1];
        double radius = bacterium.getDiameter() / 2.0;
        
        // Calculate hemieps centers
        Vec2 center = bacterium.getPosition();
        Vec2 orientation = bacterium.getOrientation();
        double totalHalfLength = bacterium.getLength() / 2.0;
        
        Vec2 leftHemiepsCenter = new Vec2(
            center.getX() - orientation.getX() * totalHalfLength,
            center.getY() - orientation.getY() * totalHalfLength
        );
        
        Vec2 rightHemiepsCenter = new Vec2(
            center.getX() + orientation.getX() * totalHalfLength,
            center.getY() + orientation.getY() * totalHalfLength
        );
        
        // Distance to cylindrical body (between the axis endpoints)
        double cylinderDistance = pointToLineSegmentDistance(point, start, end) - radius;
        
        // Distance to left hemieps
        double leftHemiepsDistance = distance(point, leftHemiepsCenter) - radius;
        
        // Distance to right hemieps
        double rightHemiepsDistance = distance(point, rightHemiepsCenter) - radius;
        
        // Return minimum distance to any part of the bacterium
        double minDistance = Math.min(cylinderDistance, Math.min(leftHemiepsDistance, rightHemiepsDistance));
        return Math.max(0.0, minDistance);
    }
    
    /**
     * Calculates minimum distance between two bacteria with hemispherical end caps.
     * 
     * This method considers all possible combinations of bacterial geometry:
     * - Cylinder vs cylinder
     * - Hemispheres vs cylinder
     * - Hemisphere vs hemisphere
     * 
     * @param b1 First bacterium
     * @param b2 Second bacterium
     * @return Minimum distance between bacterial surfaces
     */
    private static double bacteriumToBacteriumDistance(Bacterium b1, Bacterium b2) {
        // For bacterium-bacterium collision, we need to check:
        // 1. Cylinder of b1 vs entire bacterium of b2
        // 2. Hemiepss of b1 vs entire bacterium of b2
        
        double minDistance = Double.MAX_VALUE;
        
        // Get bacterium geometry for both bacteria
        Vec2[] endpoints1 = getBacteriumEndpoints(b1);
        Vec2[] endpoints2 = getBacteriumEndpoints(b2);
        
        // Calculate hemieps centers for b1
        Vec2 center1 = b1.getPosition();
        Vec2 orientation1 = b1.getOrientation();
        double totalHalfLength1 = b1.getLength() / 2.0;
        
        Vec2 leftHemieps1 = new Vec2(
            center1.getX() - orientation1.getX() * totalHalfLength1,
            center1.getY() - orientation1.getY() * totalHalfLength1
        );
        
        Vec2 rightHemieps1 = new Vec2(
            center1.getX() + orientation1.getX() * totalHalfLength1,
            center1.getY() + orientation1.getY() * totalHalfLength1
        );
        
        // Calculate hemieps centers for b2
        Vec2 center2 = b2.getPosition();
        Vec2 orientation2 = b2.getOrientation();
        double totalHalfLength2 = b2.getLength() / 2.0;
        
        Vec2 leftHemieps2 = new Vec2(
            center2.getX() - orientation2.getX() * totalHalfLength2,
            center2.getY() - orientation2.getY() * totalHalfLength2
        );
        
        Vec2 rightHemieps2 = new Vec2(
            center2.getX() + orientation2.getX() * totalHalfLength2,
            center2.getY() + orientation2.getY() * totalHalfLength2
        );
        
        // Check all possible combinations:
        
        // 1. Cylinder of b1 vs cylinder of b2
        double cylinderDistance = lineSegmentToLineSegmentDistance(
            endpoints1[0], endpoints1[1], endpoints2[0], endpoints2[1]
        );
        minDistance = Math.min(minDistance, cylinderDistance);
        
        // 2. Hemiepss of b1 vs cylinder of b2
        minDistance = Math.min(minDistance, pointToLineSegmentDistance(leftHemieps1, endpoints2[0], endpoints2[1]));
        minDistance = Math.min(minDistance, pointToLineSegmentDistance(rightHemieps1, endpoints2[0], endpoints2[1]));
        
        // 3. Cylinder of b1 vs hemiepss of b2
        minDistance = Math.min(minDistance, pointToLineSegmentDistance(leftHemieps2, endpoints1[0], endpoints1[1]));
        minDistance = Math.min(minDistance, pointToLineSegmentDistance(rightHemieps2, endpoints1[0], endpoints1[1]));
        
        // 4. Hemieps vs hemieps (eps-eps distance)
        minDistance = Math.min(minDistance, distance(leftHemieps1, leftHemieps2));
        minDistance = Math.min(minDistance, distance(leftHemieps1, rightHemieps2));
        minDistance = Math.min(minDistance, distance(rightHemieps1, leftHemieps2));
        minDistance = Math.min(minDistance, distance(rightHemieps1, rightHemieps2));
        
        return minDistance;
    }
    
    /**
     * Gets the endpoints of a bacterium's cylindrical body (excluding hemispherical caps).
     * 
     * @param bacterium Bacterium to get endpoints for
     * @return Array containing start and end points of the cylindrical body
     */
    private static Vec2[] getBacteriumEndpoints(Bacterium bacterium) {
        Vec2 center = bacterium.getPosition();
        Vec2 orientation = bacterium.getOrientation();
        
        // For bacterium collision detection, we use the cylindrical body length
        // The bacterium extends from center minus half cylindrical length to center plus half cylindrical length
        double cylindricalLength = bacterium.getLength() - bacterium.getDiameter();
        double halfLength = Math.max(0.0, cylindricalLength / 2.0);
        
        // Calculate endpoints along the orientation vector
        double dx = orientation.getX() * halfLength;
        double dy = orientation.getY() * halfLength;
        
        Vec2 start = new Vec2(center.getX() - dx, center.getY() - dy);
        Vec2 end = new Vec2(center.getX() + dx, center.getY() + dy);
        
        return new Vec2[]{start, end};
    }
    
    /**
     * Calculates the Euclidean distance between two points.
     * 
     * @param p1 First point
     * @param p2 Second point
     * @return Euclidean distance between the points
     */
    private static double distance(Vec2 p1, Vec2 p2) {
        double dx = p1.getX() - p2.getX();
        double dy = p1.getY() - p2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculates the distance from a point to a line segment.
     * 
     * @param point Point to measure distance from
     * @param segStart Start point of the line segment
     * @param segEnd End point of the line segment
     * @return Distance from point to the closest point on the line segment
     */
    private static double pointToLineSegmentDistance(Vec2 point, Vec2 segStart, Vec2 segEnd) {
        double dx = segEnd.getX() - segStart.getX();
        double dy = segEnd.getY() - segStart.getY();
        
        if (dx == 0 && dy == 0) {
            // Degenerate segment - just point to point distance
            return distance(point, segStart);
        }
        
        // Parameter t for closest point on line segment
        double t = ((point.getX() - segStart.getX()) * dx + (point.getY() - segStart.getY()) * dy) / (dx * dx + dy * dy);
        
        // Clamp t to [0, 1] to stay on segment
        t = Math.max(0, Math.min(1, t));
        
        // Closest point on segment
        Vec2 closest = new Vec2(
            segStart.getX() + t * dx,
            segStart.getY() + t * dy
        );
        
        return distance(point, closest);
    }
    
    /**
     * Calculates the distance between two line segments.
     * 
     * @param seg1Start Start point of first line segment
     * @param seg1End End point of first line segment
     * @param seg2Start Start point of second line segment
     * @param seg2End End point of second line segment
     * @return Minimum distance between the two line segments
     */
    private static double lineSegmentToLineSegmentDistance(Vec2 seg1Start, Vec2 seg1End, Vec2 seg2Start, Vec2 seg2End) {
        // Simplified approach: check all endpoint combinations and find minimum
        double minDistance = Double.MAX_VALUE;
        
        // Distance from each endpoint of seg1 to seg2
        minDistance = Math.min(minDistance, pointToLineSegmentDistance(seg1Start, seg2Start, seg2End));
        minDistance = Math.min(minDistance, pointToLineSegmentDistance(seg1End, seg2Start, seg2End));
        
        // Distance from each endpoint of seg2 to seg1
        minDistance = Math.min(minDistance, pointToLineSegmentDistance(seg2Start, seg1Start, seg1End));
        minDistance = Math.min(minDistance, pointToLineSegmentDistance(seg2End, seg1Start, seg1End));
        
        return minDistance;
    }
    
    /**
     * Calculates the contact point between two particles.
     * This is where the repulsive force is applied during collision.
     * 
     * @param a First particle
     * @param b Second particle
     * @param parallelEps Small tolerance for handling parallel vectors
     * @return Contact point between the two particles
     */
    public static Vec2 getContactPoint(Particle a, Particle b, double parallelEps) {
        Vec2 posA = a.getPosition();
        Vec2 posB = b.getPosition();

        // Calculate the vector from B to A
        double dx = posA.getX() - posB.getX();
        double dy = posA.getY() - posB.getY();
        double distance = Math.hypot(dx, dy);

        if (distance <= parallelEps) {
            // Particles are at same position - use position A
            return posA;
        }

        // Normalize the direction vector
        double unitX = dx / distance;
        double unitY = dy / distance;

        // For different particle types, we need different contact point calculations
        if (a instanceof Bacterium && b instanceof Bacterium) {
            return getBacteriumBacteriumContactPoint((Bacterium) a, (Bacterium) b, unitX, unitY);
        } else if (a instanceof EPS && b instanceof EPS) {
            return getEpsEpsContactPoint((EPS) a, (EPS) b, unitX, unitY);
        } else if ((a instanceof Bacterium && b instanceof EPS) || (a instanceof EPS && b instanceof Bacterium)) {
            Bacterium bacterium = (a instanceof Bacterium) ? (Bacterium) a : (Bacterium) b;
            EPS eps = (a instanceof EPS) ? (EPS) a : (EPS) b;
            return getEpsBacteriumContactPoint(eps, bacterium, unitX, unitY, parallelEps);
        }

        // Fallback: use midpoint between centers
        return new Vec2(
            (posA.getX() + posB.getX()) / 2.0,
            (posA.getY() + posB.getY()) / 2.0
        );
    }
    
    /**
     * Calculates contact point for EPS-EPS collision.
     * 
     * @param eps1 First EPS particle
     * @param eps2 Second EPS particle
     * @param unitX Unit vector X component (from eps2 to eps1)
     * @param unitY Unit vector Y component (from eps2 to eps1)
     * @return Contact point on the surface of eps1
     */
    private static Vec2 getEpsEpsContactPoint(EPS eps1, EPS eps2, double unitX, double unitY) {
        Vec2 pos1 = eps1.getPosition();
        
        // Contact point is at the surface of eps1, along the line connecting centers
        double contactX = pos1.getX() - unitX * eps1.getRadius();
        double contactY = pos1.getY() - unitY * eps1.getRadius();
        
        return new Vec2(contactX, contactY);
    }
    
    /**
     * Calculates contact point for EPS-Bacterium collision.
     * 
     * @param eps EPS particle (sphere)
     * @param bacterium Bacterium particle (spherocylinder)
     * @param unitX Unit vector X component (from bacterium to eps)
     * @param unitY Unit vector Y component (from bacterium to eps)
     * @param parallelEps Small tolerance for handling parallel vectors
     * @return Contact point on the surface of the EPS
     */
    private static Vec2 getEpsBacteriumContactPoint(EPS eps, Bacterium bacterium, double unitX, double unitY, double parallelEps) {
        Vec2 epsPos = eps.getPosition();
        
        // Find the closest point on the bacterium to the EPS center
        Vec2 closestPoint = findClosestPointOnBacterium(epsPos, bacterium);
        
        // Contact point is at the surface of the EPS, along the line from EPS center to closest point
        double dx = closestPoint.getX() - epsPos.getX();
        double dy = closestPoint.getY() - epsPos.getY();
        double distance = Math.hypot(dx, dy);
        
        if (distance <= parallelEps) {
            return epsPos;
        }
        
        double contactX = epsPos.getX() + (dx / distance) * eps.getRadius();
        double contactY = epsPos.getY() + (dy / distance) * eps.getRadius();
        
        return new Vec2(contactX, contactY);
    }
    
    /**
     * Calculates contact point for Bacterium-Bacterium collision.
     * 
     * @param b1 First bacterium (spherocylinder)
     * @param b2 Second bacterium (spherocylinder)
     * @param unitX Unit vector X component (from b2 to b1)
     * @param unitY Unit vector Y component (from b2 to b1)
     * @return Contact point between the two bacteria
     */
    private static Vec2 getBacteriumBacteriumContactPoint(Bacterium b1, Bacterium b2, double unitX, double unitY) {
        // For rod-rod collision, find the closest points on each bacterium
        Vec2[] endpoints1 = getBacteriumEndpoints(b1);
        Vec2[] endpoints2 = getBacteriumEndpoints(b2);
        
        // Find closest points between the two line segments
        Vec2[] closestPoints = findClosestPointsOnLineSegments(endpoints1[0], endpoints1[1], endpoints2[0], endpoints2[1]);
        
        // Contact point is the midpoint between the closest points
        return new Vec2(
            (closestPoints[0].getX() + closestPoints[1].getX()) / 2.0,
            (closestPoints[0].getY() + closestPoints[1].getY()) / 2.0
        );
    }
    
    /**
     * Finds the closest point on a bacterium to a given point.
     * 
     * @param point Point to find closest point to
     * @param bacterium Bacterium to find closest point on
     * @return Closest point on the bacterium's surface
     */
    public static Vec2 findClosestPointOnBacterium(Vec2 point, Bacterium bacterium) {
        Vec2[] endpoints = getBacteriumEndpoints(bacterium);
        return findClosestPointOnLineSegment(point, endpoints[0], endpoints[1]);
    }
    
    /**
     * Finds the closest point on a line segment to a given point.
     * 
     * @param point Point to find closest point to
     * @param segStart Start point of the line segment
     * @param segEnd End point of the line segment
     * @return Closest point on the line segment
     */
    public static Vec2 findClosestPointOnLineSegment(Vec2 point, Vec2 segStart, Vec2 segEnd) {
        double dx = segEnd.getX() - segStart.getX();
        double dy = segEnd.getY() - segStart.getY();
        
        if (dx == 0 && dy == 0) {
            return segStart;
        }
        
        // Parameter t for closest point on line segment
        double t = ((point.getX() - segStart.getX()) * dx + (point.getY() - segStart.getY()) * dy) / (dx * dx + dy * dy);
        
        // Clamp t to [0, 1] to stay on segment
        t = Math.max(0, Math.min(1, t));
        
        // Closest point on segment
        return new Vec2(
            segStart.getX() + t * dx,
            segStart.getY() + t * dy
        );
    }
    
    /**
     * Finds the closest points between two line segments.
     * 
     * @param seg1Start Start point of first line segment
     * @param seg1End End point of first line segment
     * @param seg2Start Start point of second line segment
     * @param seg2End End point of second line segment
     * @return Array containing the closest point on each line segment
     */
    public static Vec2[] findClosestPointsOnLineSegments(Vec2 seg1Start, Vec2 seg1End, Vec2 seg2Start, Vec2 seg2End) {
        // Simplified approach: find closest point on seg1 to seg2Start and seg2End
        Vec2 closest1 = findClosestPointOnLineSegment(seg2Start, seg1Start, seg1End);
        Vec2 closest2 = findClosestPointOnLineSegment(seg2End, seg1Start, seg1End);
        
        // Return the pair that gives minimum distance
        double dist1 = Math.hypot(closest1.getX() - seg2Start.getX(), closest1.getY() - seg2Start.getY());
        double dist2 = Math.hypot(closest2.getX() - seg2End.getX(), closest2.getY() - seg2End.getY());
        
        if (dist1 <= dist2) {
            return new Vec2[]{closest1, seg2Start};
        } else {
            return new Vec2[]{closest2, seg2End};
        }
    }
    
}
