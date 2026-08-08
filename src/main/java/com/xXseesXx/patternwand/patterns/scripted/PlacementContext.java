package com.xXseesXx.patternwand.patterns.scripted;

/**
 * Contextual information about pattern placement.
 * Exposes data about where and how the pattern is being placed.
 */
public class PlacementContext {

    // Click position
    private final int clickedX;
    private final int clickedY;
    private final int clickedZ;
    private final int clickFace;

    // Bounding box
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    // Player orientation
    private final float playerYaw;
    private final float playerPitch;

    // World time
    private final long worldTime;
    private final long dayTime;

    /**
     * Create a new placement context.
     */
    public PlacementContext(int clickedX, int clickedY, int clickedZ, int clickFace, int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ, float playerYaw, float playerPitch, long worldTime, long dayTime) {
        this.clickedX = clickedX;
        this.clickedY = clickedY;
        this.clickedZ = clickedZ;
        this.clickFace = clickFace;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.playerYaw = playerYaw;
        this.playerPitch = playerPitch;
        this.worldTime = worldTime;
        this.dayTime = dayTime;
    }

    // Getters
    public int getClickedX() {
        return clickedX;
    }

    public int getClickedY() {
        return clickedY;
    }

    public int getClickedZ() {
        return clickedZ;
    }

    public int getClickFace() {
        return clickFace;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public float getPlayerYaw() {
        return playerYaw;
    }

    public float getPlayerPitch() {
        return playerPitch;
    }

    public long getWorldTime() {
        return worldTime;
    }

    public long getDayTime() {
        return dayTime;
    }
}
