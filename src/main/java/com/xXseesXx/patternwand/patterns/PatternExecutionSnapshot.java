package com.xXseesXx.patternwand.patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xXseesXx.patternwand.patterns.scripted.CompiledScript;
import com.xXseesXx.patternwand.patterns.scripted.PlacementContext;

import portablejim.bbw.basics.Point3d;

/**
 * Immutable snapshot of all data needed to execute a pattern asynchronously.
 * 
 * <p>
 * Thread Safety Contract:
 * <ul>
 * <li>Contains NO Minecraft objects (World, EntityPlayer, ItemStack, etc.)</li>
 * <li>All collections are immutable (wrapped with Collections.unmodifiable*)</li>
 * <li>All data is either primitive or immutable</li>
 * <li>Safe to pass to background threads for Lua execution</li>
 * </ul>
 * 
 * <p>
 * This class represents the boundary between main thread (Minecraft) and
 * background threads (Lua computation). Once created, it can be handed off
 * to worker threads without any synchronization concerns.
 * 
 * <p>
 * Design Rationale:
 * <ul>
 * <li>Block objects are NOT thread-safe and tied to Minecraft's tick cycle</li>
 * <li>We convert Block references to registry names (strings) for thread safety</li>
 * <li>The result (PlacementPlan) is also thread-safe and can be passed back</li>
 * <li>World modification happens ONLY on main thread after Lua completes</li>
 * </ul>
 * 
 * @see PlacementPlan
 * @see com.xXseesXx.patternwand.patterns.scripted.PlacementContext
 */
public class PatternExecutionSnapshot {

    /**
     * Immutable representation of a single palette slot.
     * Uses registry name instead of Block reference for thread safety.
     */
    public static class PaletteSlot {

        public final String blockRegistryName; // e.g., "minecraft:stone"
        public final int metadata;
        public final int weight; // Stack size (1-64, or 0 if empty)

        public PaletteSlot(String blockRegistryName, int metadata, int weight) {
            this.blockRegistryName = blockRegistryName;
            this.metadata = metadata;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return String.format("PaletteSlot{%s:%d, weight=%d}", blockRegistryName, metadata, weight);
        }
    }

    /**
     * Immutable representation of a block position to evaluate.
     * Contains both absolute and relative coordinates.
     */
    public static class Position {

        public final int x;
        public final int y;
        public final int z;
        public final int relX;
        public final int relY;
        public final int relZ;

        public Position(int x, int y, int z, int relX, int relY, int relZ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.relX = relX;
            this.relY = relY;
            this.relZ = relZ;
        }

        public Position(Point3d point, Point3d origin) {
            this.x = point.x;
            this.y = point.y;
            this.z = point.z;
            this.relX = point.x - origin.x;
            this.relY = point.y - origin.y;
            this.relZ = point.z - origin.z;
        }

        /**
         * Convert back to Point3d for PlacementPlan compatibility.
         */
        public Point3d toPoint3d() {
            return new Point3d(x, y, z);
        }

        @Override
        public String toString() {
            return String.format("Position{%d,%d,%d rel(%d,%d,%d)}", x, y, z, relX, relY, relZ);
        }
    }

    // Pattern execution data
    private final CompiledScript compiledScript;
    private final String patternName;

    // Positions to evaluate
    private final List<Position> positions;

    // Palette data (thread-safe representation)
    private final List<PaletteSlot> palette;

    // Pattern parameters
    private final long seed;
    private final Map<String, Object> parameters;

    // Placement context
    private final PlacementContext context;

    /**
     * Create a new immutable pattern execution snapshot.
     * 
     * <p>
     * All collections passed in will be defensively copied and made immutable.
     * 
     * @param compiledScript The compiled Lua script (CompiledScript is thread-safe)
     * @param patternName    Name of the pattern (for error reporting)
     * @param positions      List of positions to evaluate
     * @param palette        Palette slots with weights
     * @param seed           Random seed
     * @param parameters     Pattern parameters
     * @param context        Placement context (already immutable)
     */
    public PatternExecutionSnapshot(CompiledScript compiledScript, String patternName, List<Position> positions,
        List<PaletteSlot> palette, long seed, Map<String, Object> parameters, PlacementContext context) {

        if (compiledScript == null) {
            throw new IllegalArgumentException("compiledScript cannot be null");
        }
        if (patternName == null || patternName.isEmpty()) {
            throw new IllegalArgumentException("patternName cannot be null or empty");
        }
        if (positions == null || positions.isEmpty()) {
            throw new IllegalArgumentException("positions cannot be null or empty");
        }
        if (palette == null) {
            throw new IllegalArgumentException("palette cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }

        this.compiledScript = compiledScript;
        this.patternName = patternName;

        // Defensive copies + make immutable
        this.positions = Collections.unmodifiableList(new ArrayList<Position>(positions));
        this.palette = Collections.unmodifiableList(new ArrayList<PaletteSlot>(palette));
        this.parameters = parameters == null ? Collections.<String, Object>emptyMap()
            : Collections.unmodifiableMap(new HashMap<String, Object>(parameters));

        this.seed = seed;
        this.context = context; // Already immutable
    }

    // Getters (all return immutable views or primitives)

    public CompiledScript getCompiledScript() {
        return compiledScript;
    }

    public String getPatternName() {
        return patternName;
    }

    public List<Position> getPositions() {
        return positions; // Already immutable
    }

    public List<PaletteSlot> getPalette() {
        return palette; // Already immutable
    }

    public long getSeed() {
        return seed;
    }

    public Map<String, Object> getParameters() {
        return parameters; // Already immutable
    }

    public PlacementContext getContext() {
        return context;
    }

    @Override
    public String toString() {
        return String.format(
            "PatternExecutionSnapshot{pattern='%s', positions=%d, palette=%d, seed=%d}",
            patternName,
            positions.size(),
            palette.size(),
            seed);
    }
}
