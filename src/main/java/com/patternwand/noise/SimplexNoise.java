package com.patternwand.noise;

import java.util.Random;

/**
 * Simplex noise implementation for 2D and 3D noise generation.
 * Based on Ken Perlin's simplex noise algorithm.
 * Simplex noise is faster and has fewer directional artifacts than Perlin noise.
 */
public class SimplexNoise {

    private final int[] permutation;

    // Simplex skew constants
    private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
    private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
    private static final double F3 = 1.0 / 3.0;
    private static final double G3 = 1.0 / 6.0;

    // Gradient vectors for 3D
    private static final int[][] GRAD3 = { { 1, 1, 0 }, { -1, 1, 0 }, { 1, -1, 0 }, { -1, -1, 0 }, { 1, 0, 1 },
        { -1, 0, 1 }, { 1, 0, -1 }, { -1, 0, -1 }, { 0, 1, 1 }, { 0, -1, 1 }, { 0, 1, -1 }, { 0, -1, -1 } };

    /**
     * Default permutation table
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
     * Create a new Simplex noise generator with the given seed.
     *
     * @param seed Random seed
     */
    public SimplexNoise(long seed) {
        // Initialize permutation table with seed
        permutation = new int[512];
        int[] p = new int[256];

        if (seed == 0) {
            // Use default permutation
            System.arraycopy(DEFAULT_PERMUTATION, 0, p, 0, 256);
        } else {
            // Generate seeded permutation
            Random random = new Random(seed);
            for (int i = 0; i < 256; i++) {
                p[i] = i;
            }
            // Fisher-Yates shuffle
            for (int i = 255; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temp = p[i];
                p[i] = p[j];
                p[j] = temp;
            }
        }

        // Duplicate permutation table
        for (int i = 0; i < 256; i++) {
            permutation[i] = permutation[i + 256] = p[i];
        }
    }

    /**
     * Generate 2D Simplex noise.
     *
     * @param x X coordinate
     * @param z Z coordinate
     * @return Noise value in range [-1, 1]
     */
    public double noise(double x, double z) {
        double n0, n1, n2;

        // Skew the input space to determine which simplex cell we're in
        double s = (x + z) * F2;
        int i = fastFloor(x + s);
        int j = fastFloor(z + s);

        double t = (i + j) * G2;
        double X0 = i - t;
        double Y0 = j - t;
        double x0 = x - X0;
        double y0 = z - Y0;

        // Determine which simplex we are in
        int i1, j1;
        if (x0 > y0) {
            i1 = 1;
            j1 = 0;
        } else {
            i1 = 0;
            j1 = 1;
        }

        // Offsets for middle corner in (x,y) unskewed coords
        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        // Offsets for last corner in (x,y) unskewed coords
        double x2 = x0 - 1.0 + 2.0 * G2;
        double y2 = y0 - 1.0 + 2.0 * G2;

        // Work out the hashed gradient indices
        int ii = i & 255;
        int jj = j & 255;
        int gi0 = permutation[ii + permutation[jj]] % 12;
        int gi1 = permutation[ii + i1 + permutation[jj + j1]] % 12;
        int gi2 = permutation[ii + 1 + permutation[jj + 1]] % 12;

        // Calculate contribution from three corners
        double t0 = 0.5 - x0 * x0 - y0 * y0;
        if (t0 < 0) n0 = 0.0;
        else {
            t0 *= t0;
            n0 = t0 * t0 * dot(GRAD3[gi0], x0, y0);
        }

        double t1 = 0.5 - x1 * x1 - y1 * y1;
        if (t1 < 0) n1 = 0.0;
        else {
            t1 *= t1;
            n1 = t1 * t1 * dot(GRAD3[gi1], x1, y1);
        }

        double t2 = 0.5 - x2 * x2 - y2 * y2;
        if (t2 < 0) n2 = 0.0;
        else {
            t2 *= t2;
            n2 = t2 * t2 * dot(GRAD3[gi2], x2, y2);
        }

        // Add contributions and scale to [-1, 1]
        return 70.0 * (n0 + n1 + n2);
    }

    /**
     * Generate 3D Simplex noise.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Noise value in range [-1, 1]
     */
    public double noise(double x, double y, double z) {
        double n0, n1, n2, n3;

        // Skew the input space
        double s = (x + y + z) * F3;
        int i = fastFloor(x + s);
        int j = fastFloor(y + s);
        int k = fastFloor(z + s);

        double t = (i + j + k) * G3;
        double X0 = i - t;
        double Y0 = j - t;
        double Z0 = k - t;
        double x0 = x - X0;
        double y0 = y - Y0;
        double z0 = z - Z0;

        // Determine which simplex we are in
        int i1, j1, k1;
        int i2, j2, k2;

        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            } else if (x0 >= z0) {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            } else {
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            }
        } else {
            if (y0 < z0) {
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 0;
                j2 = 1;
                k2 = 1;
            } else if (x0 < z0) {
                i1 = 0;
                j1 = 1;
                k1 = 0;
                i2 = 0;
                j2 = 1;
                k2 = 1;
            } else {
                i1 = 0;
                j1 = 1;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            }
        }

        // Offsets for remaining corners
        double x1 = x0 - i1 + G3;
        double y1 = y0 - j1 + G3;
        double z1 = z0 - k1 + G3;
        double x2 = x0 - i2 + 2.0 * G3;
        double y2 = y0 - j2 + 2.0 * G3;
        double z2 = z0 - k2 + 2.0 * G3;
        double x3 = x0 - 1.0 + 3.0 * G3;
        double y3 = y0 - 1.0 + 3.0 * G3;
        double z3 = z0 - 1.0 + 3.0 * G3;

        // Work out the hashed gradient indices
        int ii = i & 255;
        int jj = j & 255;
        int kk = k & 255;
        int gi0 = permutation[ii + permutation[jj + permutation[kk]]] % 12;
        int gi1 = permutation[ii + i1 + permutation[jj + j1 + permutation[kk + k1]]] % 12;
        int gi2 = permutation[ii + i2 + permutation[jj + j2 + permutation[kk + k2]]] % 12;
        int gi3 = permutation[ii + 1 + permutation[jj + 1 + permutation[kk + 1]]] % 12;

        // Calculate contributions from the four corners
        double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0;
        if (t0 < 0) n0 = 0.0;
        else {
            t0 *= t0;
            n0 = t0 * t0 * dot(GRAD3[gi0], x0, y0, z0);
        }

        double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1;
        if (t1 < 0) n1 = 0.0;
        else {
            t1 *= t1;
            n1 = t1 * t1 * dot(GRAD3[gi1], x1, y1, z1);
        }

        double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2;
        if (t2 < 0) n2 = 0.0;
        else {
            t2 *= t2;
            n2 = t2 * t2 * dot(GRAD3[gi2], x2, y2, z2);
        }

        double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3;
        if (t3 < 0) n3 = 0.0;
        else {
            t3 *= t3;
            n3 = t3 * t3 * dot(GRAD3[gi3], x3, y3, z3);
        }

        // Add contributions and scale to [-1, 1]
        return 32.0 * (n0 + n1 + n2 + n3);
    }

    /**
     * Fast floor function
     */
    private int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    /**
     * 2D dot product
     */
    private double dot(int[] g, double x, double y) {
        return g[0] * x + g[1] * y;
    }

    /**
     * 3D dot product
     */
    private double dot(int[] g, double x, double y, double z) {
        return g[0] * x + g[1] * y + g[2] * z;
    }
}
