package com.xXseesXx.patternwand.patterns.scripted.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import com.xXseesXx.patternwand.patterns.scripted.PlacementContext;

/**
 * Lua-friendly wrapper for placement context.
 * Exposes context information as a Lua table.
 */
public class LuaContextWrapper {

    /**
     * Create a Lua table wrapping placement context.
     *
     * @param context Placement context
     * @return Lua table with context data
     */
    public static LuaTable wrap(final PlacementContext context) {
        LuaTable table = new LuaTable();

        // Click position
        table.set("clickedX", LuaValue.valueOf(context.getClickedX()));
        table.set("clickedY", LuaValue.valueOf(context.getClickedY()));
        table.set("clickedZ", LuaValue.valueOf(context.getClickedZ()));
        table.set("clickFace", LuaValue.valueOf(context.getClickFace()));

        // Bounding box
        table.set("minX", LuaValue.valueOf(context.getMinX()));
        table.set("minY", LuaValue.valueOf(context.getMinY()));
        table.set("minZ", LuaValue.valueOf(context.getMinZ()));
        table.set("maxX", LuaValue.valueOf(context.getMaxX()));
        table.set("maxY", LuaValue.valueOf(context.getMaxY()));
        table.set("maxZ", LuaValue.valueOf(context.getMaxZ()));

        // Player orientation
        table.set("playerYaw", LuaValue.valueOf(context.getPlayerYaw()));
        table.set("playerPitch", LuaValue.valueOf(context.getPlayerPitch()));

        // World time
        table.set("worldTime", LuaValue.valueOf(context.getWorldTime()));
        table.set("dayTime", LuaValue.valueOf(context.getDayTime()));

        return table;
    }
}
