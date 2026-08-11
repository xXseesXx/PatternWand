// Example showing how to modify PatternWandWorker to use async execution
// This would replace the current placeBlocksWithPattern() implementation

/**
 * Modified placeBlocksWithPattern() using async plan generation.
 * 
 * BEFORE: All execution on main thread (blocks for seconds on large patterns)
 * AFTER: Lua execution async, only placement on main thread
 */
private void placeBlocksWithPatternAsync(ItemStack itemStack, LinkedList<Point3d> blocks,
    Point3d clickedPos, ItemStack sourceItems, IPlayerShim playerShim, int side, 
    float hitX, float hitY, float hitZ, String patternName) {
    
    // Create context for async execution
    AsyncPlacementExecutor.PlanContext context = new AsyncPlacementExecutor.PlanContext(
        this, // PatternWandWorker instance
        blocks,
        patternName,
        itemStack,
        clickedPos,
        playerShim,
        side
    );
    
    // Submit async plan generation
    AsyncPlacementExecutor.executeAsync(context, new AsyncPlacementExecutor.PlacementCallback() {
        @Override
        public void onPlanComplete(AsyncPlacementExecutor.PlanContext ctx, 
                                  AsyncPlacementExecutor.PlanResult result,
                                  EntityPlayer player) {
            // === THIS RUNS ON MAIN THREAD ===
            
            if (result.hasError()) {
                // Report error to player
                player.addChatMessage(new ChatComponentText("§cPattern error: " + result.errorMessage));
                return;
            }
            
            if (result.hasMissingMaterials()) {
                // Report missing materials
                reportMissingMaterials(result.missingMaterials, playerShim);
                return;
            }
            
            if (result.isSuccess()) {
                // Execute placement on main thread (Phase 5)
                ArrayList<Point3d> placedBlocks = executePlacementPlan(
                    result.plan,
                    itemStack,
                    clickedPos,
                    playerShim,
                    side,
                    hitX,
                    hitY,
                    hitZ
                );
                
                // Report success
                if (!placedBlocks.isEmpty()) {
                    if (placedBlocks.size() < result.plan.size()) {
                        player.addChatMessage(
                            new ChatComponentText(
                                String.format("§ePlaced %d of %d blocks", 
                                    placedBlocks.size(), result.plan.size())
                            )
                        );
                    } else {
                        player.addChatMessage(
                            new ChatComponentText(
                                String.format("§aPlaced %d blocks", placedBlocks.size())
                            )
                        );
                    }
                }
            }
        }
    });
    
    // Note: Method returns immediately, placement happens async
    // This prevents freezing the main thread during Lua execution
}

/**
 * Execute a complete placement plan on main thread.
 * Extracted from Phase 5 of original placeBlocksWithPattern().
 */
private ArrayList<Point3d> executePlacementPlan(PlacementPlan plan, ItemStack wandItem,
    Point3d clickedPos, IPlayerShim playerShim, int side, 
    float hitX, float hitY, float hitZ) {
    
    ArrayList<Point3d> placedBlocks = new ArrayList<Point3d>();
    
    com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.startPhase5();
    
    for (PlacementPlan.PlacementEntry entry : plan.getPlacements()) {
        ItemStack blockStack = new ItemStack(entry.block, 1, entry.metadata);
        
        // Use parent's single-block placement logic
        LinkedList<Point3d> singlePos = new LinkedList<Point3d>();
        singlePos.add(entry.position);
        
        // This handles material consumption and actual placement
        ArrayList<Point3d> placed = super.placeBlocks(
            wandItem,
            singlePos,
            entry.position,
            blockStack,
            playerShim,
            side,
            hitX,
            hitY,
            hitZ
        );
        
        if (!placed.isEmpty()) {
            placedBlocks.add(entry.position);
        }
    }
    
    com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.endPhase5(placedBlocks.size());
    
    return placedBlocks;
}

/**
 * Integration point in ItemPatternWandUnbreakable.onItemUse()
 * 
 * MINIMAL CHANGES - just add config flag check
 */
// In onItemUse() after creating PatternWandWorker:
if (Config.enableAsyncPlanning && activePattern != null) {
    // Use async execution
    worker.placeBlocksWithPatternAsync(
        itemstack, blocks, clickedPos, sourceItems, 
        playerShim, side, hitX, hitY, hitZ, activePattern
    );
    // Note: This returns immediately, player doesn't freeze
} else {
    // Original synchronous execution
    List<Point3d> placedBlocks = worker.placeBlocks(
        itemstack, blocks, clickedPos, sourceItems, 
        playerShim, side, hitX, hitY, hitZ
    );
    // Blocks until complete
}

/**
 * Server shutdown hook to cleanup async tasks.
 */
// In PatternWandMod.preInit() or similar:
Runtime.getRuntime().addShutdownHook(new Thread() {
    @Override
    public void run() {
        AsyncPlacementExecutor.shutdown();
    }
});

// Or in FMLServerStoppingEvent handler:
@SubscribeEvent
public void onServerStopping(FMLServerStoppingEvent event) {
    AsyncPlacementExecutor.shutdown();
}

/* ========================================
 * ALTERNATIVE: Chunked Placement Example
 * ======================================== */

/**
 * Spread placement over multiple ticks (no threading, safer).
 * Good for very large patterns where placement itself causes lag.
 */
public static class ChunkedPlacementExecutor {
    private static final int BLOCKS_PER_TICK = 100;
    
    private final PlacementPlan plan;
    private final PatternWandWorker worker;
    private final EntityPlayer player;
    private int currentIndex = 0;
    
    public ChunkedPlacementExecutor(PlacementPlan plan, PatternWandWorker worker, EntityPlayer player) {
        this.plan = plan;
        this.worker = worker;
        this.player = player;
    }
    
    public void start() {
        player.addChatMessage(
            new ChatComponentText(
                String.format("§7Placing %d blocks progressively...", plan.size())
            )
        );
        scheduleNextBatch();
    }
    
    private void scheduleNextBatch() {
        MinecraftServer.getServer().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                executeBatch();
            }
        });
    }
    
    private void executeBatch() {
        if (!player.isEntityAlive()) {
            return; // Player disconnected
        }
        
        int end = Math.min(currentIndex + BLOCKS_PER_TICK, plan.size());
        int placed = 0;
        
        for (int i = currentIndex; i < end; i++) {
            PlacementPlan.PlacementEntry entry = plan.getPlacements().get(i);
            
            // Place block (similar to executePlacementPlan)
            if (placeBlock(entry)) {
                placed++;
            }
        }
        
        currentIndex = end;
        
        // Show progress
        if (currentIndex % 500 == 0 && currentIndex < plan.size()) {
            player.addChatMessage(
                new ChatComponentText(
                    String.format("§7Progress: %d/%d blocks...", currentIndex, plan.size())
                )
            );
        }
        
        if (currentIndex < plan.size()) {
            // More to place, schedule next batch
            scheduleNextBatch();
        } else {
            // Complete
            player.addChatMessage(
                new ChatComponentText(
                    String.format("§aCompleted! Placed %d blocks.", plan.size())
                )
            );
        }
    }
    
    private boolean placeBlock(PlacementPlan.PlacementEntry entry) {
        // Implementation similar to executePlacementPlan
        return true;
    }
}

// Usage with chunked placement:
PlacementPlan plan = generatePlan(...); // Synchronous or async
new ChunkedPlacementExecutor(plan, worker, player).start();
// Returns immediately, placement spreads over multiple ticks
