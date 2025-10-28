package uct.csc3003s.biofilm2.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import uct.csc3003s.biofilm2.parser.ConfigParser;
import uct.csc3003s.biofilm2.util.Vec2;

/**
 * Represents a bacterial cell in the biofilm simulation.
 * 
 * This class models individual bacterial cells with properties. 
 */
public class Bacterium extends Particle {
    private double length;              // Current length of the bacterial cell in micrometers

    /**
     * Constructs a new bacterial cell with the specified properties.
     * 
     * @param id Unique bacterial identifier
     * @param diameter Bacterial diameter in micrometers
     * @param pos Initial position in 2D space
     * @param vel Initial velocity vector
     * @param orientation Initial orientation vector
     * @param length Initial length in micrometers
     */
    public Bacterium(int id, double diameter, Vec2 pos, Vec2 vel, Vec2 orientation, double length) {
        super(id, diameter, pos, vel, orientation);
        this.length = length;
    }

    /**
     * Simulates bacterial growth based on nutrient availability and growth rate.
     * Growth is proportional to the cell's surface area and follows the research paper specifications.
     * 
     * @param growthRate Local growth rate based on nutrient concentration
     * @param deltaTime Time step for growth calculation
     * @return true if the bacterium grew, false otherwise
     */
    public boolean grow(double growthRate, double deltaTime) {
        double phi = ConfigParser.growthRate; 
        
        // Calculate current cell surface area (end caps + cylindrical body)
        double r0 = this.getDiameter() / 2.0; // Radius of end caps
        double individualArea = Math.PI * r0 * r0 + 2.0 * r0 * (this.length-this.getDiameter());
        
        // Calculate average area for normalization
        double lc = ConfigParser.divisionLength;
        double averageArea = Math.PI * r0 * r0 + (3.0/2.0) * r0 * lc;
        
        // Calculate length increase based on area ratio and growth rate
        double lengthIncrease = phi * (individualArea / averageArea) * growthRate * deltaTime;
        
        if (lengthIncrease > 0) {
            length += lengthIncrease;
            return true;
        }
        return false;
    }
    
    /**
     * Determines if the bacterium is ready to divide based on its current length.
     * Division occurs when the bacterium reaches the critical length threshold.
     * 
     * @return true if the bacterium should divide, false otherwise
     */
    public boolean shouldDivide() {
        double criticalLength = ConfigParser.divisionLength;
        return length >= criticalLength;
    }

    /**
     * Performs bacterial cell division, creating two daughter cells.
     * The parent cell is split in half along its orientation axis, with slight
     * random variations in daughter cell orientations to simulate biological variation.
     * 
     * @return List containing the two daughter cells, or empty list if division is not possible
     */
    public List<Bacterium> divide() {
        List<Bacterium> listToReturn = new ArrayList<Bacterium>();
        
        if (!shouldDivide()) {
            return listToReturn; // No division if not ready
        }
        
        Vec2 currentPos = this.getPosition();
        Vec2 currentOri = this.getOrientation();
        
        double newLength = this.length / 2.0;
        
        // Calculate offset for daughter cells along the orientation axis
        double offset = (newLength / 2.0); 
        double offsetX = currentOri.getX() * offset;
        double offsetY = currentOri.getY() * offset;
        
        // Create two daughter cells positioned as if the parent was cut in half
        Vec2 daughter1Pos = new Vec2(currentPos.getX() - offsetX, currentPos.getY() - offsetY);
        Vec2 daughter2Pos = new Vec2(currentPos.getX() + offsetX, currentPos.getY() + offsetY);

        // Generate slight random orientation variations (±8 degrees)
        Random random = new Random();
        double maxAngleVariation = Math.toRadians(8.0); // ±8 degrees in radians
    
        // Daughter 1: slight random rotation
        double angle1 = (random.nextDouble() - 0.5) * 2 * maxAngleVariation;
        Vec2 daughter1Ori = currentOri.rotateVector(angle1);
    
        // Daughter 2: different slight random rotation  
        double angle2 = (random.nextDouble() - 0.5) * 2 * maxAngleVariation;
        Vec2 daughter2Ori = currentOri.rotateVector(angle2);
        
        // Create daughter cells with new IDs and varied orientations
        Bacterium daughter1 = new Bacterium(this.id + 1000, this.getDiameter(), daughter1Pos, new Vec2(0, 0), daughter1Ori, newLength);
        Bacterium daughter2 = new Bacterium(this.id + 2000, this.getDiameter(), daughter2Pos, new Vec2(0, 0), daughter2Ori, newLength);
        
        listToReturn.add(daughter1);
        listToReturn.add(daughter2);

        return listToReturn;
    }

    /**
     * Determines if the bacterium should produce EPS based on local density conditions.
     * EPS production is triggered when cell density exceeds threshold and EPS density
     * is below the maximum limit.
     * 
     * @param localCellDensity Local bacterial cell density in μm^2
     * @param localEPSDensity Local EPS density in μm^2
     * @return true if EPS should be produced, false otherwise
     */
    public boolean shouldProduceEPS(double localCellDensity, double localEPSDensity) {
        double cellDensityThreshold = ConfigParser.cellDensityThreshold; 
        double epsDensityLimit = ConfigParser.epsDensityThreshold; 
        
        return localCellDensity >= cellDensityThreshold && localEPSDensity < epsDensityLimit;
    }
    
    /**
     * Creates a new EPS particle near the bacterium's position.
     * EPS is placed at a random location within one EPS diameter of the bacterium,
     * with random orientation to simulate natural EPS production patterns.
     * 
     * @param epsId Unique identifier for the new EPS particle
     * @return New EPS particle positioned near the bacterium
     */
    public EPS produceEPS(int epsId) {
        double epsDiameter = ConfigParser.epsDiameter; 
        double epsRadius = epsDiameter / 2.0;
        
        // Generate EPS near the bacterium
        // Place EPS at a small random offset from the bacterium position
        Random random = new Random();
        double offsetDistance = epsDiameter; // Place EPS within one EPS diameter
        double angle = random.nextDouble() * 2 * Math.PI;
        
        Vec2 epsPosition = new Vec2(
            this.getPosition().getX() + offsetDistance * Math.cos(angle),
            this.getPosition().getY() + offsetDistance * Math.sin(angle)
        );
        
        // Create EPS particle with random orientation
        Vec2 epsOrientation = new Vec2(Math.cos(angle), Math.sin(angle));
        
        return new EPS(epsId, epsPosition, new Vec2(0, 0), epsOrientation, epsRadius);
    }
    
    
    /**
     * Gets the current length of the bacterial cell.
     * 
     * @return Current length in micrometers
     */
    public double getLength() {
        return length;
    }
    
    /**
     * Gets the diameter of the bacterial cell from configuration.
     * 
     * @return Bacterial diameter in micrometers (fixed value from config)
     */
    public double getDiameter() {
        return ConfigParser.diameter; // Fixed diameter d0
    }

    /**
     * Calculates the surface area of the bacterial cell.
     * Area includes both end caps and the cylindrical body surface.
     * 
     * @return Total surface area in square micrometers
     */
    public double calculateArea() {
        double radiusOfEndCaps = this.getDiameter()/2;
        // Use cylindrical length (total length - diameter) for lateral surface area
        double cylindricalLength = this.getLength() - this.getDiameter();
        double area = (Math.PI*Math.pow(radiusOfEndCaps, 2))+(2*radiusOfEndCaps*cylindricalLength);
        return area;
    }
}
