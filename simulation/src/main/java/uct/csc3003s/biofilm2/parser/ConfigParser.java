package uct.csc3003s.biofilm2.parser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Configuration parser for biofilm simulation parameters.
 * 
 * This class handles loading and parsing of simulation configuration from INI files.
 * It provides access to all simulation parameters including bacterial properties,
 * force parameters, nutrient settings, and simulation domain configuration.

 */
public class ConfigParser {

    public static int width, height, initialCount, gridWidth, gridHeight, numTicks;
    public static double length, diameter, epsDiameter, growthRate, divisionLength, divisionRate, 
            epsProductionRate, motilityForce, repulsionForce, emEpsEps, emEpsCell, emCellCell, frictionCoefficientCell, frictionCoefficientEps,
            nutrientConcentration, nutrientConsumptionRate, diffusionRate, 
            cellDensityThreshold, epsDensityThreshold, localSensingRadius, gridCellSize, deltaTime;

    /**
     * Main entry point for standalone configuration parsing.
     * 
     * @param args Command line arguments. Supports --config <path> or --config=<path>
     */
    public static void main(String[] args) {
        // Parse command line arguments
        String filePath = getConfigPath(args);

        if (filePath == null) {
            System.exit(1);
        }

        loadConfig(filePath);
    }

    /**
     * Parses command line arguments to determine the configuration file path.
     * 
     * @param args Command line arguments
     * @return Path to configuration file, or null if invalid arguments or file not found
     */
    private static String getConfigPath(String[] args) {
        String defaultPath = "src/main/resources/config.ini";

        // If no arguments provided, try default path
        if (args.length == 0) {
            if (new File(defaultPath).exists()) {
                return defaultPath;
            } else {
                System.err.println("No config file found at default location: " + defaultPath);
                return null;
            }
        }

        // Parse --config argument in various formats
        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i]) && i + 1 < args.length) {
                return args[i + 1];
            } else if (args[i].startsWith("--config=")) {
                return args[i].substring("--config=".length());
            }
        }

        System.err.println("Invalid arguments. Use --config <path> to specify config file.");
        return null;
    }

    /**
     * Loads and parses configuration from the specified INI file.
     * 
     * @param filePath Path to the configuration file
     */
    private static void loadConfig(String filePath) {
        Map<String, String> configMap = new HashMap<>();

        try {
            File configFile = new File(filePath);
            if (!configFile.exists()) {
                System.err.println("Config file not found: " + filePath);
                System.exit(1);
            }

            // Parse INI file format
            try (Scanner sc = new Scanner(configFile)) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine().trim();
                    
                    // Skip empty lines, comments, and section headers
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) {
                        continue;
                    }
                    
                    // Parse key=value pairs
                    if (line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            String key = parts[0].trim();
                            String value = parts[1].trim();
                            
                            // Remove inline comments
                            if (value.contains("#")) {
                                value = value.substring(0, value.indexOf("#")).trim();
                            }
                            
                            configMap.put(key, value);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
            System.exit(1);
        }

        // Parse the configuration values into static fields
        parseConfigurationValues(configMap);
    }

    /**
     * Parses configuration values from the key-value map into static fields.
     * Uses default values for missing parameters and provides warnings.
     * 
     * @param config Map of configuration key-value pairs
     */
    private static void parseConfigurationValues(Map<String, String> config) {
        try {
            // Domain settings
            width = getIntValue(config, "width", 800);
            height = getIntValue(config, "height", 800);

            initialCount = getIntValue(config, "initial_count", 1);
            length = getDoubleValue(config, "length", 5.0);                          // Maximum length (lmax)
            diameter = getDoubleValue(config, "diameter", 1.0);                      // Diameter of cell (dc)
            epsDiameter = getDoubleValue(config, "eps_diameter", 0.5);               // Diameter of EPS particle (deps)
            growthRate = getDoubleValue(config, "growth_rate", 3.5);                 // Linear growth rate (φ)
            divisionLength = getDoubleValue(config, "division_length", 5.0);         // Cell division at maximum length
            divisionRate = getDoubleValue(config, "division_rate", 1.0);             // Cell division rate (kdiv)
            epsProductionRate = getDoubleValue(config, "eps_production_rate", 1.0);  // EPS production rate (keps)

            // Force parameters
            motilityForce = getDoubleValue(config, "motility_force", 300.0);
            repulsionForce = getDoubleValue(config, "repulsion_force", 100.0);

            // Elastic modulus parameters 
            emEpsEps = getDoubleValue(config, "em_eps_eps", 200.0);
            emEpsCell = getDoubleValue(config, "em_eps_cell", 200.0);
            emCellCell = getDoubleValue(config, "em_cell_cell", 200.0);

            // Friction coefficient parameters
            frictionCoefficientCell = getDoubleValue(config, "friction_coefficient_cell", 200.0);
            frictionCoefficientEps = getDoubleValue(config, "friction_coefficient_eps", 200.0);

            // Nutrient parameters
            nutrientConcentration = getDoubleValue(config, "nutrient_concentration", 3.0);      // C0
            nutrientConsumptionRate = getDoubleValue(config, "nutrient_consumption_rate", 1.0); // consumption rate
            diffusionRate = getDoubleValue(config, "diffusion_rate", 300.0);                   // D

            // Density thresholds
            cellDensityThreshold = getDoubleValue(config, "cell_density_threshold", 5.0);      // Cell [x, y]
            epsDensityThreshold = getDoubleValue(config, "eps_density_threshold", 0.3);        // EPS[x, y]
            localSensingRadius = getDoubleValue(config, "local_sensing_radius", 2.0);          // Bacterial sensing range

            // Nutrient grid parameters
            gridWidth = getIntValue(config, "grid_width", 50);
            gridHeight = getIntValue(config, "grid_height", 50);
            gridCellSize = getDoubleValue(config, "grid_cell_size", 10.0);
            
            // Simulation parameters
            deltaTime = getDoubleValue(config, "delta_time", 0.1);
            numTicks = getIntValue(config, "num_ticks", 1000);

            // Print loaded configuration for verification
            printConfiguration();

        } catch (NumberFormatException e) {
            System.err.println("Error parsing config file: Invalid number format - " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Retrieves an integer value from the configuration map with fallback to default.
     * 
     * @param config Configuration map
     * @param key Configuration key
     * @param defaultValue Default value if key is missing
     * @return Parsed integer value or default
     */
    private static int getIntValue(Map<String, String> config, String key, int defaultValue) {
        String value = config.get(key);
        if (value == null) {
            System.out.println("Warning: Using default value for " + key + ": " + defaultValue);
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    /**
     * Retrieves a double value from the configuration map with fallback to default.
     * 
     * @param config Configuration map
     * @param key Configuration key
     * @param defaultValue Default value if key is missing
     * @return Parsed double value or default
     */
    private static double getDoubleValue(Map<String, String> config, String key, double defaultValue) {
        String value = config.get(key);
        if (value == null) {
            System.out.println("Warning: Using default value for " + key + ": " + defaultValue);
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    /**
     * Prints the loaded configuration parameters to the console for verification.
     * Displays all simulation parameters with their units and research paper references.
     */
    private static void printConfiguration() {
        System.out.println("Domain: " + width + "x" + height + " μm");
        System.out.println("Bacteria: " + initialCount + " initial cells");
        System.out.println("  - Max length (lmax): " + length + " μm");
        System.out.println("  - Diameter (dc): " + diameter + " μm");
        System.out.println("  - Growth rate (φ): " + growthRate + " μm/h");
        System.out.println("  - Division length: " + divisionLength + " μm");
        System.out.println("  - Division rate (kdiv): " + divisionRate + " /h");
        System.out.println("EPS Parameters:");
        System.out.println("  - Diameter (deps): " + epsDiameter + " μm");
        System.out.println("  - Production rate (keps): " + epsProductionRate + " /h");
        System.out.println("Forces: motility=" + motilityForce + " Pa·μm²");
        System.out.println("Friction: Cell=" + frictionCoefficientCell + ", EPS=" + frictionCoefficientEps + " Pa·h");
        System.out.println("Nutrients: C0=" + nutrientConcentration + " fg·μm⁻³, consumption=" + 
                          nutrientConsumptionRate + " /h, diffusion=" + diffusionRate + " μm²/h");
        System.out.println("Density Thresholds: Cell=" + cellDensityThreshold + " μm², EPS=" + epsDensityThreshold + " μm²");
        System.out.println("Local Sensing Radius: " + localSensingRadius + " μm");
        System.out.println("Grid: " + gridWidth + "x" + gridHeight + " cells, " + gridCellSize + " μm/cell");
    }
}