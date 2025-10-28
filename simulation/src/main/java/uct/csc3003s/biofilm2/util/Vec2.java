package uct.csc3003s.biofilm2.util;

/**
 * A 2D vector class for representing positions, velocities, and orientations.
 * 
 * This class provides basic 2D vector operations
 * 
 */
public class Vec2 {
    private double x;  // X-coordinate component
    private double y;  // Y-coordinate component

    /**
     * Constructs a new 2D vector with the specified components.
     * 
     * @param x X-coordinate component
     * @param y Y-coordinate component
     */
    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Gets the X-coordinate component.
     * 
     * @return X-coordinate value
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the Y-coordinate component.
     * 
     * @return Y-coordinate value
     */
    public double getY() {
        return y;
    }

    /**
     * Sets the X-coordinate component.
     * 
     * @param x New X-coordinate value
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Sets the Y-coordinate component.
     * 
     * @param y New Y-coordinate value
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Adds another vector to this vector (in-place operation).
     * 
     * @param other Vector to add to this vector
     */
    public void add(Vec2 other) {
        this.x += other.x;
        this.y += other.y;
    }

    /**
     * Subtracts another vector from this vector (in-place operation).
     * 
     * @param other Vector to subtract from this vector
     */
    public void subtract(Vec2 other) {
        this.x -= other.x;
        this.y -= other.y;
    }

    /**
     * Calculates the dot product with another vector.
     * 
     * @param other Vector to compute dot product with
     * @return Dot product value
     */
    public double dot(Vec2 other) {
        return this.x * other.x + this.y * other.y;
    }

    /**
     * Rotates this vector by the specified angle and returns a new vector.
     * Uses standard 2D rotation matrix: [cos(θ) -sin(θ); sin(θ) cos(θ)]
     * 
     * @param angle Rotation angle in radians
     * @return New rotated vector
     */
    public Vec2 rotateVector(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
    
        double newX = this.getX() * cos - this.getY() * sin;
        double newY = this.getX() * sin + this.getY() * cos;
    
        return new Vec2(newX, newY);
    }

    /**
     * Returns a string representation of this vector.
     * 
     * @return Formatted string showing x and y components
     */
    @Override
    public String toString() {
        return String.format("Vec2 with x: %.2f and y: %.2f", x, y);
    }

    /**
     * Prints this vector to the console.
     */
    public void print() {
        System.out.println(toString());
    }


}
