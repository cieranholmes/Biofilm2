package uct.csc3003s.biofilm2.spatial;

import java.util.HashMap;
import java.util.function.IntConsumer;

/**
 * Spatial hash table for efficient neighbor lookup in particle simulations.
 * 
 * This class implements a spatial hashing data structure that divides 2D space
 * into a grid of cells.
 * 
 */
public final class SpatialHash {
    private final double cell;                       // Cell size (must be >= interaction cutoff)
    private final HashMap<Long, IntList> bins = new HashMap<>(1024); // Spatial bins mapping

    /**
     * Constructs a spatial hash with the specified cell size.
     * 
     * @param cellSize Size of each spatial cell (must be >= interaction cutoff)
     * @throws IllegalArgumentException if cellSize is not positive
     */
    public SpatialHash(double cellSize) {
        if (cellSize <= 0.0) throw new IllegalArgumentException("cellSize must be > 0");
        this.cell = cellSize;
    }

    /**
     * Rebuilds the spatial hash with new particle positions.
     * Clears all existing bins and redistributes particles based on their current positions.
     * 
     * @param x Array of x-coordinates for all particles
     * @param y Array of y-coordinates for all particles
     * @param n Number of particles to process
     */
    public void rebuild(double[] x, double[] y, int n) {
        bins.clear();
        for (int i = 0; i < n; i++) {
            long k = keyFromXY(x[i], y[i]);
            IntList list = bins.get(k);
            if (list == null) {
                list = new IntList(8);  // Initial capacity for new bin
                bins.put(k, list);
            }
            list.add(i);
        }
    }

    /**
     * Enumerates all neighbor indices for a given particle using 3x3 grid search.
     * Searches the particle's own cell and all 8 adjacent cells.
     * 
     * @param i Index of the particle to find neighbors for
     * @param x Array of x-coordinates for all particles
     * @param y Array of y-coordinates for all particles
     * @param action Consumer function to call for each neighbor index
     */
    public void forEachNeighborIndex(int i, double[] x, double[] y, java.util.function.IntConsumer action) {
        int tx = tileX(x[i]);
        int ty = tileY(y[i]);
        // Search 3x3 grid around the particle's cell
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                long k = keyFromTiles(tx + dx, ty + dy);
                IntList lst = bins.get(k);
                if (lst == null) continue;
                for (int p = 0; p < lst.size(); p++) {
                    action.accept(lst.get(p));
                }
            }
        }
    }
    /**
     * Enumerates all particle indices within a specified radius of a given position.
     * Searches all cells that could contain particles within the radius.
     * 
     * @param x X-coordinate of the center position
     * @param y Y-coordinate of the center position
     * @param radius Search radius
     * @param action Consumer function to call for each particle index found
     */
    public void forEachIndexNear(double x, double y, double radius, java.util.function.IntConsumer action) {
        int tx = (int)Math.floor(x / cell);
        int ty = (int)Math.floor(y / cell);
        int rTiles = (int)Math.ceil(radius / cell);
    
        // Search all cells within the radius
        for (int dy = -rTiles; dy <= rTiles; dy++) {
            for (int dx = -rTiles; dx <= rTiles; dx++) {
                long k = (((long)(tx + dx)) << 32) | ((ty + dy) & 0xffffffffL);
                IntList lst = bins.get(k);
                if (lst == null) continue;
                for (int p = 0; p < lst.size(); p++) {
                    action.accept(lst.get(p));
                }
            }
        }
    }

    /**
     * Converts a real-world x-coordinate to a tile index.
     * 
     * @param xi Real-world x-coordinate
     * @return Tile x-index
     */
    private int tileX(double xi) { return (int)Math.floor(xi / cell); }
    
    /**
     * Converts a real-world y-coordinate to a tile index.
     * 
     * @param yi Real-world y-coordinate
     * @return Tile y-index
     */
    private int tileY(double yi) { return (int)Math.floor(yi / cell); }

    /**
     * Generates a spatial hash key from real-world coordinates.
     * 
     * @param xi Real-world x-coordinate
     * @param yi Real-world y-coordinate
     * @return Spatial hash key
     */
    private long keyFromXY(double xi, double yi) {
        return keyFromTiles(tileX(xi), tileY(yi));
    }

    /**
     * Generates a spatial hash key from tile indices.
     * Uses bit shifting to pack two 32-bit integers into a single 64-bit long.
     * 
     * @param tx Tile x-index
     * @param ty Tile y-index
     * @return Spatial hash key
     */
    private static long keyFromTiles(int tx, int ty) {
        return (((long)tx) << 32) | (ty & 0xffffffffL);
    }
}
