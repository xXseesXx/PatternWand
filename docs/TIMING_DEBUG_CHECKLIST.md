# Debug Timing Display - Diagnostic Checklist

## Issue
Debug timing not showing in player chat after `/pw debug on`

## Latest Build
The latest build (commit `542dc35`) includes extensive logging to diagnose the issue.

## How to Diagnose

### Step 1: Enable Debug Mode
```
/pw debug on
```

**Expected response:**
```
Debug mode enabled
Pattern scripts can now output debug messages using debug.print()
Pattern execution timing will be tracked and displayed
```

If you don't see this, the command isn't working at all.

### Step 2: Use Pattern
Right-click with wand to place blocks.

**You should see in chat:**
```
[Debug] Pattern execution started...
```

If you see this, chat messaging WORKS and debug is enabled.

### Step 3: Check Console/Logs

Look for these messages in your console or `logs/latest.log`:

```
[PatternWand Debug] Starting timing for player: YourUsername
[PatternWand Debug] finishPatternTiming called, debugEnabled=true
[PatternWand Debug] Generating timing report, currentPlayer=EntityPlayerMP['YourUsername'/...]
[PatternWand Debug] Sending timing to player: YourUsername
[PatternWand Debug] === Pattern Execution Timing ===
[PatternWand Debug] Total: X.XX ms (N blocks placed, M planned)
...
```

## Diagnostic Scenarios

### Scenario A: No Console Messages At All
**Problem:** Timing method not being called
**Possible causes:**
- Pattern not executing (check if blocks are placed)
- Method not in code path (compilation issue)
- Early return before timing

**Solution:** Verify blocks are actually being placed

### Scenario B: Console Shows "debugEnabled=false"
**Problem:** Debug mode not actually enabled
**Possible causes:**
- Command runs client-side, timing runs server-side (desync)
- Static field not shared between client/server
- Debug mode disabled between command and execution

**Solution:**  
The static `debugEnabled` field might be client/server separated. Need to:
1. Store debug state per-player in NBT, OR
2. Use server-side config instead of static field

### Scenario C: Console Shows "player is null!"
**Problem:** Player object not available when finishing timing
**Possible causes:**
- Player disconnected during execution (unlikely)
- Player object doesn't persist through execution
- Server-side player object vs client-side

**Solution:**
Pass player through all phase methods or store in thread-local

### Scenario D: Console Shows Messages Sent But Not In Chat
**Problem:** Messages sent to wrong player object or client doesn't receive
**Possible causes:**
- Server-side EntityPlayerMP vs client-side EntityClientPlayerMP
- Messages sent but not synced to client
- Chat packet not sent properly

**Solution:**
Use server-to-client packet for reliable message delivery

### Scenario E: Single Player vs Multiplayer
**Test both:**
- Single player (integrated server)
- Dedicated server

Behavior might differ!

## Quick Fix Options

### Option 1: Force Console Output Only (Temporary)
If chat messaging doesn't work, you can still use console logs.
The timing IS logged to console, so check there.

### Option 2: Send as Command Feedback
Instead of `addChatMessage()`, return timing as command result.
Modify command to `/pw debug stats` to show last timing.

### Option 3: Store in ItemStack NBT
Write timing to wand's NBT, show with `/pw info` command.

### Option 4: Client-Side Rendering
Render timing as HUD overlay (requires client-side code).

## Testing Matrix

| Environment | Debug Command | Pattern Execute | Expected |
|-------------|---------------|-----------------|----------|
| Single Player | `/pw debug on` | Right-click wand | Messages in chat |
| Dedicated Server | `/pw debug on` | Right-click wand | Messages in chat |
| Client connects to server | Server runs command | Player uses wand | ??? |

## What to Report

Please provide:
1. **Environment**: Single player or dedicated server?
2. **Console logs**: All `[PatternWand Debug]` lines
3. **Chat output**: What you see (or don't see)
4. **Blocks placed**: Did the pattern actually work?
5. **Test message**: Did you see "[Debug] Pattern execution started..."?

## Expected Behavior

**When it works:**
1. `/pw debug on` → confirmation message
2. Right-click with wand → "[Debug] Pattern execution started..."
3. Blocks place → timing appears in chat
4. Console shows same timing

**Current behavior:**
1. `/pw debug on` → confirmation message ✓
2. Right-click with wand → blocks place ✓
3. No timing in chat ✗
4. Console shows ??? (need to check)

## Next Steps Based on Diagnostics

1. **If console shows everything**: Chat messaging issue → use packets
2. **If debug=false**: Client/server desync → use server config
3. **If player=null**: Object lifecycle issue → thread-local or context passing
4. **If nothing in console**: Method not called → check code path

Please run the tests and share the console output!
