package uct.csc3003s.biofilm2;

import uct.csc3003s.biofilm2.parser.ConfigParser;
import uct.csc3003s.biofilm2.parser.OutputWriter;
import uct.csc3003s.biofilm2.simulation.SimulationGrid;
import uct.csc3003s.biofilm2.particle.Bacterium;
import uct.csc3003s.biofilm2.particle.EPS;
import uct.csc3003s.biofilm2.particle.Particle;

/**
 * Main simulation controller for the biofilm simulation.
 * 
 * This class orchestrates the entire biofilm simulation process.
 * 
 * The simulation follows a discrete time-stepping approach where each tick
 * represents one time step of the biofilm development process.
 */
public class Simulation {
    private SimulationGrid simGrid;    // Main simulation grid managing particles and physics
    private OutputWriter outputWriter;  // Handles CSV output generation

    /**
     * Constructs a new simulation with the specified domain dimensions.
     * 
     * @param width Simulation domain width
     * @param height Simulation domain height
     */
    public Simulation(double width, double height) {
        this.simGrid = new SimulationGrid(width, height);
    }

    /**
     * Runs the complete simulation for the specified number of time steps.
     * 
     * This method initializes the simulation, runs each time step, and handles
     * data output.
     * 
     * @param numTicks Number of time steps to simulate
     */
    public void run(long numTicks) {
        simGrid.start(); // Initialize particles and start the simulation

        this.outputWriter = new OutputWriter();
        outputWriter.writeHeader();

        // Run simulation for specified number of ticks
        for (int i = 0; i < numTicks; i++) {
            tick(i);
            outputWriter.writeSeparator(); 
        }

        outputWriter.closeWriter();
    }

    /**
     * Executes one time step of the simulation.
     * 
     * This method performs all necessary updates for a single time step:
     * - Updates the simulation grid (physics, nutrient field, particle dynamics)
     * - Outputs current particle state to CSV files
     * 
     * @param tickNum Current time step number
     */
    public void tick(int tickNum) {
        // Update simulation physics and nutrient field
        double deltaTime = ConfigParser.deltaTime; // Time step from config
        simGrid.updateSimulation(deltaTime);
        
        // Output current particle state to CSV
        writeParticlesToCSV(Integer.toString(tickNum));
    }

    /**
     * Writes all current particle data to CSV output for a specific time step.
     * 
     * This method iterates through all particles in the simulation and writes
     * their current state to the CSV output file. Different particle types
     * (Bacterium vs EPS) are handled with appropriate data formatting.
     * 
     * @param tickNum Current time step as a string
     */
    private void writeParticlesToCSV(String tickNum) {
        for (Particle particle : simGrid.getParticles()) {
            if (particle instanceof Bacterium) {
                Bacterium bacterium = (Bacterium) particle;
                outputWriter.writeToCsv(
                    bacterium.getParticleId(),
                    tickNum,
                    "cell",
                    bacterium.getPosition(),
                    bacterium.getDiameter(),
                    bacterium.getLength(),
                    bacterium.getOrientation()
                );
            } else if (particle instanceof EPS) {
                EPS eps = (EPS) particle;
                outputWriter.writeToCsv(
                    eps.getParticleId(),
                    tickNum,
                    "eps", // agent_type
                    eps.getPosition(),
                    eps.getRadius() * 2,  // Convert radius to diameter
                    0.0, // length for EPS (spherical particles have no length)
                    eps.getOrientation()
                );
            }
        }
    }

    /**
     * Main entry point for the biofilm simulation.
     * 
     * This method:
     * 1. Parses command-line arguments and configuration
     * 2. Creates a new simulation with the specified dimensions
     * 3. Runs the simulation for the configured number of time steps
     * 
     * @param args Command-line arguments (configuration file path)
     */
    public static void main(String[] args) {
        ConfigParser.main(args); // Parse configuration from command line
        Simulation simulation = new Simulation(ConfigParser.width, ConfigParser.height);
        simulation.run(ConfigParser.numTicks);
    }
}
