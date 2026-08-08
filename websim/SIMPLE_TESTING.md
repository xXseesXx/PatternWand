# Simple Testing Guide

## Quick Diagnostic Check

1. Open `websim/index.html`
2. Press `F12` to open browser console
3. Look for "=== Running Diagnostics ===" in console
4. Check that all tests show ✓ OK:
   - ✓ Lua execution: ✓ OK
   - ✓ Math library: ✓ OK
   - ✓ Palette API: ✓ OK
   - ✓ Noise API: ✓ OK
   - ✓ Util API: ✓ OK

If any show ❌ FAILED, that feature is broken.

## Test Each Feature (Step by Step)

### Test 1: Basic Pattern (MUST WORK)
1. Load "1. Basic (solid color)"
2. Should show all same color
3. If this doesn't work, nothing will

### Test 2: World Coordinates
1. Load "2. World Coords"
2. Should show vertical stripes (repeating 0,1,2)
3. Tests that x coordinate is passed correctly

### Test 3: Relative Coordinates
1. Load "3. Relative Coords"
2. Should show vertical stripes starting from left edge
3. Tests relX works

### Test 4: Lua Math
1. Load "4. Lua Math"
2. Should show diagonal pattern
3. Tests math.floor() works
4. **If this fails, Lua math library is not loaded**

### Test 5: Lua Modulo
1. Load "5. Lua Modulo"
2. Should show 2x2 checkerboard (4 colors)
3. Tests basic Lua % operator

### Test 6-9: Palette API
Load each palette test:
- Test 6: Should show number at origin
- Test 7: Should show count at origin
- Test 8: Should show random colors (changes when you adjust weights)
- Test 9: Should show random colors (ignores weights)

### Test 10-13: Util API
Load each util test:
- Test 10: Horizontal stripes (util.floor)
- Test 11: Checkerboard (util.mod)
- Test 12: Gradient (util.clamp)
- Test 13: Circle from center (util.distance)

### Test 14-15: Noise API (IMPORTANT)
1. Load "14. noise.perlin()"
2. Should show organic cloudy pattern in 3 colors
3. Check console for errors
4. **If blank or errors, noise is broken**

5. Load "15. noise.simplex()"
6. Should show similar organic pattern
7. **If blank, SimplexNoise CDN failed (acceptable, uses Perlin fallback)**

### Test 16-18: Parameters
- Test 16: Adjust "stripeWidth" slider → stripes should change
- Test 17: Adjust "scale" slider → pattern should change
- Test 18: Toggle "inverted" checkbox → pattern should invert

### Test 19-20: Advanced
- Test 19: Check console for debug output
- Test 20: Check console for context info

## What to Look For

### ✅ Working:
- Pattern renders immediately
- No errors in console
- Looks like the description says

### ❌ Broken:
- Blank canvas
- Red errors in console
- Pattern looks wrong

## Common Issues

### "Pattern doesn't render at all"
- Check console (F12) for red errors
- Most likely: Lua syntax error or missing function

### "Noise tests show blank"
1. Check console for "Noise API injected"
2. Check for "PerlinNoise initialized"
3. If missing, perlin.js didn't load

### "Math tests fail"
- Lua standard library not loaded
- Check console for "Lua state initialized with standard libraries"

### "Nothing works"
- Check console for "Diagnostics Complete"
- If missing, app didn't initialize
- Check for errors in red

## Expected Console Output

```
=== PatternWand Simulator Starting ===
Dependency checks: {fengari: true, SimplexNoise: true, ...}
Initializing simulator components...
Lua state initialized with standard libraries
=== Running Diagnostics ===
✓ Lua execution: ✓ OK
✓ Math library: ✓ OK
✓ Palette API: ✓ OK
✓ Noise API: ✓ OK
✓ Util API: ✓ OK
=== Diagnostics Complete ===
Noise API injected with methods: perlin,perlin3d,simplex,simplex3d
Simulator ready!
Rendered 1024 blocks in 45.23ms
```

If you see this, everything is working!

## Reporting Issues

If a test fails:
1. Note which test number failed
2. Copy console output (everything)
3. Take screenshot of canvas
4. Report with: "Test X failed, console shows: ..."
