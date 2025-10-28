package uct.csc3003s.biofilm2.physics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import uct.csc3003s.biofilm2.particle.Particle;
import uct.csc3003s.biofilm2.particle.Bacterium;
import uct.csc3003s.biofilm2.particle.EPS;
import uct.csc3003s.biofilm2.util.Vec2;
import uct.csc3003s.biofilm2.parser.ConfigParser;

/**
 * Consolidated unit tests for MotilityForce, RandomForce, and RepulsiveForceUtils.
 * JUnit 5 + Mockito.
 */
class PhysicsTest {

    // ---------------------------
    // MotilityForce tests
    // ---------------------------

    @Test
    @DisplayName("MotilityForce: returns zero vector when no particles are provided")
    void motilityReturnsZeroWhenNoParticles() {
        MotilityForce f = new MotilityForce(10.0);
        Vec2 out = f.calculate(); // empty varargs
        assertEquals(0.0, out.getX(), 1e-12);
        assertEquals(0.0, out.getY(), 1e-12);
    }

    @ParameterizedTest(name = "MotilityForce: mag={0}, ori=({1},{2}) → F=({3},{4})")
    @CsvSource({
            // magnitude, ox,   oy,   expectedFx, expectedFy
            "5.0,          1.0,  0.0,  5.0,        0.0",
            "5.0,          0.0,  1.0,  0.0,        5.0",
            "2.5,          0.6,  0.8,  1.5,        2.0",
            "3.0,         -1.0,  0.0, -3.0,        0.0",
            "3.0,          0.0, -1.0,  0.0,       -3.0"
    })
    void motilityScalesOrientationByMagnitude(
            double mag, double ox, double oy, double efx, double efy) {

        Particle p = mock(Particle.class);
        when(p.getOrientation()).thenReturn(new Vec2(ox, oy));

        MotilityForce f = new MotilityForce(mag);
        Vec2 out = f.calculate(p);

        assertEquals(efx, out.getX(), 1e-12);
        assertEquals(efy, out.getY(), 1e-12);
    }

    @Test
    @DisplayName("MotilityForce: uses particle orientation verbatim (no implicit normalization)")
    void motilityNoNormalization() {
        Particle p = mock(Particle.class);
        when(p.getOrientation()).thenReturn(new Vec2(2.0, 0.5));

        MotilityForce f = new MotilityForce(3.0);
        Vec2 out = f.calculate(p);

        assertEquals(6.0, out.getX(), 1e-12);
        assertEquals(1.5, out.getY(), 1e-12);
    }

    // ---------------------------
    // RandomForce tests
    // ---------------------------

    @RepeatedTest(50)
    @DisplayName("RandomForce: components lie in [-0.001, 0.001]")
    void randomBoundsCheck() {
        RandomForce rf = new RandomForce();
        Vec2 f = rf.calculate(); // independent of particle orientation

        assertTrue(f.getX() >= -0.001 && f.getX() <=  0.001);
        assertTrue(f.getY() >= -0.001 && f.getY() <=  0.001);
    }

    @Test
    @DisplayName("RandomForce: randomness is non-degenerate across samples")
    void randomNonDegenerate() {
        RandomForce rf = new RandomForce();

        boolean anyDifferent = false;
        Vec2 prev = rf.calculate();
        for (int i = 0; i < 100; i++) {
            Vec2 cur = rf.calculate();
            if (cur.getX() != prev.getX() || cur.getY() != prev.getY()) {
                anyDifferent = true;
                break;
            }
        }
        assertTrue(anyDifferent, "Expected some variability over 100 samples");
    }

    // ---------------------------
    // RepulsiveForceUtils tests
    // ---------------------------

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class RepulsiveForceUtilsTests {

        @BeforeEach
        void setParams() {
            // Fixed constants for deterministic expectations
            ConfigParser.diameter = 1.0;
            ConfigParser.emCellCell = 4.0e5;
            ConfigParser.emEpsEps  = 4.0e5;
            ConfigParser.emEpsCell = 6.0e5;
        }

        @Test
        @DisplayName("RepulsiveForceUtils: zero or negative overlap → zero force")
        void zeroOrNegOverlap() {
            Particle a = mock(Bacterium.class);
            Particle b = mock(Bacterium.class);
            assertEquals(0.0, RepulsiveForceUtils.calculateForceMagnitude(a, b, 0.0), 1e-12);
            assertEquals(0.0, RepulsiveForceUtils.calculateForceMagnitude(a, b, -1e-3), 1e-12);
        }

        @Test
        @DisplayName("RepulsiveForceUtils: Cell–Cell uses emCellCell with h^(3/2) scaling")
        void cellCellScaling() {
            Particle a = mock(Bacterium.class);
            Particle b = mock(Bacterium.class);
            double h = 0.04;
            double expected = ConfigParser.emCellCell * Math.sqrt(ConfigParser.diameter) * Math.pow(h, 1.5);
            double actual = RepulsiveForceUtils.calculateForceMagnitude(a, b, h);
            assertEquals(expected, actual, 1e-12);
        }

        @Test
        @DisplayName("RepulsiveForceUtils: EPS–EPS uses emEpsEps")
        void epsEpsScaling() {
            Particle a = mock(EPS.class);
            Particle b = mock(EPS.class);
            double h = 0.02;
            double expected = ConfigParser.emEpsEps * Math.sqrt(ConfigParser.diameter) * Math.pow(h, 1.5);
            double actual = RepulsiveForceUtils.calculateForceMagnitude(a, b, h);
            assertEquals(expected, actual, 1e-12);
        }

        @Test
        @DisplayName("RepulsiveForceUtils: EPS–Cell (mixed) uses emEpsCell and is symmetric")
        void mixedScaling() {
            Particle eps = mock(EPS.class);
            Particle cell = mock(Bacterium.class);
            double h = 0.03;
            double expected = ConfigParser.emEpsCell * Math.sqrt(ConfigParser.diameter) * Math.pow(h, 1.5);
            double actual1 = RepulsiveForceUtils.calculateForceMagnitude(eps, cell, h);
            double actual2 = RepulsiveForceUtils.calculateForceMagnitude(cell, eps, h);
            assertEquals(expected, actual1, 1e-12);
            assertEquals(expected, actual2, 1e-12);
        }
    }
}
