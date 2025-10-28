package uct.csc3003s.biofilm2.parser;

import java.io.FileWriter;
import java.io.IOException;

import com.opencsv.CSVWriter;

import uct.csc3003s.biofilm2.util.Vec2;

/**
 * Handles writing simulation output data to CSV files with automatic file splitting.
 * 
 * This class manages the output of particle data to CSV files, automatically creating
 * new files when the current file reaches the maximum line limit (100,000 lines).
 * File splitting occurs only at tick boundaries to ensure data integrity.
 */
public class OutputWriter {

    private CSVWriter writer;                                    // CSV writer for current file
    private int lineCount = 0;                                   // Current line count in active file
    private int currentPartNumber = 1;                           // Current part file number
    private static final int MAX_LINES_PER_FILE = 100000;        // Maximum lines before file split
    private String baseFileName = "../visualisation/input/simulation_output";  // Base filename for output files

    /**
     * Constructs a new OutputWriter and creates the initial output file.
     */
    public OutputWriter() {
        createNewWriter();
    }
    
    /**
     * Creates a new CSV writer for the current part file.
     * Generates filename with zero-padded part number and writes header.
     */
    private void createNewWriter() {
        try {
            String fileName = String.format("%s_part_%03d.csv", baseFileName, currentPartNumber);
            writer = new CSVWriter(new FileWriter(fileName, false));
            lineCount = 0;
            writeHeader();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output file", e);
        }
    }
    
    /**
     * Checks if current file has reached the line limit and creates a new file if needed.
     * This method is called only at tick boundaries to ensure data integrity.
     */
    private void checkAndCreateNewFile() {
        if (lineCount >= MAX_LINES_PER_FILE) {
            try {
                writer.close();
                currentPartNumber++;
                createNewWriter();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close writer and create new file", e);
            }
        }
    }

    /**
     * Writes the CSV header row with column names.
     * Header includes particle ID, tick number, type, position, size, and orientation data.
     */
    public void writeHeader() {
        writer.writeNext(new String[] { "agent_id", "tick_num", "agent_type", "pos_X", "pos_Y", "diameter", "length",
                "orientation_X", "orientation_Y" });
        lineCount++;
    }

    /**
     * Writes particle data to the CSV file.
     * 
     * @param id Particle ID
     * @param tickNum Current tick number
     * @param type Particle type (Bacterium or EPS)
     * @param pos Particle position
     * @param diameter Particle diameter
     * @param length Particle length
     * @param orientation Particle orientation vector
     */
    public void writeToCsv(int id, String tickNum, String type, Vec2 pos, double diameter, double length, Vec2 orientation) {
        writer.writeNext(new String[] {
            Integer.toString(id),
            tickNum,
            type,
            Double.toString(pos.getX()),
            Double.toString(pos.getY()),
            Double.toString(diameter),
            Double.toString(length),
            Double.toString(orientation.getX()),
            Double.toString(orientation.getY())
        });
        lineCount++;
    }

    /**
     * Writes a separator line to mark the end of a tick.
     * Checks for file size limits and creates a new file if needed.
     * This ensures file splitting occurs only at tick boundaries.
     */
    public void writeSeparator() {
        writer.writeNext(new String[] { "########################################" });
        lineCount++;
        
        // Check for file limits only after completing a tick
        checkAndCreateNewFile();
    }

    /**
     * Closes the current CSV writer and prints a summary of files created.
     * Should be called at the end of simulation to ensure all data is written.
     */
    public void closeWriter() {
        try {
            writer.close();
            System.out.println("Output writing complete. Created " + currentPartNumber + " file(s) with " + lineCount + " lines in the final file.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to close writer", e);
        }
    }
}
