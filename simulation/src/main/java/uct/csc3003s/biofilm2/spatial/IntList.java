package uct.csc3003s.biofilm2.spatial;

/**
 * A dynamic array implementation for storing integers.
 * 
 * This class provides an efficient, resizable array for storing integer values.
 * It automatically grows when capacity is exceeded, doubling the internal array size.
 * Used by SpatialHash for storing particle indices in spatial bins.
 */
public final class IntList {
    private int[] a;  // Internal array for storing integers
    private int n;    // Current number of elements

    /**
     * Constructs an IntList with the specified initial capacity.
     * 
     * @param cap Initial capacity (minimum 4)
     */
    public IntList(int cap) { this.a = new int[Math.max(4, cap)]; }
    
    /**
     * Constructs an IntList with default initial capacity of 16.
     */
    public IntList() { this(16); }

    /**
     * Adds an integer value to the list.
     * Automatically resizes the internal array if capacity is exceeded.
     * 
     * @param v Integer value to add
     */
    public void add(int v) {
        if (n == a.length) {
            int[] b = new int[a.length << 1];  // Double the capacity
            System.arraycopy(a, 0, b, 0, n);
            a = b;
        }
        a[n++] = v;
    }

    /**
     * Gets the current number of elements in the list.
     * 
     * @return Number of elements
     */
    public int size() { return n; }
    
    /**
     * Gets the integer value at the specified index.
     * 
     * @param i Index of the element to retrieve
     * @return Integer value at the specified index
     */
    public int get(int i) { return a[i]; }
    
    /**
     * Clears all elements from the list.
     * Does not resize the internal array.
     */
    public void clear() { n = 0; }
}
