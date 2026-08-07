package com.xXseesXx.patternwand.patterns.scripted.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

/**
 * Lua-friendly wrapper for noise functions.
 * Exposes noise generation as a Lua table with function fields.
 */
public class LuaNoiseWrapper {

    /**
     * Create a Lua table wrapping noise functions.
     *
     * @param api The underlying noise API
     * @return Lua table with noise functions
     */
    public static LuaTable wrap(final NoiseAPI api) {
        LuaTable table = new LuaTable();

        // 2D Perlin noise: noise.perlin(x, z)
        table.set("perlin", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue x, LuaValue z) {
                return LuaValue.valueOf(api.perlin(x.checkdouble(), z.checkdouble()));
            }
        });

        // 3D Perlin noise: noise.perlin3d(x, y, z)
        table.set("perlin3d", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
                return LuaValue.valueOf(api.perlin3d(x.checkdouble(), y.checkdouble(), z.checkdouble()));
            }
        });

        // 2D Simplex noise: noise.simplex(x, z)
        table.set("simplex", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue x, LuaValue z) {
                return LuaValue.valueOf(api.simplex(x.checkdouble(), z.checkdouble()));
            }
        });

        // 3D Simplex noise: noise.simplex3d(x, y, z)
        table.set("simplex3d", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
                return LuaValue.valueOf(api.simplex3d(x.checkdouble(), y.checkdouble(), z.checkdouble()));
            }
        });

        return table;
    }
}
