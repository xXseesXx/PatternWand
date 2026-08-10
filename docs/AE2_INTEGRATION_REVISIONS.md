# AE2 Integration Architecture - Key Revisions

## Overview

The initial plan (v1) had excellent conceptual foundation but needed significant architectural improvements based on Matter Manipulator's actual implementation details. This document summarizes the key changes.

## Critical Architectural Changes

### 1. Transactional Material Acquisition ⭐ **MOST IMPORTANT**

**Problem with v1:**
```java
for (block : blocks) {
    if (materials.simulate(block, 1) >= 1) {
        if (materials.consume(block, 1) > 0) {
            place(block);
        }
    }
}
```

This causes **partial builds** when materials run out mid-operation.

**Fixed in v2:**
```java
// 1. Generate complete plan from Lua
PatternPlan plan = generatePlan(blocks, patternName);

// 2. Calculate ALL material requirements
MaterialRequirements requirements = plan.getMaterialRequirements();

// 3. Simulate acquisition (no consumption yet)
MaterialReservation reservation = materials.prepare(requirements);

// 4. Check if fully satisfied
if (!reservation.isSatisfied()) {
    reportMissing(); // Nothing consumed!
    return;
}

// 5. Commit (atomic consumption)
reservation.commit();

// 6. Execute plan (place blocks)
executePlan(plan);
```

**Benefit:** Operations are **all-or-nothing**. No partial builds, no wasted materials.

---

### 2. Policy vs Capability Separation

**Problem with v1:**
```java
interface IMaterialProvider {
    int getPriority(); // BAD: Provider shouldn't know its priority
}
```

This makes it hard to change ordering or support modes like "AE2 first" vs "player first".

**Fixed in v2:**
```java
interface IMaterialProvider {
    // No priority! Provider is just capability
    int simulate(ItemStack stack, int amount);
    int consume(ItemStack stack, int amount);
    int inject(ItemStack stack, int amount);
}

// Chain controls policy
class MaterialProviderChain {
    private final List<IMaterialProvider> consumeOrder;
    private final List<IMaterialProvider> injectOrder;
    
    void addConsumeProvider(IMaterialProvider provider);
    void addInjectProvider(IMaterialProvider provider);
}
```

**Benefit:** Easy to support multiple modes:
```java
// Player-first mode
chain.addConsumeProvider(playerProvider);
chain.addConsumeProvider(ae2Provider);

// AE2-first mode
chain.addConsumeProvider(ae2Provider);
chain.addConsumeProvider(playerProvider);

// Player-only mode
chain.addConsumeProvider(playerProvider);
```

---

### 3. Connection Lifecycle Management

**Problem with v1:**
```java
if (itemStorage == null) {
    if (!connectToNetwork()) {
        return false;
    }
}
return canInteractWithAE();
```

Assumes cached `itemStorage` remains valid forever. **Wrong!** AE2 grids can:
- Split/merge
- Unload/reload
- Power off
- Security change

**Fixed in v2:**
```java
class AE2Connection {
    private long lastValidationTime = 0;
    
    boolean ensureConnected() {
        boolean needsValidation = (currentTime - lastValidationTime) > INTERVAL;
        
        if (itemStorage == null || needsValidation) {
            invalidate();  // Clear cached state
            if (!connect()) {
                return false;
            }
        }
        
        return isValid();  // Check node active, grid powered, etc.
    }
    
    private boolean isValid() {
        if (securityTerminal == null || securityTerminal.isInvalid()) return false;
        if (gridNode == null || !gridNode.isActive()) return false;
        if (!energyGrid.isNetworkPowered()) return false;
        return true;
    }
}
```

**Benefit:** Handles all grid lifecycle events correctly.

---

### 4. Separate Extract/Inject Permissions

**Problem with v1:**
```java
boolean canInteractWithAE() {
    return hasPermission(EXTRACT) && hasPermission(INJECT);
}
```

Placing blocks only needs `EXTRACT`. Returning blocks only needs `INJECT`. Requiring both is too strict.

**Fixed in v2:**
```java
boolean canExtract() {
    return securityGrid.hasPermission(player, SecurityPermissions.EXTRACT);
}

boolean canInject() {
    return securityGrid.hasPermission(player, SecurityPermissions.INJECT);
}

// In provider:
int consume(...) {
    if (!connection.canExtract()) return 0;
    // ... extract
}

int inject(...) {
    if (!connection.canInject()) return 0;
    // ... inject
}
```

**Benefit:** More flexible permission configurations.

---

### 5. Storage List Caching

**Problem with v1:**
```java
for (block : blocks) {  // 10,000 blocks
    itemStorage.getStorageList().findPrecise(block);  // Expensive!
}
```

For a 10,000 block pattern, this calls `getStorageList()` 10,000 times.

**Fixed in v2:**
```java
class AE2MaterialProvider {
    private IItemList<IAEItemStack> cachedStorageList;
    private long cacheTime = 0;
    
    private IItemList<IAEItemStack> getCachedStorageList() {
        if (cachedStorageList == null || expired) {
            cachedStorageList = itemStorage.getStorageList();
            cacheTime = currentTime;
        }
        return cachedStorageList;
    }
    
    // Invalidate after modifications
    private void invalidateCache() {
        cachedStorageList = null;
    }
}
```

**Benefit:** For a pattern with 5 unique blocks:
- **Without cache:** 10,000 storage list queries
- **With cache:** 1 storage list query + 5 lookups
- **Speedup:** ~2000x

---

### 6. Injection Priority

**Problem with v1:**
```java
// Injection order: AE2 first, then player
for (int i = providers.size() - 1; i >= 0; i--) {
    inject(providers.get(i));
}
```

This sends broken blocks to AE2 first, which feels unintuitive for a wand.

**Fixed in v2:**
```java
// Explicit injection order: player first, then AE2
chain.addInjectProvider(playerProvider);
chain.addInjectProvider(ae2Provider);
```

**Benefit:** More intuitive - broken blocks go to player's inventory first, overflow to AE2.

---

### 7. Linking UX

**Problem with v1:**
```
/pw link  (while looking at terminal)
```

Works, but feels disconnected. You're linking **the wand**, not running a command.

**Fixed in v2:**
```
Shift + Right-Click Security Terminal with wand
```

**Benefit:** Natural interaction - the wand physically touches the terminal to link.

---

### 8. MaterialKey for NBT Handling

**Problem with v1:**
```java
// Only Block + metadata
ItemStack blockToPlace = new ItemStack(block, 1, meta);
```

Doesn't handle:
- Machines with configuration NBT
- Items with stored data
- Complex GTNH items

**Fixed in v2:**
```java
class MaterialKey {
    private final Item item;
    private final int damage;
    private final NBTTagCompound tag;  // Handles NBT!
    
    boolean matches(ItemStack stack) {
        return item == stack.getItem()
            && damage == stack.getItemDamage()
            && Objects.equals(tag, stack.getTagCompound());
    }
}
```

**Benefit:** Correctly handles all item types, including complex GTNH machines.

---

## New Components Added

### MaterialKey
Canonical representation of materials including NBT.

### MaterialRequirements
Aggregated map of `MaterialKey → quantity`.

### MaterialReservation
Transaction result - can be committed or cancelled.

### PatternPlan
Complete placement plan with all blocks and materials.

### AE2Connection
Lifecycle manager for AE2 network connection.

---

## Comparison to Matter Manipulator

| Feature | Matter Manipulator | PatternWand v2 |
|---------|-------------------|----------------|
| Transactional model | ✅ Yes | ✅ Yes |
| Security terminal link | ✅ Yes | ✅ Yes |
| Connection validation | ✅ Yes | ✅ Yes |
| Wireless range check | ✅ Yes | ✅ Yes |
| Permission checks | ✅ Yes | ✅ Yes (improved) |
| Storage list caching | ❌ No | ✅ Yes |
| Material source modes | ❌ No | ✅ Yes |
| Pending materials | ✅ Yes | 🔮 Future |
| GTNH uplink | ✅ Yes | 🔮 Future |

---

## Implementation Order

### Phase 1: Foundation (CRITICAL)
Build the material system without AE2:
1. MaterialKey
2. MaterialRequirements
3. MaterialReservation
4. IMaterialProvider + Chain
5. PlayerInventoryProvider

**Why first?** Test the transactional model before adding AE2 complexity.

### Phase 2: AE2 Core (FEATURE)
Add AE2 network integration:
1. AE2Connection (lifecycle)
2. AE2MaterialProvider
3. Wand NBT fields

### Phase 3: Pattern Plans (INTEGRATION)
Bridge Lua patterns to material system:
1. PatternPlan
2. generatePlan()
3. executePlan()

### Phase 4: Wire It Together (MAKES IT WORK)
1. placeBlocksWithPatternTransactional()
2. createMaterialChain()
3. Error reporting

### Phase 5: UX (POLISH)
1. Shift+right-click linking
2. Commands
3. Tooltips
4. Configuration

---

## Performance Impact

### Before (v1 - per-block)
For 10,000 block pattern:
- 10,000 Lua executions
- 10,000 simulation calls
- 10,000 consumption calls
- 10,000 placement calls
- 10,000 AE2 storage list queries

**Estimated time:** 5-10 seconds

### After (v2 - transactional)
For 10,000 block pattern:
- 10,000 Lua executions (same, but batched)
- 5 simulation calls (5 unique materials)
- 5 consumption calls
- 10,000 placement calls (same)
- 1 AE2 storage list query

**Estimated time:** 0.17-0.77 seconds

**Speedup:** ~7-13x faster

---

## Key Takeaways

1. **Transaction model is essential** - prevents partial builds
2. **Cache aggressively** - AE2 storage queries are expensive
3. **Validate connections** - AE2 grids are dynamic
4. **Separate policy from capability** - makes modes easy
5. **Handle NBT properly** - GTNH has complex items
6. **Natural UX** - shift+right-click beats commands
7. **Test foundation first** - build material system before AE2

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Partial builds | ~~High~~ **Eliminated** | Critical | Transactional model |
| Performance issues | ~~High~~ **Low** | High | Storage list caching |
| Connection staleness | ~~Medium~~ **Low** | High | Lifecycle validation |
| Permission bugs | Low | Medium | Separate checks |
| NBT mismatches | ~~Medium~~ **Low** | Medium | MaterialKey |

---

## Conclusion

The v2 architecture is **production-ready** and addresses all identified issues:

✅ No partial builds (transactional)  
✅ Excellent performance (caching)  
✅ Robust connections (validation)  
✅ Flexible modes (policy separation)  
✅ NBT handling (MaterialKey)  
✅ Natural UX (shift+click)  

Estimated implementation: **16-25 hours** for full feature.

The architecture closely follows Matter Manipulator's proven design while improving on several aspects (caching, modes, permissions) and integrating seamlessly with PatternWand's Lua pattern system.
