package com.patternwand.patterns.scripted.api;

import com.patternwand.noise.PerlinNoise;
import com.patternwand.noise.SimplexNoise;

/**
 * API wrapper exposing noise functions to Lua scripts.
 * All methods are designed to be called from Lua.
 */
public class NoiseAPI {

    private final PerlinNoise perlin;
    private final SimplexNoise simplex;

    /**
     * Create a new Noise API with the given seed.
     *
     * @param seed Random seed for noise generation
     */
    public NoiseAPI(long seed) {
        this.perlin = new PerlinNoise(seed);
        this.simplex = new SimplexNoise(seed);
    }

    /**
     * Generate 2D Perlin noise.
     * Smooth, natural-looking patterns.
     *
     * @param x X coordinate
     * @param z Z coordinate
     * @return Noise value in range [-1, 1]
     */
    public double perlin(double x, double z) {
        return perlin.noise(x, z);
    }

    /**
     * Generate 3D Perlin noise.
     * Smooth, natural-looking 3D patterns.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Noise value in range [-1, 1]
     */
    public double perlin3d(double x, double y, double z) {
        return perlin.noise(x, y, z);
    }

    /**
     * Generate 2D Simplex noise.
     * Similar to Perlin but faster with fewer directional artifacts.
     *
     * @param x X coordinate
     * @param z Z coordinate
     * @return Noise value in range [-1, 1]
     */
    public double simplex(double x, double z) {
        return simplex.noise(x, z);
    }

    /**
     * Generate 3D Simplex noise.
     * Faster than 3D Perlin with fewer artifacts.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Noise value in range [-1, 1]
     */
    public double simplex3d(double x, double y, double z) {
        return simplex.noise(x, y, z);
    }
}
