# Pattern Wand Inventory Persistence Fix

## Problem
The wand wasn't storing its palette inventory data when closing the GUI.

## Root Cause
The GUI changes happen on the **client side**, but the NBT data is stored on the **server side**. The original code only saved on the server:

```java
if (!player.worldObj.isRemote) {
    savePaletteToWand();
}
```

This meant client-side changes were never synced to the server.

## Solution
Implemented client-to-server packet synchronization (matching BetterBuildersWands fork behavior):

### 1. Created Network Infrastructure
- **GenericHandler.java** - Base class for packet handlers
- **PacketSyncPalette.java** - Packet to send palette data from client to server

### 2. Registered Network Handler
In `PatternWandMod.preInit()`:
```java
networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
networkWrapper.registerMessage(PacketSyncPalette.Handler.class, PacketSyncPalette.class, 0, Side.SERVER);
```

### 3. Updated ContainerPatternWand.onContainerClosed()
- **Client side**: Builds palette NBT and sends packet to server
- **Server side**: Saves directly to wand NBT

### 4. Fixed NBT Format Mismatch
Updated `PatternPalette.fromNBT()` to read the correct keys:
- Changed from: "block" and "meta"
- Changed to: "id" and "Damage" (matching what ContainerPatternWand saves)

## How It Works Now

1. Player opens Pattern Wand GUI
2. Player configures palette with ghost items
3. Player closes GUI:
   - **Client**: Builds NBT with all 27 slots (id, Damage, Count)
   - **Client**: Sends PacketSyncPalette to server
   - **Server**: Receives packet and writes NBT to wand ItemStack
   - **Server**: Increments paletteVersion for TOCTOU protection
4. Wand now has persistent palette data!

## Files Modified
- `PatternWandMod.java` - Added network wrapper registration
- `ContainerPatternWand.java` - Client sends packet, server saves directly
- `PatternPalette.java` - Fixed fromNBT to read correct keys
- **New:** `network/GenericHandler.java`
- **New:** `network/PacketSyncPalette.java`

## Build Status
✅ BUILD SUCCESSFUL in 4s
