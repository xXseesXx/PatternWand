// Simple Perlin Noise implementation for the simulator
class PerlinNoise {
    constructor(seed = 0) {
        this.seed = seed;
        this.p = this.buildPermutationTable(seed);
        console.log('PerlinNoise constructed with seed:', seed);
    }

    buildPermutationTable(seed) {
        const p = [];
        for (let i = 0; i < 256; i++) {
            p[i] = i;
        }

        // Shuffle using seed
        let n = seed;
        for (let i = 255; i > 0; i--) {
            n = (n * 16807) % 2147483647;
            const j = Math.floor((n / 2147483647) * (i + 1));
            [p[i], p[j]] = [p[j], p[i]];
        }

        // Duplicate for wrapping
        for (let i = 0; i < 256; i++) {
            p[256 + i] = p[i];
        }

        console.log('PerlinNoise permutation table built, size:', p.length);
        return p;
    }

    fade(t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    lerp(t, a, b) {
        return a + t * (b - a);
    }

    grad(hash, x, y, z = 0) {
        const h = hash & 15;
        const u = h < 8 ? x : y;
        const v = h < 4 ? y : h === 12 || h === 14 ? x : z;
        return ((h & 1) === 0 ? u : -u) + ((h & 2) === 0 ? v : -v);
    }

    noise2D(x, z) {
        try {
            const result = this.noise3D(x, z, 0);
            if (isNaN(result)) {
                console.error('PerlinNoise.noise2D produced NaN for x:', x, 'z:', z);
                return 0;
            }
            return result;
        } catch (e) {
            console.error('PerlinNoise.noise2D error:', e, 'x:', x, 'z:', z);
            return 0;
        }
    }

    noise3D(x, y, z) {
        try {
            const X = Math.floor(x) & 255;
            const Y = Math.floor(y) & 255;
            const Z = Math.floor(z) & 255;

            x -= Math.floor(x);
            y -= Math.floor(y);
            z -= Math.floor(z);

            const u = this.fade(x);
            const v = this.fade(y);
            const w = this.fade(z);

            const A = this.p[X] + Y;
            const AA = this.p[A] + Z;
            const AB = this.p[A + 1] + Z;
            const B = this.p[X + 1] + Y;
            const BA = this.p[B] + Z;
            const BB = this.p[B + 1] + Z;

            const result = this.lerp(
                w,
                this.lerp(
                    v,
                    this.lerp(u, this.grad(this.p[AA], x, y, z), this.grad(this.p[BA], x - 1, y, z)),
                    this.lerp(u, this.grad(this.p[AB], x, y - 1, z), this.grad(this.p[BB], x - 1, y - 1, z))
                ),
                this.lerp(
                    v,
                    this.lerp(u, this.grad(this.p[AA + 1], x, y, z - 1), this.grad(this.p[BA + 1], x - 1, y, z - 1)),
                    this.lerp(u, this.grad(this.p[AB + 1], x, y - 1, z - 1), this.grad(this.p[BB + 1], x - 1, y - 1, z - 1))
                )
            );

            // Check for NaN
            if (isNaN(result)) {
                console.error('PerlinNoise.noise3D produced NaN for x:', x, 'y:', y, 'z:', z);
                return 0;
            }

            return result;
        } catch (e) {
            console.error('PerlinNoise.noise3D error:', e, 'x:', x, 'y:', y, 'z:', z);
            return 0;
        }
    }
}

// Test the implementation immediately
console.log('=== Testing PerlinNoise ===');
try {
    const testNoise = new PerlinNoise(12345);
    const testValue = testNoise.noise2D(0.5, 0.5);
    console.log('Test noise value:', testValue, 'type:', typeof testValue);
    console.log('Is NaN?', isNaN(testValue));
    console.log('In range [-1,1]?', testValue >= -1 && testValue <= 1);
} catch (e) {
    console.error('PerlinNoise test failed:', e);
}
