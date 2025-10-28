package uct.csc3003s.biofilm2.simulation;

import uct.csc3003s.biofilm2.particle.Bacterium;
import uct.csc3003s.biofilm2.parser.ConfigParser;
import java.util.List;

/**
 * Manages nutrient concentration field for the biofilm simulation.
 * 
 * This class implements a 2D nutrient field that simulates nutrient diffusion
 * and consumption by bacterial cells. The field uses finite difference methods
 * to solve the diffusion equation with consumption terms, employing parallel
 * processing and ping-pong buffering for efficient computation.
 */
public class NutrientField {
    private double[][] concentrationField;     // Current nutrient concentration field
    private int gridWidth, gridHeight;         // Grid dimensions
    private double initialConcentrationValue;  // Initial concentration value
    private double dx, dy;                     // Grid spacing in x and y directions
    private double minX, minY;                 // Grid origin coordinates
    
    private double diffusionConstant;          // Diffusion coefficient D
    private double consumptionRate;            // Nutrient consumption rate
    private double[][] newFieldBuf;            // Ping-pong buffer for field updates
    private double[][] areaGrid;               // Bacterial area density grid

    /**
     * Constructs a new nutrient field with the specified dimensions and parameters.
     * 
     * @param gridWidth Number of grid points in x-direction
     * @param gridHeight Number of grid points in y-direction
     * @param simWidth Physical width of the simulation domain
     * @param simHeight Physical height of the simulation domain
     */
    public NutrientField(int gridWidth, int gridHeight, double simWidth, double simHeight) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.dx = simWidth / (gridWidth - 1);
        this.dy = simHeight / (gridHeight - 1);
        this.initialConcentrationValue = ConfigParser.nutrientConcentration;
        this.diffusionConstant = ConfigParser.diffusionRate; 
        this.consumptionRate = ConfigParser.nutrientConsumptionRate; 
        
        this.minX = 0.0;
        this.minY = 0.0;

        // Initialize concentration field with uniform initial concentration
        this.concentrationField = new double[gridWidth][gridHeight];
        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                concentrationField[i][j] = initialConcentrationValue;
            }
        }
    }

    /**
     * Converts real-world coordinates to grid indices.
     * 
     * @param x Real-world x-coordinate
     * @param y Real-world y-coordinate
     * @return Grid indices [i, j] or null if coordinates are out of bounds
     */
    public int[] realToGrid(double x, double y) {
        int i = (int) Math.round((x - minX) / dx);
        int j = (int) Math.round((y - minY) / dy);

        // Check if coordinates are outside grid bounds
        if (i < 0 || i >= gridWidth || j < 0 || j >= gridHeight) {
            return null; // Indicate out-of-bounds
        }

        return new int[]{i,j};
    }
    
    /**
     * Checks if a real-world coordinate is within the current grid bounds.
     * 
     * @param x Real-world x-coordinate
     * @param y Real-world y-coordinate
     * @return True if coordinates are within grid bounds
     */
    public boolean isWithinBounds(double x, double y) {
        int[] gridPos = realToGrid(x, y);
        return gridPos != null;
    }
    

    /**
     * Calculates the Monod function value at a specific grid position.
     * The Monod function models nutrient-limited growth: μ(C) = C/(1+C)
     * 
     * @param x Grid x-coordinate
     * @param y Grid y-coordinate
     * @return Monod function value (growth rate factor)
     */
    public double calculateMonodFunc(int x, int y) {
        if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight) {
            return initialConcentrationValue / (1 + initialConcentrationValue); // Default for out-of-bounds
        }
        return concentrationField[x][y]/(1+concentrationField[x][y]);
    }
    
    /**
     * Calculates the Monod function value for a real-world coordinate.
     * 
     * @param realX Real-world x-coordinate
     * @param realY Real-world y-coordinate
     * @return Monod function value (growth rate factor)
     */
    public double calculateMonodFuncAtPosition(double realX, double realY) {
        int[] gridPos = realToGrid(realX, realY);
        if (gridPos == null) {
            return 0.1 / (1 + 0.1);
        }
        return calculateMonodFunc(gridPos[0], gridPos[1]);
    }

    /**
     * Gets the nutrient concentration value at a specific grid position.
     * 
     * @param x Grid x-coordinate
     * @param y Grid y-coordinate
     * @return Concentration value at the specified position
     */
    public double getConcentrationValue(int x, int y) {
        return concentrationField[x][y];
    }

    /**
     * Sets the nutrient concentration value at a specific grid position.
     * 
     * @param x Grid x-coordinate
     * @param y Grid y-coordinate
     * @param value New concentration value
     */
    public void setConcentrationValue(int x, int y, double value) {
        concentrationField[x][y] = value;
    }
    /**
     * Rebuilds the bacterial area density grid for consumption calculations.
     * This grid tracks the total bacterial area in each grid cell.
     * 
     * @param bacteria List of all bacteria in the simulation
     */
    private void rebuildAreaGrid(List<Bacterium> bacteria) {
        if (areaGrid == null || areaGrid.length != gridWidth || areaGrid[0].length != gridHeight) {
            areaGrid = new double[gridWidth][gridHeight];
        } else {
            for (int i = 0; i < gridWidth; i++) {
                java.util.Arrays.fill(areaGrid[i], 0.0);
            }
        }
    
        // Accumulate bacterial area in each grid cell
        for (int k = 0; k < bacteria.size(); k++) {
            Bacterium b = bacteria.get(k);
            int[] cell = realToGrid(b.getPosition().getX(), b.getPosition().getY());
            if (cell != null) {
                int x = cell[0], y = cell[1];
                if (x >= 0 && x < gridWidth && y >= 0 && y < gridHeight) {
                    areaGrid[x][y] += b.calculateArea(); // Aᵢ = π r₀² + 2 r₀ lᵢ
                }
            }
        }
    }

    /**
     * Calculates the nutrient consumption sink term for a specific grid position.
     * The consumption rate is proportional to bacterial area and follows Monod kinetics.
     * 
     * @param x Grid x-coordinate
     * @param y Grid y-coordinate
     * @param bacteria List of bacteria (used to ensure areaGrid is current)
     * @return Consumption sink term (negative rate of change)
     */
    public double calculateConsumptionSink(int x, int y, List<Bacterium> bacteria) {
        // areaGrid[x][y] was rebuilt this tick
        double C = concentrationField[x][y];
        double monod = C / (1.0 + C);              
        double totalArea = areaGrid[x][y];        
        return consumptionRate * totalArea * monod;
    }
    
    /**
     * Updates the nutrient field using the diffusion equation with consumption.
     * Implements: ∂C/∂t = D∇²C - R·A·μ(C)
     * where D is diffusion coefficient, R is consumption rate, A is bacterial area, and μ(C) is Monod function.
     * Uses parallel processing and ping-pong buffering for efficiency.
     * 
     * @param deltaTime Time step size
     * @param bacteria List of all bacteria for consumption calculations
     */
    public void updateField(double deltaTime, List<Bacterium> bacteria) {
        if (newFieldBuf == null || newFieldBuf.length != gridWidth || newFieldBuf[0].length != gridHeight) {
            newFieldBuf = new double[gridWidth][gridHeight];
        }
    
        rebuildAreaGrid(bacteria);
    
        final double invDx2 = 1.0 / (dx * dx);
        final double invDy2 = 1.0 / (dy * dy);
        final double D = diffusionConstant;
    
        java.util.stream.IntStream.range(1, gridWidth - 1).parallel().forEach(i -> {
            double[] colL = concentrationField[i - 1];
            double[] col  = concentrationField[i];
            double[] colR = concentrationField[i + 1];
            double[] out  = newFieldBuf[i];
            double[] area = areaGrid[i];
    
            for (int j = 1; j < gridHeight - 1; j++) {
                double d2C_dx2 = (colR[j] - 2.0 * col[j] + colL[j]) * invDx2;
                double d2C_dy2 = (col[j + 1] - 2.0 * col[j] + col[j - 1]) * invDy2;
                double diffusionTerm = D * (d2C_dx2 + d2C_dy2);
    
                double C = col[j];
                double monod = C / (1.0 + C);
                double consumptionTerm = consumptionRate * area[j] * monod;
    
                double cNew = C + deltaTime * (diffusionTerm - consumptionTerm);
                out[j] = (cNew >= 0.0) ? cNew : 0.0;
            }
        });
    
        // no-flux boundaries
        for (int i = 0; i < gridWidth; i++) {
            newFieldBuf[i][0] = concentrationField[i][1];
            newFieldBuf[i][gridHeight - 1] = concentrationField[i][gridHeight - 2];
        }
        for (int j = 0; j < gridHeight; j++) {
            newFieldBuf[0][j] = concentrationField[1][j];
            newFieldBuf[gridWidth - 1][j] = concentrationField[gridWidth - 2][j];
        }
    
        // Ping-pong buffer swap: new field becomes current field
        double[][] tmp = concentrationField;
        concentrationField = newFieldBuf;
        newFieldBuf = tmp;
    }
    
    // Getter and setter methods for parameters
    
    /**
     * Gets the diffusion constant.
     * @return Diffusion coefficient D
     */
    public double getDiffusionConstant() {
        return diffusionConstant;
    }
    
    /**
     * Sets the diffusion constant.
     * @param diffusionConstant New diffusion coefficient
     */
    public void setDiffusionConstant(double diffusionConstant) {
        this.diffusionConstant = diffusionConstant;
    }
    
    /**
     * Gets the nutrient consumption rate.
     * @return Consumption rate R
     */
    public double getConsumptionRate() {
        return consumptionRate;
    }
    
    /**
     * Sets the nutrient consumption rate.
     * @param consumptionRate New consumption rate
     */
    public void setConsumptionRate(double consumptionRate) {
        this.consumptionRate = consumptionRate;
    }
    
    /**
     * Gets the grid width.
     * @return Number of grid points in x-direction
     */
    public int getGridWidth() {
        return gridWidth;
    }
    
    /**
     * Gets the grid height.
     * @return Number of grid points in y-direction
     */
    public int getGridHeight() {
        return gridHeight;
    }
    
    /**
     * Gets the current concentration field.
     * @return 2D array of concentration values
     */
    public double[][] getConcentrationField() {
        return concentrationField;
    }
    
    /**
     * Gets the grid spacing in x-direction.
     * @return Grid spacing dx
     */
    public double getDx() {
        return dx;
    }
    
    /**
     * Gets the grid spacing in y-direction.
     * @return Grid spacing dy
     */
    public double getDy() {
        return dy;
    }

}
