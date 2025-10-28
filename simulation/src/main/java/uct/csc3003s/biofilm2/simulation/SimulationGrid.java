package uct.csc3003s.biofilm2.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import uct.csc3003s.biofilm2.parser.ConfigParser;
import uct.csc3003s.biofilm2.particle.Bacterium;
import uct.csc3003s.biofilm2.particle.EPS;
import uct.csc3003s.biofilm2.particle.Particle;
import uct.csc3003s.biofilm2.physics.Force;
import uct.csc3003s.biofilm2.physics.RepulsiveForce;
import uct.csc3003s.biofilm2.physics.RepulsiveTorque;
import uct.csc3003s.biofilm2.physics.MotilityForce;
import uct.csc3003s.biofilm2.physics.Velocity;
import uct.csc3003s.biofilm2.physics.RandomForce;
import uct.csc3003s.biofilm2.util.CollisionDetector;
import uct.csc3003s.biofilm2.util.Vec2;
import uct.csc3003s.biofilm2.spatial.SpatialHash;

/**
 * Main simulation grid that manages the biofilm simulation.
 * 
 * This class coordinates all aspects of the biofilm simulation including:
 * - Particle management (bacteria and EPS)
 * - Nutrient field dynamics
 * - Force calculations and particle motion
 * - Collision detection
 * - Bacterial growth, division, and EPS production
 * - Parallel processing for performance
 */
public class SimulationGrid {
    private double width;                                    // Simulation domain width
    private double height;                                   // Simulation domain height
    private List<Particle> particles;                        // All particles in the simulation
    private NutrientField nutrientField;                     // Nutrient concentration field
    
    // Friction coefficients for different particle types
    private double zetaCell = ConfigParser.frictionCoefficientCell; // Friction coefficient ζ for cells
    private double zetaEPS = ConfigParser.frictionCoefficientEps;   // Friction coefficient ζ for EPS
    private double neighborRadius = 4.0;                     // Radius for neighbor search and pair forces
    private double maxAngularSpeed = Math.toRadians(720);    // Maximum angular velocity (safety clamp)
    private SpatialHash grid = new SpatialHash(neighborRadius); // Spatial hash for efficient neighbor lookup
    private double[] xStep, yStep, vXStep, vYStep, omegaStep; // Arrays for parallel force calculations
    private java.util.HashMap<Particle, Integer> idxOf = new java.util.HashMap<>(); // Particle index mapping

    static Random r = new Random();                          // Random number generator for initialization

    /**
     * Constructs a new simulation grid with the specified dimensions.
     * 
     * @param width Simulation domain width
     * @param height Simulation domain height
     */
    public SimulationGrid(double width, double height) {
        this.width = width;
        this.height = height;

        this.particles = new ArrayList<Particle>();
        // Create dynamic nutrient field with configurable parameters
        double gridDomainWidth = ConfigParser.gridWidth * ConfigParser.gridCellSize;
        double gridDomainHeight = ConfigParser.gridHeight * ConfigParser.gridCellSize;
        this.nutrientField = new NutrientField(ConfigParser.gridWidth, ConfigParser.gridHeight, 
                                              gridDomainWidth, gridDomainHeight);
    }

    /**
     * Gets the simulation domain width.
     * @return Domain width
     */
    public double getWidth() {
        return width;
    }

    /**
     * Gets the simulation domain height.
     * @return Domain height
     */
    public double getHeight() {
        return height;
    }

    /**
     * Gets all particles in the simulation.
     * @return List of all particles
     */
    public List<Particle> getParticles() {
        return particles;
    }

    /**
     * Adds a particle to the simulation.
     * @param p Particle to add
     */
    public void addParticle(Particle p) {
        particles.add(p);
    }

    /**
     * Calculates the total area of all bacterial cells in the simulation.
     * @return Total bacterial area
     */
    public double calculateSumArea() {
        double sumArea = 0.0;
        for (Particle particle : particles) {
            if (particle instanceof Bacterium) {
                sumArea += ((Bacterium) particle).calculateArea();
            }
        }
        return sumArea;
    }

    /**
     * Calculates the local bacterial cell density around a given position.
     * Uses spatial hashing for efficient neighbor lookup.
     * 
     * @param position Center position for density calculation
     * @param radius Search radius
     * @return Total bacterial area within the radius
     */
    public double calculateLocalCellDensity(Vec2 position, double radius) {
        final double x = position.getX();
        final double y = position.getY();
        final double r2 = radius * radius;
    
        final double[] acc = new double[1]; // Lambda-friendly accumulator
    
        // Iterate only indices in tiles near (x,y) using spatial hash
        grid.forEachIndexNear(x, y, radius, j -> {
            Particle pj = particles.get(j);
            if (pj instanceof Bacterium b) {
                double dx = x - b.getPosition().getX();
                double dy = y - b.getPosition().getY();
                if (dx*dx + dy*dy <= r2) {
                    acc[0] += b.calculateArea(); // Accumulate bacterial area
                }
            }
        });

        return acc[0];
    }

    /**
     * Calculates the local EPS density around a given position.
     * Uses spatial hashing for efficient neighbor lookup.
     * 
     * @param position Center position for density calculation
     * @param radius Search radius
     * @return Total EPS area within the radius
     */
    public double calculateLocalEPSDensity(Vec2 position, double radius) {
        final double x = position.getX();
        final double y = position.getY();
        final double r2 = radius * radius;
    
        final double[] acc = new double[1]; // Lambda-friendly accumulator
    
        grid.forEachIndexNear(x, y, radius, j -> {
            Particle pj = particles.get(j);
            if (pj instanceof EPS) {
                EPS eps = (EPS) pj;
                double dx = x - eps.getPosition().getX();
                double dy = y - eps.getPosition().getY();
                if (dx*dx + dy*dy <= r2) {
                    double epsArea = Math.PI * eps.getRadius() * eps.getRadius();
                    acc[0] += epsArea;
                }
            }
        });
    
        return acc[0];
    }

    /**
     * Initializes the simulation by creating initial bacterial cells.
     * Places bacteria in a circular cluster at the center of the domain.
     */
    public void start() {
        // Create initial bacteria based on config initial_count
        double centerX = width / 2;
        double centerY = height / 2;
        double clusterRadius = ConfigParser.diameter * 2.0; // Radius within which to place initial bacteria

        for (int i = 0; i < ConfigParser.initialCount; i++) {
            // Generate random position within cluster radius (uniform distribution in circle)
            double angle = r.nextDouble() * 2 * Math.PI;
            double radius = Math.sqrt(r.nextDouble()) * clusterRadius;

            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            // Random orientation
            double orientAngle = r.nextDouble() * 2 * Math.PI;
            Vec2 orientation = new Vec2(Math.cos(orientAngle), Math.sin(orientAngle));

            // Start with initial length (half of maximum length for more realistic growth)
            double initialLength = ConfigParser.length / 2.0;
            Bacterium bacterium = new Bacterium(i, ConfigParser.diameter, new Vec2(x, y), new Vec2(0, 0), orientation,
                    initialLength);
            addParticle(bacterium);
        }
    }

    /**
     * Rotates a particle's orientation by the specified angle.
     * 
     * @param p Particle whose orientation to rotate
     * @param dtheta Angle of rotation in radians
     */
    private static void rotateOrientation(Particle p, double dtheta) {
        var u = p.getOrientation();
        double ux = u.getX(), uy = u.getY();
        double c = Math.cos(dtheta), s = Math.sin(dtheta);
        double rx = ux * c - uy * s;
        double ry = ux * s + uy * c;
        double n = Math.hypot(rx, ry);
        if (n == 0)
            return;
        p.setOrientation(new Vec2(rx / n, ry / n));
    }

    /**
     * Updates the simulation for one time step.
     * This is the main simulation loop that handles:
     * - Nutrient field updates
     * - Bacterial growth, division, and EPS production
     * - Force calculations and particle motion
     * - Collision detection
     * 
     * @param deltaTime Time step size
     */
    public void updateSimulation(double deltaTime) {
        // Get all bacteria for nutrient field calculation
        List<Bacterium> bacteria = new ArrayList<>();
        for (Particle particle : particles) {
            if (particle instanceof Bacterium) {
                bacteria.add((Bacterium) particle);
            }
        }

        // Update nutrient field using diffusion equation
        nutrientField.updateField(deltaTime, bacteria);

        // Update bacterial growth based on local nutrient concentration
        List<Bacterium> newBacteria = new ArrayList<>();
        List<EPS> newEPS = new ArrayList<>();
        List<Particle> toRemove = new ArrayList<>();
        int nextEPSId = getNextEPSId();
        final var nextEPS = new java.util.concurrent.atomic.AtomicInteger(nextEPSId);
final var qDaughters = new java.util.concurrent.ConcurrentLinkedQueue<Bacterium>();
final var qNewEPS    = new java.util.concurrent.ConcurrentLinkedQueue<EPS>();
final var qRemove    = new java.util.concurrent.ConcurrentLinkedQueue<Particle>();

final long seed = 12345L; // or from config
final ThreadLocal<java.util.SplittableRandom> TL_RNG =
    ThreadLocal.withInitial(() -> new java.util.SplittableRandom(seed ^ Thread.currentThread().getId()));

java.util.stream.IntStream.range(0, bacteria.size()).parallel().forEach(idx -> {
    Bacterium b = bacteria.get(idx);

    double growthRate = nutrientField.calculateMonodFuncAtPosition(
        b.getPosition().getX(), b.getPosition().getY());
    b.grow(growthRate, deltaTime);

    double R = ConfigParser.localSensingRadius;
    double rhoC = calculateLocalCellDensity(b.getPosition(), R);
    double rhoE = calculateLocalEPSDensity(b.getPosition(), R);

    if (b.shouldProduceEPS(rhoC, rhoE)) {
        double p = ConfigParser.epsProductionRate / 10.0;
        if (TL_RNG.get().nextDouble() < p) {
            qNewEPS.add(b.produceEPS(nextEPS.getAndIncrement()));
        }
    }

    if (b.shouldDivide()) {
        var daughters = b.divide();
        if (!daughters.isEmpty()) {
            qDaughters.addAll(daughters);
            qRemove.add(b);
        }
    }
});

// single-thread merge
newBacteria.addAll(qDaughters);
newEPS.addAll(qNewEPS);
toRemove.addAll(qRemove);
nextEPSId = nextEPS.get();

        particles.removeAll(toRemove);
        particles.addAll(newBacteria);
        particles.addAll(newEPS);
        var allParticles = getParticles();
        int n = allParticles.size();
        if (xStep == null || xStep.length < n) {
            xStep = new double[n];
            yStep = new double[n];
            vXStep = new double[n];
            vYStep = new double[n];
            omegaStep = new double[n];
        }
        
        // Populate arrays and index mapping
        idxOf.clear();
        for (int i = 0; i < n; i++) {
            Particle p = allParticles.get(i);
            xStep[i] = p.getPosition().getX();
            yStep[i] = p.getPosition().getY();
            idxOf.put(p, i);
        }
        
        // Rebuild the spatial hash
        grid.rebuild(xStep, yStep, n);
        double parallelTol = 1e-12;

        Force rfForce = new RepulsiveForce(parallelTol);
        Force mtForce = new MotilityForce(ConfigParser.motilityForce);
        Force rForce = new RandomForce();
        
        // Create torque for repulsive forces
        Velocity.Torque rfTorque = new RepulsiveTorque(parallelTol);
        final java.util.List<Force> FORCES_BACTERIUM = java.util.List.of(rfForce, mtForce, rForce);
        final java.util.List<Force> FORCES_EPS       = java.util.List.of(rfForce, rForce);
        final java.util.List<Velocity.Torque> TORQUES_BACTERIUM = java.util.List.of(rfTorque);
        final java.util.List<Velocity.Torque> TORQUES_EPS       = java.util.List.of(); // empty
        final int N = allParticles.size();

        IntStream.range(0, N).parallel().forEach(i -> {
            Particle p = allParticles.get(i);
            Particle[] neigh = findNearbyParticlesGrid(p).toArray(Particle[]::new);

            final java.util.List<Force> forces;
            final java.util.List<Velocity.Torque> torques;

            if (p instanceof Bacterium) {
                forces = FORCES_BACTERIUM;
                torques = TORQUES_BACTERIUM;
            } else { // EPS or other spherical
                forces = FORCES_EPS;
                torques = TORQUES_EPS;
            }

            double friction = (p instanceof EPS) ? zetaEPS : zetaCell;
            Vec2 v = Velocity.linearVelocity(p, friction, forces, neigh);
            double omega = Velocity.angularVelocity(p, friction, torques, neigh);
            if (Double.isFinite(maxAngularSpeed)) {
                omega = Math.max(-maxAngularSpeed, Math.min(maxAngularSpeed, omega));
            }
            vXStep[i] = v.getX();
            vYStep[i] = v.getY();
            omegaStep[i] = omega;
        });

        IntStream.range(0, N).parallel().forEach(i -> {
            Particle p = allParticles.get(i);
            p.setPosition(new Vec2(
                    p.getPosition().getX() + vXStep[i] * deltaTime,
                    p.getPosition().getY() + vYStep[i] * deltaTime
            ));
            rotateOrientation(p, omegaStep[i] * deltaTime);
        });
        // Collision detection (detection only, no resolution)
        // Note: Collision detection is performed but not used for resolution
        detectCollisions();
    }

    /**
     * Gets the nutrient field.
     * @return The nutrient concentration field
     */
    public NutrientField getNutrientField() {
        return nutrientField;
    }

    /**
     * Generates the next unique EPS ID.
     * EPS IDs start from 10000 to avoid conflicts with bacterial IDs.
     * 
     * @return Next available EPS ID
     */
    private int getNextEPSId() {
        // Generate unique EPS IDs starting from 10000
        int maxId = 10000;
        for (Particle particle : particles) {
            if (particle instanceof EPS && particle.getParticleId() >= maxId) {
                maxId = particle.getParticleId() + 1;
            }
        }
        return maxId;
    }

    /**
     * Detects all collisions in the current particle system using parallel processing.
     * Uses thread-local buffers and spatial hashing for efficient collision detection.
     * 
     * @return List of collision pairs
     */
    public List<CollisionPair> detectCollisions() {
    // snapshot once
    final List<Particle> P = getParticles();
    final int N = P.size();

    // thread-local buffers (no contention)
    final ThreadLocal<ArrayList<CollisionPair>> TL = ThreadLocal.withInitial(ArrayList::new);

    java.util.stream.IntStream.range(0, N).parallel().forEach(i -> {
        Particle p1 = P.get(i);
        ArrayList<CollisionPair> buf = TL.get();

        // enumerate neighbors from spatial hash; j>i to avoid duplicates
        grid.forEachNeighborIndex(i, xStep, yStep, j -> {
            if (j <= i) return; // keep one canonical ordering
            Particle p2 = P.get(j);
            if (CollisionDetector.isColliding(p1, p2)) {
                buf.add(new CollisionPair(p1, p2));
            }
        });
    });

    // merge all thread-local lists
    ArrayList<CollisionPair> collisions = new ArrayList<>();
    // Collect TL instances via thread locals not directly iterable: keep a concurrent bag
    java.util.Set<ArrayList<CollisionPair>> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    java.util.stream.IntStream.range(0, Math.max(1, Runtime.getRuntime().availableProcessors())).forEach(i -> seen.add(TL.get()));
    for (ArrayList<CollisionPair> buf : seen) collisions.addAll(buf);

    return collisions;
}

    /**
     * Finds all particles within a given radius of a target particle.
     * 
     * @param target The particle to search around
     * @param radius Search radius
     * @return List of nearby particles
     */
    public List<Particle> findNearbyParticles(Particle target, double radius) {
        List<Particle> nearby = new ArrayList<>();
        Vec2 targetPos = target.getPosition();

        for (Particle particle : particles) {
            if (particle == target)
                continue;

            Vec2 pos = particle.getPosition();
            double dx = targetPos.getX() - pos.getX();
            double dy = targetPos.getY() - pos.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance <= radius) {
                nearby.add(particle);
            }
        }

        return nearby;
    }
    /**
     * Finds nearby particles using spatial hashing for efficient neighbor lookup.
     * 
     * @param target The particle to search around
     * @return List of nearby particles
     */
    private java.util.List<Particle> findNearbyParticlesGrid(Particle target) {
        Integer iObj = idxOf.get(target);
        if (iObj == null)
            return java.util.List.of();
        final int i = iObj;

        final double r2 = neighborRadius * neighborRadius;
        java.util.ArrayList<Particle> out = new java.util.ArrayList<>();

        grid.forEachNeighborIndex(i, xStep, yStep, j -> {
            if (j == i)
                return;
            double dx = xStep[i] - xStep[j];
            double dy = yStep[i] - yStep[j];
            if (dx * dx + dy * dy <= r2) {
                out.add(particles.get(j));
            }
        });
        return out;
    }

    /**
     * Checks if a particle is colliding with any other particle.
     * Uses brute force search for collision detection.
     * 
     * @param particle The particle to check
     * @return True if colliding with any other particle
     */
    public boolean isParticleColliding(Particle particle) {
        for (Particle other : particles) {
            if (other == particle)
                continue;

            if (CollisionDetector.isColliding(particle, other)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the minimum distance from a particle to any other particle.
     * Uses brute force search to find the nearest neighbor.
     * 
     * @param particle The particle to check
     * @return Minimum distance to nearest particle
     */
    public double getMinimumDistanceToOthers(Particle particle) {
        double minDistance = Double.MAX_VALUE;

        for (Particle other : particles) {
            if (other == particle)
                continue;

            double distance = CollisionDetector.getMinimumDistance(particle, other);
            minDistance = Math.min(minDistance, distance);
        }

        return minDistance;
    }

    /**
     * Container class for collision pairs.
     * Represents two particles that are colliding.
     */
    public static class CollisionPair {
        private final Particle particle1;
        private final Particle particle2;

        /**
         * Constructs a collision pair.
         * @param p1 First particle in the collision
         * @param p2 Second particle in the collision
         */
        public CollisionPair(Particle p1, Particle p2) {
            this.particle1 = p1;
            this.particle2 = p2;
        }

        /**
         * Gets the first particle in the collision.
         * @return First particle
         */
        public Particle getParticle1() {
            return particle1;
        }

        /**
         * Gets the second particle in the collision.
         * @return Second particle
         */
        public Particle getParticle2() {
            return particle2;
        }
    }

}
