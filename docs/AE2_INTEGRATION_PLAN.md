# AE2 Integration Plan for PatternWand (REVISED)

## Overview

Add Matter Manipulator-inspired AE2 network integration to PatternWand, allowing the wand to source and return blocks from a linked Applied Energistics 2 network. This transforms the wand from a player-inventory-only tool into a powerful building device backed by massive AE2 storage.

**Key Improvement:** Uses a **transactional/reservation model** to ensure atomic resource acquisition and prevent partial builds.

## Core Concept: Transactional Material System

Following Matter Manipulator's excellent architecture, we introduce:
1. Clean abstraction between pattern generation and material sourcing
2. **Aggregate material requirements before consumption**
3. **Reservation-based transaction model**
4. Clear separation of policy (provider chain) from capability (providers)

### Complete Architecture

```
                PatternWandWorker
                       │
                       ▼
                ┌──────────────┐
                │ PatternPlan  │  ← Generated from Lua
                └──────┬───────┘
                       │
                       ▼
             MaterialRequirements  ← Aggregated: stone×1000, glass×500
                       │
                       ▼
            MaterialProviderChain  ← Policy: player→AE2 order
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
     Player           AE2          (Future:
    Provider        Provider       GT Uplink)
        │              │              │
        └──────────────┼──────────────┘
                       │
                       ▼
              MaterialReservation  ← Simulation result
                       │
                ┌──────┴──────┐
                │             │
             success       failure
                │             │
                ▼             ▼
             commit       report missing
                │
                ▼
         Execute PatternPlan  ← Place blocks in world
```

## Implementation Phases

### Phase 1: Core Material System (Foundation)

Build the fundamental material system with proper abstraction and transaction support.

#### 1.1 Create `MaterialKey` - Canonical Item Representation

**File:** `src/main/java/com/xXseesXx/patternwand/materials/MaterialKey.java`

Handles the complexity of block→item mapping including NBT.

```java
/**
 * Canonical representation of a material for consumption/injection.
 * Handles Item + damage + NBT to avoid ambiguity.
 */
public class MaterialKey {
    private final Item item;
    private final int damage;
    private final NBTTagCompound tag; // Can be null
    
    private MaterialKey(Item item, int damage, NBTTagCompound tag) {
        this.item = item;
        this.damage = damage;
        this.tag = tag;
    }
    
    /**
     * Create MaterialKey from ItemStack.
     */
    public static MaterialKey fromItemStack(ItemStack stack) {
        if (stack == null) return null;
        
        NBTTagCompound tag = null;
        if (stack.hasTagCompound()) {
            tag = (NBTTagCompound) stack.getTagCompound().copy();
        }
        
        return new MaterialKey(stack.getItem(), stack.getItemDamage(), tag);
    }
    
    /**
     * Create MaterialKey from Block + metadata.
     */
    public static MaterialKey fromBlock(Block block, int meta) {
        Item item = Item.getItemFromBlock(block);
        if (item == null) return null;
        return new MaterialKey(item, meta, null);
    }
    
    /**
     * Convert to ItemStack with specified size.
     */
    public ItemStack toItemStack(int size) {
        ItemStack stack = new ItemStack(item, size, damage);
        if (tag != null) {
            stack.setTagCompound((NBTTagCompound) tag.copy());
        }
        return stack;
    }
    
    /**
     * Check if ItemStack matches this key.
     */
    public boolean matches(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() != item) return false;
        if (stack.getItemDamage() != damage) return false;
        
        // NBT comparison
        if (tag == null) {
            return !stack.hasTagCompound();
        } else {
            return stack.hasTagCompound() && tag.equals(stack.getTagCompound());
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MaterialKey)) return false;
        MaterialKey other = (MaterialKey) obj;
        
        if (item != other.item) return false;
        if (damage != other.damage) return false;
        
        if (tag == null) return other.tag == null;
        return tag.equals(other.tag);
    }
    
    @Override
    public int hashCode() {
        int hash = item.hashCode();
        hash = 31 * hash + damage;
        if (tag != null) {
            hash = 31 * hash + tag.hashCode();
        }
        return hash;
    }
    
    public Item getItem() { return item; }
    public int getDamage() { return damage; }
    public NBTTagCompound getTag() { return tag; }
}
```

#### 1.2 Create `MaterialRequirements` - Aggregated Requirements

**File:** `src/main/java/com/xXseesXx/patternwand/materials/MaterialRequirements.java`

```java
/**
 * Aggregated material requirements for an operation.
 * Maps MaterialKey → quantity needed.
 */
public class MaterialRequirements {
    private final Map<MaterialKey, Integer> requirements;
    
    public MaterialRequirements() {
        this.requirements = new HashMap<>();
    }
    
    /**
     * Add requirement for a material.
     */
    public void add(MaterialKey key, int amount) {
        requirements.put(key, requirements.getOrDefault(key, 0) + amount);
    }
    
    /**
     * Add requirement from ItemStack.
     */
    public void add(ItemStack stack) {
        if (stack == null) return;
        MaterialKey key = MaterialKey.fromItemStack(stack);
        if (key != null) {
            add(key, stack.stackSize);
        }
    }
    
    /**
     * Get all required materials.
     */
    public Map<MaterialKey, Integer> getAll() {
        return Collections.unmodifiableMap(requirements);
    }
    
    /**
     * Get amount needed for specific material.
     */
    public int getAmount(MaterialKey key) {
        return requirements.getOrDefault(key, 0);
    }
    
    /**
     * Check if any materials are required.
     */
    public boolean isEmpty() {
        return requirements.isEmpty();
    }
    
    /**
     * Get total number of distinct materials.
     */
    public int size() {
        return requirements.size();
    }
    
    /**
     * Get total item count across all materials.
     */
    public int getTotalCount() {
        return requirements.values().stream().mapToInt(Integer::intValue).sum();
    }
}
```

#### 1.3 Create `MaterialReservation` - Transaction Result

**File:** `src/main/java/com/xXseesXx/patternwand/materials/MaterialReservation.java`

```java
/**
 * Result of simulating material acquisition.
 * Can be committed to actually consume the materials.
 */
public class MaterialReservation {
    private final MaterialRequirements requirements;
    private final Map<MaterialKey, Integer> available;
    private final Map<MaterialKey, Map<IMaterialProvider, Integer>> allocation;
    private boolean committed = false;
    
    MaterialReservation(MaterialRequirements requirements) {
        this.requirements = requirements;
        this.available = new HashMap<>();
        this.allocation = new HashMap<>();
    }
    
    /**
     * Record that provider can supply amount of material.
     */
    void recordAvailable(MaterialKey key, IMaterialProvider provider, int amount) {
        available.put(key, available.getOrDefault(key, 0) + amount);
        
        allocation.computeIfAbsent(key, k -> new HashMap<>())
            .put(provider, amount);
    }
    
    /**
     * Check if all requirements can be satisfied.
     */
    public boolean isSatisfied() {
        for (Map.Entry<MaterialKey, Integer> entry : requirements.getAll().entrySet()) {
            int needed = entry.getValue();
            int avail = available.getOrDefault(entry.getKey(), 0);
            if (avail < needed) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get missing materials (needed but not available).
     */
    public Map<MaterialKey, Integer> getMissing() {
        Map<MaterialKey, Integer> missing = new HashMap<>();
        for (Map.Entry<MaterialKey, Integer> entry : requirements.getAll().entrySet()) {
            MaterialKey key = entry.getKey();
            int needed = entry.getValue();
            int avail = available.getOrDefault(key, 0);
            if (avail < needed) {
                missing.put(key, needed - avail);
            }
        }
        return missing;
    }
    
    /**
     * Commit the reservation - actually consume materials.
     * Can only be called once.
     */
    public void commit() {
        if (committed) {
            throw new IllegalStateException("Reservation already committed");
        }
        if (!isSatisfied()) {
            throw new IllegalStateException("Cannot commit unsatisfied reservation");
        }
        
        // Consume from each provider according to allocation
        for (Map.Entry<MaterialKey, Map<IMaterialProvider, Integer>> entry : allocation.entrySet()) {
            MaterialKey key = entry.getKey();
            
            for (Map.Entry<IMaterialProvider, Integer> providerEntry : entry.getValue().entrySet()) {
                IMaterialProvider provider = providerEntry.getKey();
                int amount = providerEntry.getValue();
                
                ItemStack stack = key.toItemStack(amount);
                int consumed = provider.consume(stack, amount);
                
                if (consumed != amount) {
                    // This shouldn't happen if simulation was correct
                    throw new IllegalStateException(
                        String.format("Provider %s failed to deliver %d of %s (got %d)",
                            provider.getName(), amount, key, consumed)
                    );
                }
            }
        }
        
        committed = true;
    }
    
    /**
     * Cancel the reservation (no-op, nothing was consumed yet).
     */
    public void cancel() {
        // Nothing to do - simulation doesn't consume
        committed = true; // Prevent future commit
    }
    
    public MaterialRequirements getRequirements() {
        return requirements;
    }
    
    public boolean isCommitted() {
        return committed;
    }
}
```

#### 1.4 Create `IMaterialProvider` Interface (Simplified)

**File:** `src/main/java/com/xXseesXx/patternwand/materials/IMaterialProvider.java`

**Key Change:** No `getPriority()` - the chain controls policy, not providers.

```java
/**
 * A source/sink for materials.
 * Providers are capability, not policy.
 */
public interface IMaterialProvider {
    /**
     * Simulate item availability (check without consuming).
     * @param stack The item to check
     * @param amount Number needed
     * @return Number actually available (may be less than requested)
     */
    int simulate(ItemStack stack, int amount);
    
    /**
     * Actually consume items from the provider.
     * @param stack The item to consume
     * @param amount Number to take
     * @return Number actually consumed
     */
    int consume(ItemStack stack, int amount);
    
    /**
     * Return items to the provider.
     * @param stack The item to return
     * @param amount Number to return
     * @return Number actually returned (remainder if provider is full)
     */
    int inject(ItemStack stack, int amount);
    
    /**
     * Get provider name for display/debugging.
     */
    String getName();
}
```

#### 1.5 Create `MaterialProviderChain` (Policy Container)

**File:** `src/main/java/com/xXseesXx/patternwand/materials/MaterialProviderChain.java`

**Key Change:** Chain manages provider order explicitly, not via priority.

```java
/**
 * Orchestrates material sourcing across multiple providers.
 * Policy: defines the order and behavior of provider cascade.
 */
public class MaterialProviderChain {
    private final List<IMaterialProvider> consumeOrder;
    private final List<IMaterialProvider> injectOrder;
    
    public MaterialProviderChain() {
        this.consumeOrder = new ArrayList<>();
        this.injectOrder = new ArrayList<>();
    }
    
    /**
     * Add provider for consumption (will check in this order).
     */
    public void addConsumeProvider(IMaterialProvider provider) {
        consumeOrder.add(provider);
    }
    
    /**
     * Add provider for injection (will inject in this order).
     */
    public void addInjectProvider(IMaterialProvider provider) {
        injectOrder.add(provider);
    }
    
    /**
     * Prepare a reservation for the given requirements.
     * This simulates the entire operation without consuming anything.
     */
    public MaterialReservation prepare(MaterialRequirements requirements) {
        MaterialReservation reservation = new MaterialReservation(requirements);
        
        // For each required material, simulate acquisition
        for (Map.Entry<MaterialKey, Integer> entry : requirements.getAll().entrySet()) {
            MaterialKey key = entry.getKey();
            int needed = entry.getValue();
            int remaining = needed;
            
            // Try each provider in order
            for (IMaterialProvider provider : consumeOrder) {
                if (remaining <= 0) break;
                
                ItemStack stack = key.toItemStack(remaining);
                int available = provider.simulate(stack, remaining);
                
                if (available > 0) {
                    reservation.recordAvailable(key, provider, available);
                    remaining -= available;
                }
            }
        }
        
        return reservation;
    }
    
    /**
     * Inject items back into providers.
     */
    public int inject(ItemStack stack, int amount) {
        int remaining = amount;
        
        for (IMaterialProvider provider : injectOrder) {
            if (remaining <= 0) break;
            
            int injected = provider.inject(stack, remaining);
            remaining -= injected;
        }
        
        return amount - remaining;
    }
    
    /**
     * Get consume providers (for debugging/display).
     */
    public List<IMaterialProvider> getConsumeProviders() {
        return Collections.unmodifiableList(consumeOrder);
    }
    
    /**
     * Get inject providers (for debugging/display).
     */
    public List<IMaterialProvider> getInjectProviders() {
        return Collections.unmodifiableList(injectOrder);
    }
}
```

#### 1.6 Create `PlayerInventoryProvider`

**File:** `src/main/java/com/xXseesXx/patternwand/materials/PlayerInventoryProvider.java`

Wraps the existing `IPlayerShim` logic into the provider interface.

```java
/**
 * Material provider backed by player inventory.
 */
public class PlayerInventoryProvider implements IMaterialProvider {
    private final IPlayerShim player;
    
    public PlayerInventoryProvider(IPlayerShim player) {
        this.player = player;
    }
    
    @Override
    public int simulate(ItemStack stack, int amount) {
        return Math.min(amount, player.countItems(stack, false));
    }
    
    @Override
    public int consume(ItemStack stack, int amount) {
        int available = simulate(stack, amount);
        if (available > 0) {
            player.useItems(stack, available, false);
        }
        return available;
    }
    
    @Override
    public int inject(ItemStack stack, int amount) {
        // Try to add to player inventory
        ItemStack copy = stack.copy();
        copy.stackSize = amount;
        
        if (player.addItem(copy)) {
            return amount;
        }
        
        // Check how much was actually added
        int remaining = copy.stackSize;
        return amount - remaining;
    }
    
    @Override
    public String getName() {
        return "Player Inventory";
    }
}

### Phase 2: AE2 Network Integration

Implement the core AE2 connection mechanism inspired by Matter Manipulator's design.

#### 2.1 Create `AE2Connection` - Connection State Manager

**File:** `src/main/java/com/xXseesXx/patternwand/materials/ae2/AE2Connection.java`

Handles connection lifecycle and validation.

```java
/**
 * Manages connection to an AE2 network via security terminal.
 * Handles connection validation and reconnection.
 */
class AE2Connection {
    private final long securityTerminalId;
    private final EntityPlayer player;
    
    // Transient - recreated each use
    private TileSecurity securityTerminal;
    private IGridNode gridNode;
    private IGrid grid;
    private IStorageGrid storageGrid;
    private IMEMonitor<IAEItemStack> itemStorage;
    
    // Cache validation
    private long lastValidationTime = 0;
    private static final long VALIDATION_INTERVAL = 20; // ticks
    
    AE2Connection(long securityTerminalId, EntityPlayer player) {
        this.securityTerminalId = securityTerminalId;
        this.player = player;
    }
    
    /**
     * Ensure connection is established and valid.
     */
    boolean ensureConnected() {
        // Check if we need to revalidate
        long currentTime = player.worldObj.getTotalWorldTime();
        boolean needsValidation = (currentTime - lastValidationTime) > VALIDATION_INTERVAL;
        
        if (itemStorage == null || needsValidation) {
            // Invalidate existing connection
            invalidate();
            
            // Try to reconnect
            if (!connect()) {
                return false;
            }
            
            lastValidationTime = currentTime;
        }
        
        // Validate connection is still usable
        return isValid();
    }
    
    /**
     * Establish connection to AE2 network.
     */
    private boolean connect() {
        // Resolve security terminal from locatable registry
        ILocatable locatable = AEApi.instance()
            .registries()
            .locatable()
            .getLocatableBy(securityTerminalId);
            
        if (!(locatable instanceof TileSecurity)) {
            return false;
        }
        
        this.securityTerminal = (TileSecurity) locatable;
        
        // Verify tile entity is valid and loaded
        if (securityTerminal.isInvalid()) {
            return false;
        }
        
        // Get grid node
        this.gridNode = securityTerminal.getGridNode(ForgeDirection.UNKNOWN);
        if (this.gridNode == null) {
            return false;
        }
        
        // Verify node is active
        if (!gridNode.isActive()) {
            return false;
        }
        
        // Get grid
        this.grid = gridNode.getGrid();
        if (this.grid == null) {
            return false;
        }
        
        // Get storage grid
        this.storageGrid = this.grid.getCache(IStorageGrid.class);
        if (this.storageGrid == null) {
            return false;
        }
        
        // Get item storage
        this.itemStorage = this.storageGrid.getItemInventory();
        
        return this.itemStorage != null;
    }
    
    /**
     * Validate existing connection is still good.
     */
    private boolean isValid() {
        if (itemStorage == null) return false;
        if (securityTerminal == null || securityTerminal.isInvalid()) return false;
        if (gridNode == null || !gridNode.isActive()) return false;
        if (grid == null) return false;
        
        // Check if grid is powered
        IEnergyGrid energyGrid = grid.getCache(IEnergyGrid.class);
        if (energyGrid == null || !energyGrid.isNetworkPowered()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Invalidate cached connection state.
     */
    void invalidate() {
        this.securityTerminal = null;
        this.gridNode = null;
        this.grid = null;
        this.storageGrid = null;
        this.itemStorage = null;
    }
    
    /**
     * Check if player has permission to extract items.
     */
    boolean canExtract() {
        if (!isValid()) return false;
        
        ISecurityGrid securityGrid = grid.getCache(ISecurityGrid.class);
        if (securityGrid != null) {
            return securityGrid.hasPermission(player, SecurityPermissions.EXTRACT);
        }
        
        return true; // No security = allowed
    }
    
    /**
     * Check if player has permission to inject items.
     */
    boolean canInject() {
        if (!isValid()) return false;
        
        ISecurityGrid securityGrid = grid.getCache(ISecurityGrid.class);
        if (securityGrid != null) {
            return securityGrid.hasPermission(player, SecurityPermissions.INJECT);
        }
        
        return true; // No security = allowed
    }
    
    /**
     * Check if player is within wireless range.
     * Uses AE2's actual wireless system.
     */
    boolean isPlayerInWirelessRange() {
        if (grid == null) return false;
        
        // Iterate wireless access points in network
        for (IGridNode node : grid.getMachines(TileWireless.class)) {
            IGridHost host = node.getMachine();
            if (!(host instanceof TileWireless)) continue;
            
            TileWireless wireless = (TileWireless) host;
            
            // Check if wireless is active
            if (!wireless.isActive()) continue;
            
            // Check distance
            double range = wireless.getRange();
            TileEntity tile = (TileEntity) wireless;
            
            double distSq = player.getDistanceSq(
                tile.xCoord + 0.5,
                tile.yCoord + 0.5,
                tile.zCoord + 0.5
            );
            
            if (distSq <= range * range) {
                return true;
            }
        }
        
        return false;
    }
    
    // Getters for connected components
    IMEMonitor<IAEItemStack> getItemStorage() { return itemStorage; }
    TileSecurity getSecurityTerminal() { return securityTerminal; }
    IGrid getGrid() { return grid; }
}
```

#### 2.2 Create `AE2MaterialProvider`

**File:** `src/main/java/com/xXseesXx/patternwand/materials/ae2/AE2MaterialProvider.java`

**Key Changes:**
- Uses `AE2Connection` for lifecycle management
- Separate `canExtract()` / `canInject()` permission checks
- Caches storage list queries for batch operations

```java
/**
 * Material provider backed by an AE2 network.
 * Connects via security terminal and respects permissions/wireless range.
 */
public class AE2MaterialProvider implements IMaterialProvider {
    private final AE2Connection connection;
    
    // Cache for batch operations
    private IItemList<IAEItemStack> cachedStorageList;
    private long cacheTime = 0;
    private static final long CACHE_DURATION = 5; // ticks
    
    public AE2MaterialProvider(long securityTerminalId, EntityPlayer player) {
        this.connection = new AE2Connection(securityTerminalId, player);
    }
    
    @Override
    public int simulate(ItemStack stack, int amount) {
        if (!connection.ensureConnected()) {
            return 0;
        }
        
        if (!connection.canExtract()) {
            return 0;
        }
        
        if (!connection.isPlayerInWirelessRange()) {
            return 0;
        }
        
        IAEItemStack aeStack = AEApi.instance()
            .storage()
            .createItemStack(stack);
        aeStack.setStackSize(amount);
        
        // Use cached storage list if available
        IItemList<IAEItemStack> storageList = getCachedStorageList();
        
        // Find item in network
        IAEItemStack available = storageList.findPrecise(aeStack);
        
        if (available == null) {
            return 0;
        }
        
        return (int) Math.min(amount, available.getStackSize());
    }
    
    @Override
    public int consume(ItemStack stack, int amount) {
        if (!connection.ensureConnected()) {
            return 0;
        }
        
        if (!connection.canExtract()) {
            return 0;
        }
        
        if (!connection.isPlayerInWirelessRange()) {
            return 0;
        }
        
        IAEItemStack aeStack = AEApi.instance()
            .storage()
            .createItemStack(stack);
        aeStack.setStackSize(amount);
        
        // Extract from network
        IMEMonitor<IAEItemStack> storage = connection.getItemStorage();
        IAEItemStack extracted = storage.extractItems(
            aeStack,
            Actionable.MODULATE,
            new PlayerSource(
                connection.getPlayer(),
                connection.getSecurityTerminal()
            )
        );
        
        // Invalidate cache after modification
        invalidateCache();
        
        return extracted != null ? (int) extracted.getStackSize() : 0;
    }
    
    @Override
    public int inject(ItemStack stack, int amount) {
        if (!connection.ensureConnected()) {
            return 0;
        }
        
        if (!connection.canInject()) {
            return 0;
        }
        
        // NOTE: Injection doesn't require wireless range
        // You can return items to the network remotely
        
        IAEItemStack aeStack = AEApi.instance()
            .storage()
            .createItemStack(stack);
        aeStack.setStackSize(amount);
        
        // Inject into network
        IMEMonitor<IAEItemStack> storage = connection.getItemStorage();
        IAEItemStack remainder = storage.injectItems(
            aeStack,
            Actionable.MODULATE,
            new PlayerSource(
                connection.getPlayer(),
                connection.getSecurityTerminal()
            )
        );
        
        // Invalidate cache after modification
        invalidateCache();
        
        int injected = amount;
        if (remainder != null) {
            injected -= (int) remainder.getStackSize();
        }
        
        return injected;
    }
    
    /**
     * Get cached storage list for batch queries.
     * Prevents repeated getStorageList() calls.
     */
    private IItemList<IAEItemStack> getCachedStorageList() {
        long currentTime = connection.getPlayer().worldObj.getTotalWorldTime();
        
        if (cachedStorageList == null || (currentTime - cacheTime) > CACHE_DURATION) {
            IMEMonitor<IAEItemStack> storage = connection.getItemStorage();
            cachedStorageList = storage.getStorageList();
            cacheTime = currentTime;
        }
        
        return cachedStorageList;
    }
    
    /**
     * Invalidate cached storage list after modifications.
     */
    private void invalidateCache() {
        cachedStorageList = null;
        cacheTime = 0;
    }
    
    @Override
    public String getName() {
        return "AE2 Network";
    }
    
    /**
     * Test connection and provide detailed status.
     */
    public AE2ConnectionStatus getStatus() {
        if (!connection.ensureConnected()) {
            return new AE2ConnectionStatus(false, "Cannot connect to security terminal");
        }
        
        if (!connection.canExtract()) {
            return new AE2ConnectionStatus(false, "No EXTRACT permission");
        }
        
        if (!connection.canInject()) {
            return new AE2ConnectionStatus(false, "No INJECT permission");
        }
        
        if (!connection.isPlayerInWirelessRange()) {
            return new AE2ConnectionStatus(false, "Out of wireless range");
        }
        
        return new AE2ConnectionStatus(true, "Connected");
    }
    
    public static class AE2ConnectionStatus {
        public final boolean connected;
        public final String message;
        
        public AE2ConnectionStatus(boolean connected, String message) {
            this.connected = connected;
            this.message = message;
        }
    }
}
```

#### 2.3 Add NBT Fields to Wand

**In `ItemPatternWandUnbreakable.java`:**

```java
private static final String NBT_AE2_LINK = "ae2SecurityTerminal";

/**
 * Get linked AE2 security terminal ID.
 * @return Security terminal locatable ID, or null if not linked
 */
public Long getAE2Link(ItemStack wand) {
    if (wand != null && wand.hasTagCompound()) {
        NBTTagCompound tag = wand.getTagCompound();
        if (tag.hasKey(NBT_AE2_LINK)) {
            return tag.getLong(NBT_AE2_LINK);
        }
    }
    return null;
}

/**
 * Link wand to AE2 security terminal.
 */
public void setAE2Link(ItemStack wand, long securityTerminalId) {
    NBTTagCompound tag = getOrCreateTag(wand);
    tag.setLong(NBT_AE2_LINK, securityTerminalId);
}

/**
 * Unlink wand from AE2 network.
 */
public void clearAE2Link(ItemStack wand) {
    if (wand != null && wand.hasTagCompound()) {
        NBTTagCompound tag = wand.getTagCompound();
        tag.removeTag(NBT_AE2_LINK);
    }
}

/**
 * Check if wand is linked to AE2.
 */
public boolean isAE2Linked(ItemStack wand) {
    return getAE2Link(wand) != null;
}

private NBTTagCompound getOrCreateTag(ItemStack wand) {
    if (!wand.hasTagCompound()) {
        wand.setTagCompound(new NBTTagCompound());
    }
    return wand.getTagCompound();
}
```

### Phase 3: Pattern Plan System

Create the system for generating and executing placement plans from Lua patterns.

#### 3.1 Create `PatternPlan` - Placement Plan

**File:** `src/main/java/com/xXseesXx/patternwand/patterns/PatternPlan.java`

```java
/**
 * A complete plan for block placement generated from a pattern.
 * Separates pattern generation from material acquisition and execution.
 */
public class PatternPlan {
    /**
     * Single block placement in the plan.
     */
    public static class PlacementEntry {
        public final Point3d position;
        public final MaterialKey material;
        
        public PlacementEntry(Point3d position, MaterialKey material) {
            this.position = position;
            this.material = material;
        }
    }
    
    private final List<PlacementEntry> placements;
    private final Point3d origin;
    private final PlacementContext context;
    
    public PatternPlan(Point3d origin, PlacementContext context) {
        this.placements = new ArrayList<>();
        this.origin = origin;
        this.context = context;
    }
    
    /**
     * Add a block placement to the plan.
     */
    public void addPlacement(Point3d position, Block block, int meta) {
        MaterialKey key = MaterialKey.fromBlock(block, meta);
        if (key != null) {
            placements.add(new PlacementEntry(position, key));
        }
    }
    
    /**
     * Get aggregated material requirements for this plan.
     */
    public MaterialRequirements getMaterialRequirements() {
        MaterialRequirements requirements = new MaterialRequirements();
        
        for (PlacementEntry entry : placements) {
            requirements.add(entry.material, 1);
        }
        
        return requirements;
    }
    
    /**
     * Get all placements.
     */
    public List<PlacementEntry> getPlacements() {
        return Collections.unmodifiableList(placements);
    }
    
    /**
     * Get number of blocks to place.
     */
    public int size() {
        return placements.size();
    }
    
    public Point3d getOrigin() { return origin; }
    public PlacementContext getContext() { return context; }
}
```

#### 3.2 Modify `PatternWandWorker` - Add Plan Generation

**In `PatternWandWorker.java`:**

```java
/**
 * Generate a placement plan from the active pattern.
 * This executes the Lua pattern for all positions and records what should be placed.
 * Does NOT consume materials or place blocks yet.
 */
public PatternPlan generatePlan(LinkedList<Point3d> positions, 
                                String patternName,
                                ItemStack wandItem) throws PatternExecutionException {
    
    // Get the compiled pattern script
    CompiledScript script = PatternWandMod.proxy.getScriptLoader().getScript(patternName);
    if (script == null) {
        throw new PatternExecutionException("Pattern not found: " + patternName);
    }
    
    // Create placement context
    PlacementContext context = createPlacementContext(clickedPos, positions, 
        playerShim.getPlayer(), side);
    
    // Create plan
    PatternPlan plan = new PatternPlan(originPos, context);
    
    // Convert palette to inventory for script API
    IInventory paletteInventory = paletteToInventory(palette);
    
    // Get parameters and seed
    long seed = getPatternSeed(wandItem);
    Map<String, Object> params = extractParameters(wandItem, script);
    
    // Get palette entries for quick lookup
    List<PaletteEntry> paletteEntries = palette.getEntries();
    
    // Execute pattern for each position
    for (Point3d pos : positions) {
        try {
            // Calculate relative coordinates
            int relX = pos.x - originPos.x;
            int relY = pos.y - originPos.y;
            int relZ = pos.z - originPos.z;
            
            // Execute pattern to get palette index
            int paletteIndex = PatternWandMod.proxy.getScriptLoader()
                .getEngine()
                .executePattern(script, pos.x, pos.y, pos.z, 
                    relX, relY, relZ, paletteInventory, 
                    seed, params, context);
            
            // -1 means gap (skip)
            if (paletteIndex == -1) {
                continue;
            }
            
            // Get block from palette
            if (paletteIndex >= 0 && paletteIndex < paletteEntries.size()) {
                PaletteEntry entry = paletteEntries.get(paletteIndex);
                if (entry != null && entry.block != null) {
                    plan.addPlacement(pos, entry.block, entry.meta);
                }
            }
        } catch (ScriptExecutionException e) {
            throw new PatternExecutionException("Pattern execution failed at " + pos, e);
        }
    }
    
    return plan;
}

/**
 * Execute a placement plan by actually placing blocks in the world.
 * Assumes materials have already been acquired via MaterialReservation.
 */
public ArrayList<Point3d> executePlan(PatternPlan plan, 
                                       ItemStack wandItem,
                                       int side, 
                                       float hitX, float hitY, float hitZ) {
    
    ArrayList<Point3d> placedBlocks = new ArrayList<>();
    
    for (PatternPlan.PlacementEntry entry : plan.getPlacements()) {
        Point3d pos = entry.position;
        ItemStack blockToPlace = entry.material.toItemStack(1);
        
        // Place block at position using parent's placeBlocks
        LinkedList<Point3d> singleBlock = new LinkedList<>();
        singleBlock.add(pos);
        
        ArrayList<Point3d> placed = super.placeBlocks(
            wandItem,
            singleBlock,
            pos,
            blockToPlace,
            playerShim,
            side,
            hitX, hitY, hitZ
        );
        
        if (!placed.isEmpty()) {
            placedBlocks.add(pos);
        }
    }
    
    return placedBlocks;
}
```

### Phase 4: Integration - Wire Everything Together

#### 4.1 Modify `placeBlocks()` to Use Transactional Model

**In `PatternWandWorker.java`:**

```java
@Override
public ArrayList<Point3d> placeBlocks(ItemStack itemStack, LinkedList<Point3d> blocks, 
                                      Point3d clickedPos, ItemStack sourceItems, 
                                      IPlayerShim playerShim, int side, 
                                      float hitX, float hitY, float hitZ) {
    
    // Check if a pattern is active
    String activePattern = getActivePattern(itemStack);
    
    if (activePattern != null && !activePattern.isEmpty()) {
        // Use transactional pattern-based placement
        return placeBlocksWithPatternTransactional(
            itemStack, blocks, clickedPos, sourceItems, 
            playerShim, side, hitX, hitY, hitZ, activePattern);
    } else {
        // Use default palette-based placement
        return super.placeBlocks(itemStack, blocks, clickedPos, 
            sourceItems, playerShim, side, hitX, hitY, hitZ);
    }
}

/**
 * Place blocks using a scripted pattern with transactional material acquisition.
 * 
 * Flow:
 * 1. Generate pattern plan (Lua execution)
 * 2. Calculate material requirements
 * 3. Simulate material acquisition (check availability)
 * 4. If satisfied, commit reservation (consume materials)
 * 5. Execute plan (place blocks in world)
 */
private ArrayList<Point3d> placeBlocksWithPatternTransactional(
        ItemStack wandItem, LinkedList<Point3d> blocks, Point3d clickedPos,
        ItemStack sourceItems, IPlayerShim playerShim, int side,
        float hitX, float hitY, float hitZ, String patternName) {
    
    // Start timing for debug mode
    com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.startPatternTiming();
    
    try {
        // === PHASE 1: Generate Placement Plan ===
        PatternPlan plan = generatePlan(blocks, patternName, wandItem);
        
        if (plan.size() == 0) {
            // Nothing to place
            return new ArrayList<>();
        }
        
        // === PHASE 2: Calculate Material Requirements ===
        MaterialRequirements requirements = plan.getMaterialRequirements();
        
        // === PHASE 3: Build Material Provider Chain ===
        MaterialProviderChain materials = createMaterialChain(wandItem, playerShim);
        
        // === PHASE 4: Simulate Material Acquisition ===
        MaterialReservation reservation = materials.prepare(requirements);
        
        if (!reservation.isSatisfied()) {
            // Cannot satisfy requirements - report and abort
            reportMissingMaterials(reservation, playerShim.getPlayer());
            return new ArrayList<>();
        }
        
        // === PHASE 5: Commit Reservation (Actually Consume Materials) ===
        reservation.commit();
        
        // === PHASE 6: Execute Plan (Place Blocks in World) ===
        ArrayList<Point3d> placedBlocks = executePlan(plan, wandItem, side, hitX, hitY, hitZ);
        
        // Report success
        reportPlacementSuccess(plan.size(), placedBlocks.size(), playerShim.getPlayer());
        
        return placedBlocks;
        
    } catch (PatternExecutionException e) {
        PatternWandMod.LOG.error("Pattern execution failed", e);
        playerShim.getPlayer().addChatMessage(
            new ChatComponentText("§cPattern execution failed: " + e.getMessage())
        );
        return new ArrayList<>();
    } finally {
        // Finish timing and print summary
        com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.finishPatternTiming();
    }
}

/**
 * Create material provider chain based on wand configuration.
 */
private MaterialProviderChain createMaterialChain(ItemStack wandItem, IPlayerShim playerShim) {
    MaterialProviderChain chain = new MaterialProviderChain();
    
    // Add player inventory provider (consume first)
    chain.addConsumeProvider(new PlayerInventoryProvider(playerShim));
    
    // Add AE2 provider if linked (consume second)
    Long ae2Link = ((ItemPatternWandUnbreakable) wandItem.getItem()).getAE2Link(wandItem);
    if (ae2Link != null) {
        AE2MaterialProvider ae2Provider = new AE2MaterialProvider(ae2Link, playerShim.getPlayer());
        chain.addConsumeProvider(ae2Provider);
        
        // For injection: player first, then AE2
        chain.addInjectProvider(new PlayerInventoryProvider(playerShim));
        chain.addInjectProvider(ae2Provider);
    } else {
        // No AE2: only player inventory for injection
        chain.addInjectProvider(new PlayerInventoryProvider(playerShim));
    }
    
    return chain;
}

/**
 * Report missing materials to player.
 */
private void reportMissingMaterials(MaterialReservation reservation, EntityPlayer player) {
    Map<MaterialKey, Integer> missing = reservation.getMissing();
    
    player.addChatMessage(new ChatComponentText("§cInsufficient materials!"));
    
    int displayed = 0;
    for (Map.Entry<MaterialKey, Integer> entry : missing.entrySet()) {
        if (displayed >= 5) {
            int remaining = missing.size() - displayed;
            player.addChatMessage(new ChatComponentText(
                "§7... and " + remaining + " more material(s)"));
            break;
        }
        
        MaterialKey key = entry.getKey();
        int amount = entry.getValue();
        
        String itemName = key.toItemStack(1).getDisplayName();
        player.addChatMessage(new ChatComponentText(
            String.format("§7- Need %d more: §f%s", amount, itemName)));
        
        displayed++;
    }
}

/**
 * Report successful placement to player.
 */
private void reportPlacementSuccess(int planned, int placed, EntityPlayer player) {
    if (placed < planned) {
        player.addChatMessage(new ChatComponentText(
            String.format("§ePlaced %d of %d blocks", placed, planned)));
    } else {
        player.addChatMessage(new ChatComponentText(
            String.format("§aPlaced %d blocks", placed)));
    }
}
```

### Phase 5: Linking Interface

#### 5.1 Add Shift+Right-Click Linking

**In `ItemPatternWandUnbreakable.java`:**

```java
@Override
public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, 
                               int x, int y, int z, int side, 
                               float hitX, float hitY, float hitZ) {
    // Shift + right-click on Security Terminal = link
    if (player.isSneaking()) {
        TileEntity tile = world.getTileEntity(x, y, z);
        
        if (tile instanceof TileSecurity) {
            if (!world.isRemote) {
                TileSecurity security = (TileSecurity) tile;
                long encKey = security.getLocatableSerial();
                
                // Validate player has permission
                IGridNode node = security.getGridNode(ForgeDirection.UNKNOWN);
                if (node != null && node.isActive()) {
                    IGrid grid = node.getGrid();
                    if (grid != null) {
                        ISecurityGrid securityGrid = grid.getCache(ISecurityGrid.class);
                        if (securityGrid != null) {
                            // Check if player has at least one permission
                            boolean hasExtract = securityGrid.hasPermission(player, SecurityPermissions.EXTRACT);
                            boolean hasInject = securityGrid.hasPermission(player, SecurityPermissions.INJECT);
                            
                            if (!hasExtract && !hasInject) {
                                player.addChatMessage(new ChatComponentText(
                                    "§cYou don't have permission to use this network"));
                                return true;
                            }
                        }
                    }
                }
                
                // Link wand
                setAE2Link(stack, encKey);
                
                player.addChatMessage(new ChatComponentText("§aWand linked to AE2 network!"));
                player.addChatMessage(new ChatComponentText(
                    String.format("§7Security Terminal: %d, %d, %d", x, y, z)));
                
                world.playSoundAtEntity(player, "random.orb", 0.3F, 1.2F);
            }
            
            return true; // Consume click
        }
    }
    
    return false;
}
```

#### 5.2 Add Commands

**In `PatternWandCommand.java`:**

```java
private void handleLinkCommand(ICommandSender sender, String[] args) {
    if (!(sender instanceof EntityPlayer)) {
        throw new CommandException("Only players can use this command");
    }
    
    EntityPlayer player = (EntityPlayer) sender;
    ItemStack held = player.getHeldItem();
    
    if (held == null || !(held.getItem() instanceof ItemPatternWandUnbreakable)) {
        throw new CommandException("You must be holding a Pattern Wand");
    }
    
    ItemPatternWandUnbreakable wandItem = (ItemPatternWandUnbreakable) held.getItem();
    
    if (args.length == 0) {
        // Show link status
        Long link = wandItem.getAE2Link(held);
        if (link == null) {
            addChatMessage(player, "§7Wand is not linked to an AE2 network");
            addChatMessage(player, "§7Shift+right-click a Security Terminal to link");
        } else {
            addChatMessage(player, "§aWand is linked to AE2 network");
            addChatMessage(player, "§7Security Terminal ID: " + link);
            
            // Test connection and show status
            AE2MaterialProvider provider = new AE2MaterialProvider(link, player);
            AE2MaterialProvider.AE2ConnectionStatus status = provider.getStatus();
            
            if (status.connected) {
                addChatMessage(player, "§aStatus: " + status.message);
            } else {
                addChatMessage(player, "§cStatus: " + status.message);
            }
        }
    } else if (args[0].equalsIgnoreCase("unlink")) {
        // Unlink wand
        wandItem.clearAE2Link(held);
        addChatMessage(player, "§cWand unlinked from AE2 network");
    }
}
```

### Phase 6: Polish & User Experience

#### 6.1 Update Tooltip

**In `ItemPatternWandUnbreakable.addInformation()`:**

```java
// Add AE2 link status
Long ae2Link = getAE2Link(itemstack);
if (ae2Link != null) {
    lines.add("§bAE2: §aLinked");
    
    // Show connection status if holding shift
    if (GuiScreen.isShiftKeyDown()) {
        AE2MaterialProvider provider = new AE2MaterialProvider(ae2Link, player);
        AE2MaterialProvider.AE2ConnectionStatus status = provider.getStatus();
        
        if (status.connected) {
            lines.add("  §aConnected");
        } else {
            lines.add("  §c" + status.message);
        }
    } else {
        lines.add("  §7Hold shift for details");
    }
} else {
    lines.add("§bAE2: §7Not linked");
    lines.add("  §7Shift+click Security Terminal");
}
```

#### 6.2 Add Configuration Options

**In `Config.java`:**

```java
@Comment("Enable AE2 network integration")
public static boolean ae2IntegrationEnabled = true;

@Comment("Require wireless range for AE2 extraction (injection always works remotely)")
public static boolean ae2RequireWirelessForExtract = true;

@Comment("Multiplier for AE2 wireless range checks")
public static double ae2WirelessRangeMultiplier = 1.0;

@Comment("Material source priority: player_first, ae2_first, player_only, ae2_only")
public static String materialSourceMode = "player_first";
```

#### 6.3 Add Material Source Modes

**In `PatternWandWorker.createMaterialChain()`:**

```java
private MaterialProviderChain createMaterialChain(ItemStack wandItem, IPlayerShim playerShim) {
    MaterialProviderChain chain = new MaterialProviderChain();
    
    String mode = Config.materialSourceMode;
    Long ae2Link = ((ItemPatternWandUnbreakable) wandItem.getItem()).getAE2Link(wandItem);
    AE2MaterialProvider ae2Provider = null;
    
    if (ae2Link != null && Config.ae2IntegrationEnabled) {
        ae2Provider = new AE2MaterialProvider(ae2Link, playerShim.getPlayer());
    }
    
    // Configure consume order based on mode
    if (mode.equals("ae2_first") && ae2Provider != null) {
        chain.addConsumeProvider(ae2Provider);
        chain.addConsumeProvider(new PlayerInventoryProvider(playerShim));
    } else if (mode.equals("ae2_only") && ae2Provider != null) {
        chain.addConsumeProvider(ae2Provider);
    } else if (mode.equals("player_only")) {
        chain.addConsumeProvider(new PlayerInventoryProvider(playerShim));
    } else { // "player_first" (default)
        chain.addConsumeProvider(new PlayerInventoryProvider(playerShim));
        if (ae2Provider != null) {
            chain.addConsumeProvider(ae2Provider);
        }
    }
    
    // Configure inject order (always player → AE2)
    chain.addInjectProvider(new PlayerInventoryProvider(playerShim));
    if (ae2Provider != null) {
        chain.addInjectProvider(ae2Provider);
    }
    
    return chain;
}
```

```java
// In ItemPatternWandUnbreakable
private static final String NBT_AE2_LINK = "ae2SecurityTerminal";
private static final String NBT_AE2_ENABLED = "ae2Enabled";

/**
 * Get linked AE2 security terminal ID.
 * @return Security terminal locatable ID, or null if not linked
 */
public Long getAE2Link(ItemStack wand) {
    if (wand != null && wand.hasTagCompound()) {
        NBTTagCompound tag = wand.getTagCompound();
        if (tag.hasKey(NBT_AE2_LINK)) {
            return tag.getLong(NBT_AE2_LINK);
        }
    }
    return null;
}

/**
 * Link wand to AE2 security terminal.
 */
public void setAE2Link(ItemStack wand, long securityTerminalId) {
    NBTTagCompound tag = getOrCreateTag(wand);
    tag.setLong(NBT_AE2_LINK, securityTerminalId);
    tag.setBoolean(NBT_AE2_ENABLED, true);
}

/**
 * Unlink wand from AE2 network.
 */
public void clearAE2Link(ItemStack wand) {
    if (wand != null && wand.hasTagCompound()) {
        NBTTagCompound tag = wand.getTagCompound();
        tag.removeTag(NBT_AE2_LINK);
        tag.setBoolean(NBT_AE2_ENABLED, false);
    }
}
```

#### 2.2 Create `AE2MaterialProvider`

**File:** `src/main/java/com/xXseesXx/patternwand/integration/ae2/AE2MaterialProvider.java`

```java
public class AE2MaterialProvider implements IMaterialProvider {
    private final long securityTerminalId;
    private final EntityPlayer player;
    
    // Transient - recreated each use
    private transient TileSecurity securityTerminal;
    private transient IGrid grid;
    private transient IStorageGrid storageGrid;
    private transient IMEMonitor<IAEItemStack> itemStorage;
    
    public AE2MaterialProvider(long securityTerminalId, EntityPlayer player) {
        this.securityTerminalId = securityTerminalId;
        this.player = player;
    }
    
    /**
     * Connect to AE2 network via security terminal.
     */
    private boolean connectToNetwork() {
        // Resolve security terminal from locatable registry
        ILocatable locatable = AEApi.instance()
            .registries()
            .locatable()
            .getLocatableBy(securityTerminalId);
            
        if (!(locatable instanceof TileSecurity)) {
            return false;
        }
        
        this.securityTerminal = (TileSecurity) locatable;
        
        // Get grid node
        this.gridNode = securityTerminal.getGridNode(ForgeDirection.UNKNOWN);
        if (this.gridNode == null) {
            return false;
        }
        
        // Get grid
        this.grid = this.gridNode.getGrid();
        if (this.grid == null) {
            return false;
        }
        
        // Get storage grid
        this.storageGrid = this.grid.getCache(IStorageGrid.class);
        if (this.storageGrid == null) {
            return false;
        }
        
        // Get item storage
        this.itemStorage = this.storageGrid.getItemInventory();
        
        return this.itemStorage != null;
    }
    
    /**
     * Check if player has permission and is in wireless range.
     */
    private boolean canInteractWithAE() {
        if (grid == null || securityTerminal == null) {
            return false;
        }
        
        // Check if grid is powered
        IEnergyGrid energyGrid = grid.getCache(IEnergyGrid.class);
        if (energyGrid == null || !energyGrid.isNetworkPowered()) {
            return false;
        }
        
        // Check permissions
        ISecurityGrid securityGrid = grid.getCache(ISecurityGrid.class);
        if (securityGrid != null) {
            if (!securityGrid.hasPermission(player, SecurityPermissions.EXTRACT)) {
                return false;
            }
            if (!securityGrid.hasPermission(player, SecurityPermissions.INJECT)) {
                return false;
            }
        }
        
        // Check wireless range
        return isPlayerInWirelessRange();
    }
    
    /**
     * Check if player is within range of a wireless access point.
     */
    private boolean isPlayerInWirelessRange() {
        // Iterate wireless access points in network
        for (IGridNode node : grid.getMachines(TileWireless.class)) {
            IGridHost host = node.getMachine();
            if (host instanceof TileWireless) {
                TileWireless wireless = (TileWireless) host;
                
                // Check if wireless is active
                if (!wireless.isActive()) {
                    continue;
                }
                
                // Check distance
                double range = wireless.getRange();
                TileEntity tile = (TileEntity) wireless;
                
                double distSq = player.getDistanceSq(
                    tile.xCoord + 0.5,
                    tile.yCoord + 0.5,
                    tile.zCoord + 0.5
                );
                
                if (distSq <= range * range) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public int simulate(ItemStack itemStack, int count) {
        if (!ensureConnected()) {
            return 0;
        }
        
        IAEItemStack aeStack = AEApi.instance()
            .storage()
            .createItemStack(itemStack);
        aeStack.setStackSize(count);
        
        // Find item in network
        IAEItemStack available = itemStorage
            .getStorageList()
            .findPrecise(aeStack);
            
        if (available == null) {
            return 0;
        }
        
        return (int) Math.min(count, available.getStackSize());
    }
    
    @Override
    public int consume(ItemStack itemStack, int count) {
        if (!ensureConnected()) {
            return 0;
        }
        
        IAEItemStack aeStack = AEApi.instance()
            .storage()
            .createItemStack(itemStack);
        aeStack.setStackSize(count);
        
        // Extract from network
        IAEItemStack extracted = itemStorage.extractItems(
            aeStack,
            Actionable.MODULATE,
            new PlayerSource(player, securityTerminal)
        );
        
        return extracted != null ? (int) extracted.getStackSize() : 0;
    }
    
    @Override
    public int inject(ItemStack itemStack, int count) {
        if (!ensureConnected()) {
            return 0;
        }
        
        IAEItemStack aeStack = AEApi.instance()
            .storage()
            .createItemStack(itemStack);
        aeStack.setStackSize(count);
        
        // Inject into network
        IAEItemStack remainder = itemStorage.injectItems(
            aeStack,
            Actionable.MODULATE,
            new PlayerSource(player, securityTerminal)
        );
        
        int injected = count;
        if (remainder != null) {
            injected -= (int) remainder.getStackSize();
        }
        
        return injected;
    }
    
    private boolean ensureConnected() {
        if (itemStorage == null) {
            if (!connectToNetwork()) {
                return false;
            }
        }
        return canInteractWithAE();
    }
    
    @Override
    public String getName() {
        return "AE2 Network";
    }
    
    @Override
    public int getPriority() {
        return 50; // Lower than player inventory (100)
    }
}
```

### Phase 3: Linking Commands & GUI

#### 3.1 Add Linking Command

**Command:** `/patternwand link` (or `/pw link`)

Usage:
```
/pw link           - Link wand to clicked security terminal
/pw unlink         - Unlink wand from AE2 network
/pw linkstatus     - Show current AE2 link status
```

**Implementation in PatternWandCommand:**

```java
private void handleLinkCommand(ICommandSender sender, String[] args) {
    if (!(sender instanceof EntityPlayer)) {
        throw new CommandException("Only players can link wands");
    }
    
    EntityPlayer player = (EntityPlayer) sender;
    ItemStack held = player.getHeldItem();
    
    if (held == null || !(held.getItem() instanceof IPatternWandItem)) {
        throw new CommandException("You must be holding a Pattern Wand");
    }
    
    if (args.length == 0 || args[0].equals("link")) {
        // Look at what the player is targeting
        MovingObjectPosition mop = getPlayerLookTarget(player, 5.0);
        
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            throw new CommandException("You must be looking at an AE2 Security Terminal");
        }
        
        TileEntity tile = player.worldObj.getTileEntity(
            mop.blockX, mop.blockY, mop.blockZ
        );
        
        if (!(tile instanceof TileSecurity)) {
            throw new CommandException("You must be looking at an AE2 Security Terminal");
        }
        
        TileSecurity security = (TileSecurity) tile;
        
        // Get the locatable ID
        long encKey = security.getLocatableSerial();
        
        // Link wand
        ((ItemPatternWandUnbreakable) held.getItem()).setAE2Link(held, encKey);
        
        addChatMessage(player, "§aWand linked to AE2 network!");
        addChatMessage(player, "§7Security Terminal: " + 
            mop.blockX + ", " + mop.blockY + ", " + mop.blockZ);
        
    } else if (args[0].equals("unlink")) {
        ((ItemPatternWandUnbreakable) held.getItem()).clearAE2Link(held);
        addChatMessage(player, "§cWand unlinked from AE2 network");
        
    } else if (args[0].equals("linkstatus")) {
        Long link = ((ItemPatternWandUnbreakable) held.getItem()).getAE2Link(held);
        
        if (link == null) {
            addChatMessage(player, "§7Wand is not linked to an AE2 network");
        } else {
            addChatMessage(player, "§aWand is linked to AE2 network");
            addChatMessage(player, "§7Security Terminal ID: " + link);
            
            // Try to connect and show status
            AE2MaterialProvider provider = new AE2MaterialProvider(link, player);
            // Test connection...
        }
    }
}
```

#### 3.2 Add GUI Link Button

Add a button to the wand GUI for linking:
- "Link to AE2" button (opens linking mode)
- Shows current link status
- "Unlink" button when linked

### Phase 4: Integration with PatternWandWorker

Modify `PatternWandWorker.placeBlocksWithPattern()` to use the material provider chain:

```java
private ArrayList<Point3d> placeBlocksWithPattern(...) {
    // Build material provider chain
    MaterialProviderChain materials = new MaterialProviderChain();
    
    // Add player inventory provider
    materials.addProvider(new PlayerInventoryProvider(playerShim));
    
    // Add AE2 provider if linked
    Long ae2Link = ((ItemPatternWandUnbreakable) itemStack.getItem())
        .getAE2Link(itemStack);
    if (ae2Link != null) {
        materials.addProvider(
            new AE2MaterialProvider(ae2Link, playerShim.getPlayer())
        );
    }
    
    // For each block to place...
    for (Point3d pos : blocks) {
        // ... execute pattern to get block type ...
        ItemStack blockToPlace = ...;
        
        // SIMULATION PASS: Check if we can get the block
        int available = materials.simulate(blockToPlace, 1);
        if (available < 1) {
            continue; // Skip if can't source
        }
        
        // ACTUAL CONSUMPTION
        int consumed = materials.consume(blockToPlace, 1);
        if (consumed > 0) {
            // Place block in world
            // ... placement logic ...
            placedBlocks.add(pos);
        }
    }
    
    return placedBlocks;
}
```

### Phase 5: Polish & Features

#### 5.1 Visual Feedback

- Chat messages showing material sources:
  ```
  Placed 1,247 blocks
  - 53 from player inventory
  - 1,194 from AE2 network
  ```

#### 5.2 Configuration

Add to `Config.java`:
```java
public static boolean ae2IntegrationEnabled = true;
public static double ae2WirelessRangeMultiplier = 1.0;
public static boolean ae2RequireWireless = true;
```

#### 5.3 Tooltip Information

Update `addInformation()` to show AE2 status:
```java
if (ae2Link != null) {
    lines.add("§bAE2: §aLinked");
    lines.add("§7Right-click security terminal to link");
} else {
    lines.add("§bAE2: §7Not linked");
    lines.add("§7Use /pw link while looking at security terminal");
}
```

## Dependencies

Add to `dependencies.gradle`:
```groovy
dependencies {
    // AE2 integration (compile-time only, optional at runtime)
    compileOnly "appeng:appliedenergistics2:rv3-beta-6:dev"
}
```

Add soft dependency to `@Mod` annotation:
```java
@Mod(
    modid = PatternWandMod.MODID,
    dependencies = "required-after:BuildMod;after:appliedenergistics2"
)
```

## Architecture Improvements Over Initial Plan

### ✅ Transactional Material Acquisition
- **Old:** Per-block `simulate → consume → place`
- **New:** Aggregate `generate plan → reserve → commit → execute`
- **Benefit:** Prevents partial builds, ensures atomic operations

### ✅ Separation of Policy and Capability
- **Old:** Providers have `getPriority()`
- **New:** Chain explicitly defines order
- **Benefit:** Easy to support multiple modes (player_first, ae2_first, etc.)

### ✅ Connection Lifecycle Management
- **Old:** Assumed cached connection remains valid
- **New:** Validates and reconnects as needed
- **Benefit:** Handles grid changes, chunk unloads, network events

### ✅ Separate Extract/Inject Permissions
- **Old:** Required both EXTRACT and INJECT for all operations
- **New:** Only checks permission needed for specific operation
- **Benefit:** More flexible permission configurations

### ✅ Storage List Caching
- **Old:** `getStorageList().findPrecise()` for every block
- **New:** Cache storage list for batch operations
- **Benefit:** Massively better performance for large patterns

### ✅ Better Injection Priority
- **Old:** AE2 first for returns
- **New:** Player inventory first, then AE2
- **Benefit:** More intuitive for wand usage

### ✅ Shift+Right-Click Linking
- **Old:** Command-based linking
- **New:** Shift+right-click Security Terminal
- **Benefit:** Natural interaction, wand touches terminal

### ✅ MaterialKey for NBT Handling
- **Old:** Block + metadata only
- **New:** Item + damage + NBT
- **Benefit:** Correctly handles machines, configured items, etc.

## Testing Plan

### 1. Material System Foundation (Phase 1)
- **MaterialKey equality**: Blocks with same/different NBT
- **MaterialRequirements**: Aggregate counting
- **MaterialReservation**: Simulation vs commit
- **PlayerInventoryProvider**: Simulate, consume, inject

### 2. AE2 Integration (Phase 2)
- **Connection lifecycle**:
  - Link to security terminal
  - Reconnect after chunk unload
  - Handle terminal destruction
  - Handle grid split/merge
- **Permissions**:
  - Extract permission required for consumption
  - Inject permission required for injection
  - No permission → graceful fallback
- **Wireless range**:
  - Inside range → works
  - Outside range → falls back to player inventory
  - Multiple wireless access points

### 3. Pattern Plan System (Phase 3)
- **Plan generation**:
  - Simple pattern (single block type)
  - Complex pattern (multiple block types)
  - Large pattern (10,000+ blocks)
- **Material requirements**:
  - Accurate counting
  - Multiple material types
  - NBT-sensitive materials

### 4. Transactional Integration (Phase 4)
- **Successful operations**:
  - All materials available in player inventory
  - All materials available in AE2
  - Split across player + AE2
- **Failed operations**:
  - Missing materials → nothing consumed
  - Partial materials → abort with report
- **Material source priority**:
  - player_first: Uses player inventory first
  - ae2_first: Uses AE2 first
  - player_only: Ignores AE2
  - ae2_only: Ignores player inventory

### 5. Linking Interface (Phase 5)
- **Shift+right-click linking**:
  - On Security Terminal → link
  - On other blocks → open GUI
- **Permission validation**:
  - Link attempt without permission → error message
  - Link with permission → success
- **Commands**:
  - `/pw link` → show status
  - `/pw link unlink` → unlink

### 6. Edge Cases
- **World operations**:
  - World save/reload with linked wand
  - Dimension change
  - Server restart
- **Network changes**:
  - Security terminal destroyed while linked
  - Network power loss during operation
  - Grid split/merge
- **Material edge cases**:
  - NBT-tagged items (machines with config)
  - Metadata items (logs with rotation)
  - Full player inventory + full AE2 network

## Performance Considerations

### Batch Pattern Operations
For a 10,000 block pattern:
- **Pattern generation:** ~10-50ms (Lua execution)
- **Material aggregation:** ~1ms (counting)
- **AE2 simulation:** ~5-20ms (storage list query × unique materials)
- **Material commitment:** ~50-200ms (AE2 extract calls)
- **World placement:** ~100-500ms (block placement)

**Total:** ~166-770ms for 10,000 blocks (0.17-0.77 seconds)

### Optimization: Storage List Caching
Without caching: `getStorageList().findPrecise()` × 10,000 = very slow

With caching: `getStorageList()` × 1 + lookup × unique materials = fast

For a pattern using 5 different block types:
- **Without cache:** 10,000 storage list queries
- **With cache:** 1 storage list query + 5 lookups

**Speedup:** ~2000x for this case

## Future Enhancements

### 1. Pending Materials Buffer (Inspired by Matter Manipulator)
For operations that replace blocks:
```
Break blocks → pending buffer → place blocks → unused → player/AE2
```
Prevents unnecessary AE2 inject→extract cycles.

### 2. Crafting Integration
- Request crafting from AE2 if item not in stock
- Show crafting progress
- Wait for crafting to complete

### 3. Material Preview
Show in GUI or HUD:
```
Required materials:
  1,000 stone (✓ 500 player, ✓ 500 AE2)
    500 glass (✓ 500 AE2)
    100 iron (✗ only 80 available)
```

### 4. Multiple Network Support
- Link to multiple security terminals
- Priority ordering between networks
- Network selection in GUI

### 5. GTNH Uplink Integration
Add third provider in chain for GTNH mod support.

### 6. Undo/Redo with AE2
Store broken blocks in AE2, retrieve for undo.

## Matter Manipulator Feature Parity

✅ Persistent link via security terminal locatable ID  
✅ Transient connection state (recreated each use)  
✅ Connection validation and reconnection  
✅ Security permission checks (separate extract/inject)  
✅ Wireless range validation  
✅ **Transactional reservation model**  
✅ Network power status checks  
✅ **Aggregate material requirements before consumption**  
✅ Player inventory → AE2 priority cascade  
✅ Bidirectional (consume and inject)  

## Key Improvements Over Matter Manipulator

1. **Cleaner separation:** Pattern generation → material system → world modification
2. **Material source modes:** Configurable priority (player_first, ae2_first, etc.)
3. **Better caching:** Storage list cached for batch operations
4. **Simpler linking:** Shift+right-click vs GUI-based
5. **Pattern-focused:** Integrates seamlessly with Lua scripting

## Implementation Priority

### ✅ Phase 1: Core Material System (CRITICAL)
Foundation for everything else. Must be solid.
- MaterialKey (NBT handling)
- MaterialRequirements (aggregation)
- MaterialReservation (transaction model)
- IMaterialProvider interface
- MaterialProviderChain (policy)
- PlayerInventoryProvider

### ✅ Phase 2: AE2 Integration (CORE FEATURE)
The actual AE2 connection mechanism.
- AE2Connection (lifecycle management)
- AE2MaterialProvider (network interface)
- Wand NBT fields

### ✅ Phase 3: Pattern Plan System (INTEGRATION)
Bridge between Lua patterns and material system.
- PatternPlan (placement plan)
- generatePlan() (Lua execution)
- executePlan() (world modification)

### ✅ Phase 4: Wire It Together (MAKES IT WORK)
Connect all the pieces.
- placeBlocksWithPatternTransactional()
- createMaterialChain()
- Error reporting

### ⚠️ Phase 5: Linking Interface (USER EXPERIENCE)
How users interact with the system.
- Shift+right-click linking
- `/pw link` command
- Permission validation

### 💡 Phase 6: Polish (NICE TO HAVE)
Improve user experience.
- Tooltips with status
- Configuration options
- Material source modes
- Better error messages

## Summary

This revised plan addresses all major architectural concerns:

1. **✅ Transactional model:** No more partial builds
2. **✅ Policy separation:** Chain controls order, not providers
3. **✅ Connection validation:** Handles grid changes properly
4. **✅ Performance:** Storage list caching for large patterns
5. **✅ UX:** Shift+right-click linking feels natural
6. **✅ Permissions:** Separate extract/inject checks
7. **✅ NBT handling:** MaterialKey for complex items
8. **✅ Injection priority:** Player first, then AE2

The architecture is now **production-ready** and follows Matter Manipulator's excellent design principles while being adapted for PatternWand's unique Lua pattern system.

**Estimated implementation time:**
- Phase 1: 4-6 hours (foundation)
- Phase 2: 4-6 hours (AE2 integration)
- Phase 3: 2-3 hours (pattern plans)
- Phase 4: 2-3 hours (wiring)
- Phase 5: 2-3 hours (linking UX)
- Phase 6: 2-4 hours (polish)

**Total:** ~16-25 hours for complete implementation
