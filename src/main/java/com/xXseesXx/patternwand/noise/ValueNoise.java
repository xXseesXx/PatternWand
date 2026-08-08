package com.xXseesXx.patternwand.noise;

import java.util.Random;

/**
 * Value noise implementation for 2D and 3D noise generation.
 *
 * Value noise interpolates random scalar values assigned to a lattice of grid
 * points, using smooth (quintic) interpolation between them. It produces a
 * softer, more "pillowy" look compared to Perlin noise, which uses gradient
 * vectors instead of scalar values.
 *
 * Output range: [-1, 1]
 */
public class ValueNoise {

    /** Doubled permutation table, indexed with (coord & 255). */
    private final int[] permutation;

    /** Random scalar values in [-1, 1] for each of the 256 lattice points. */
    private final double[] values;

    /**
     * Default permutation table from original Perlin noise (reused for consistency
     * with the rest of the noise suite).
     */
    private static final int[] DEFAULT_PERMUTATION = { 151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7,
        225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94,
        252, 219, 203, 117, 35, 11, 32, 57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74,
        165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55,
        46, 245, 40, 244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196,
        135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147,
        118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248,
        152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224,
        232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51,
        145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4,
        150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180 };

    /**
     * Create a new Value noise generator with the given seed.
     *
     * @param seed Random seed (0 uses a deterministic default permutation)
     */
    public ValueNoise(long seed) {
        permutation = new int[512];
        values = new double[512];

        int[] p = new int[256];
        double[] v = new double[256];

        if (seed == 0) {
            System.arraycopy(DEFAULT_PERMUTATION, 0, p, 0, 256);
            // Derive values from default permutation so that seed=0 is still deterministic
            for (int i = 0; i < 256; i++) {
                v[i] = (p[i] / 127.5) - 1.0; // map [0,255] -> [-1, 1]
            }
        } else {
            Random random = new Random(seed);
            for (int i = 0; i < 256; i++) {
                p[i] = i;
                v[i] = random.nextDouble() * 2.0 - 1.0; // uniform in [-1, 1]
            }
            // Fisher-Yates shuffle for the permutation
            for (int i = 255; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temp = p[i];
                p[i] = p[j];
                p[j] = temp;
            }
        }

        // Duplicate tables for wrap-around indexing
        for (int i = 0; i < 256; i++) {
            permutation[i] = permutation[i + 256] = p[i];
            values[i] = values[i + 256] = v[i];
        }
    }

    // -------------------------------------------------------------------------
    // Public interface
    // -------------------------------------------------------------------------

    /**
     * Generate 2D value noise.
     *
     * @param x X coordinate
     * @param z Z coordinate
     * @return Noise value in range [-1, 1]
     */
    public double noise(double x, double z) {
        // Grid cell coordinates
        int X = (int) Math.floor(x) & 255;
        int Z = (int) Math.floor(z) & 255;

        // Fractional position within cell
        double fx = x - Math.floor(x);
        double fz = z - Math.floor(z);

        // Quintic fade for smooth interpolation (same curve as Perlin's improved noise)
        double u = fade(fx);
        double w = fade(fz);

        // Corner values
        double v00 = latticeValue2D(X, Z);
        double v10 = latticeValue2D(X + 1, Z);
        double v01 = latticeValue2D(X, Z + 1);
        double v11 = latticeValue2D(X + 1, Z + 1);

        // Bilinear interpolation
        return lerp(w, lerp(u, v00, v10), lerp(u, v01, v11));
    }

    /**
     * Generate 3D value noise.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Noise value in range [-1, 1]
     */
    public double noise(double x, double y, double z) {
        // Grid cell coordinates
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;

        // Fractional position within cell
        double fx = x - Math.floor(x);
        double fy = y - Math.floor(y);
        double fz = z - Math.floor(z);

        // Quintic fade curves
        double u = fade(fx);
        double v = fade(fy);
        double w = fade(fz);

        // Corner values (8 corners of the unit cube)
        double v000 = latticeValue3D(X, Y, Z);
        double v100 = latticeValue3D(X + 1, Y, Z);
        double v010 = latticeValue3D(X, Y + 1, Z);
        double v110 = latticeValue3D(X + 1, Y + 1, Z);
        double v001 = latticeValue3D(X, Y, Z + 1);
        double v101 = latticeValue3D(X + 1, Y, Z + 1);
        double v011 = latticeValue3D(X, Y + 1, Z + 1);
        double v111 = latticeValue3D(X + 1, Y + 1, Z + 1);

        // Trilinear interpolation
        return lerp(
            w,
            lerp(v, lerp(u, v000, v100), lerp(u, v010, v110)),
            lerp(v, lerp(u, v001, v101), lerp(u, v011, v111)));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Look up the random value at a 2D lattice point. */
    private double latticeValue2D(int x, int z) {
        return values[permutation[permutation[x & 255] + (z & 255)]];
    }

    /** Look up the random value at a 3D lattice point. */
    private double latticeValue3D(int x, int y, int z) {
        return values[permutation[permutation[permutation[x & 255] + (y & 255)] + (z & 255)]];
    }

    /** Quintic fade: 6t^5 - 15t^4 + 10t^3 */
    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /** Linear interpolation */
    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }
}
