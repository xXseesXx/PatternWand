# OpenComputers Integration - Implementation Summary

**Date:** 2026-08-08  
**Status:** Design Complete, Implementation Pending  
**Target Version:** PatternWand 1.1.0

## Executive Summary

OpenComputers integration enables programmatic pattern generation, preview, and management through OC computers and robots. This creates a powerful workflow for advanced users while maintaining PatternWand's instant-use simplicity for casual players.

## Design Decisions

### ✅ What We're Implementing

1. **Pattern Management Component** (`patternwand_manager`)
   - List, query, and reload patterns
   - Validate pattern code
   - Write/delete user patterns (sandboxed)
   - Generate pattern previews (virtual execution)

2. **Wand Control Component** (`patternwand_controller`)
   - Query wand state (pattern, seed, blocks)
   - Set active pattern with parameters
   - Control seed value
   - Access palette information

3. **Preview System**
   - Virtual pattern execution (no world modification)
   - Block color mapping for visualization
   - 3D data structure for OC programs to render
   - Resource limits to prevent abuse

4. **Security & Safety**
   - Sandboxed file access (patterns directory only)
   - Example pattern protection
   - Energy costs for all operations
   - Configuration options for server operators

### ❌ What We're NOT Implementing

1. **Direct Block Placement from OC**
   - Would bypass game mechanics
   - Could enable griefing
   - Conflicts with survival gameplay

2. **Pattern Execution Engine Replacement**
   - Current LuaJ system is optimal for use case
   - OC's persistent execution would add overhead
   - No performance benefit

3. **Real-time World Interaction**
   - OC programs can't monitor block placement
   - No events for pattern usage
   - Read-only world interaction

## Architecture Overview

```
┌───────────────────────────────────────────────────────────┐
│                  OpenComputers Computer                   │
│                                                           │
│  User Lua Program                                         │
│  └─> component.patternwand_manager                       │
│      └─> component.patternwand_controller                │
└────────────────────┬──────────────────────────────────────┘
                     │ (Component API)
                     ▼
┌───────────────────────────────────────────────────────────┐
│              PatternWand Mod (if OC present)              │
│                                                           │
│  OCIntegration.java (@Optional.Method)                   │
│  ├─> ComponentPatternManager                             │
│  │   ├─> PatternScriptLoader (list, reload)             │
│  │   ├─> ScriptEngine (validate, compile)               │
│  │   ├─> PreviewGenerator (virtual execution)           │
│  │   └─> FileAccessManager (secure file ops)            │
│  └─> ComponentPatternWandController                      │
│      ├─> ItemPatternWandUnbreakable (NBT access)        │
│      └─> PatternPalette (palette info)                  │
└───────────────────────────────────────────────────────────┘
```

## Implementation Phases

### Phase 1: Foundation (2-3 hours) ✅ PLANNED
- Add OC dependency (dev-only, optional)
- Create OCIntegration module with soft dependency
- Implement component registration framework

### Phase 2: Pattern Management (4-5 hours) 📋 TODO
- List patterns with metadata
- Get pattern details
- Reload patterns
- Validate pattern code

### Phase 3: File Operations (3-4 hours) 📋 TODO
- Sandboxed file access
- Write patterns
- Delete patterns
- Security validation

### Phase 4: Preview System (6-8 hours) 📋 TODO
- Virtual pattern execution
- Block color mapping
- Preview API
- Optimization & caching

### Phase 5: Wand Control (4-5 hours) 📋 TODO
- Wand NBT access
- State queries
- Pattern selection
- Seed control

### Phase 6: Configuration & Polish (2-3 hours) 📋 TODO
- Config options
- Error handling
- Energy system
- Logging

### Phase 7: Testing & Documentation (4-5 hours) 📋 TODO
- Unit tests
- Integration tests
- Example programs
- Documentation

**Total Estimated Time:** 25-35 hours

## Use Case Examples

### Use Case 1: Parametric Pattern Generator
**Actor:** Technical player  
**Goal:** Generate mathematical patterns programmatically

```lua
-- OC program generates pattern based on formula
local pm = require("component").patternwand_manager

local code = [[
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    local dist = util.distance(relX, relZ, 0, 0)
    local angle = util.atan2(relX, relZ)
    local value = math.sin(dist * 0.5 + angle * 3)
    return value > 0 and 0 or 1
end
return pattern
]]

pm.writePattern("spiral_wave", code)
pm.reload()
```

**Benefit:** Complex patterns without manual Lua file editing

---

### Use Case 2: Pattern Preview Before Building
**Actor:** Creative builder  
**Goal:** Visualize pattern before committing blocks

```lua
-- Preview on OC screen before using wand
local pm = require("component").patternwand_manager
local gpu = require("component").gpu

local preview = pm.createPreview("noise_terrain", 32, 32, 1)

-- Render 2D slice
for y = 0, 31 do
    for x = 0, 31 do
        local idx = preview.getBlock(x, y, 0)
        if idx >= 0 then
            gpu.setBackground(preview.getPaletteColor(idx))
            gpu.set(x + 1, y + 1, " ")
        end
    end
end

preview.close()
-- Decision: looks good, use wand. Or tweak parameters and preview again
```

**Benefit:** No wasted blocks, instant feedback on pattern appearance

---

### Use Case 3: Robot Automation
**Actor:** Advanced player  
**Goal:** Automate large-scale building projects

```lua
-- Robot with pattern wand builds automatically
local robot = require("robot")
local pw = require("component").patternwand_controller

-- Set pattern for wall section
pw.setPattern("bricks", {brickWidth=4, weathered=false})

-- Build 10 wall segments
for i = 1, 10 do
    robot.forward()
    robot.use()  -- Uses pattern wand
    if i % 5 == 0 then
        -- Randomize appearance mid-build
        pw.setSeed(os.time() + i)
    end
end
```

**Benefit:** Scale up building projects, creative automation

---

### Use Case 4: Pattern Library Management
**Actor:** Server admin or content creator  
**Goal:** Manage large pattern collections

```lua
-- Pattern browser with search and filtering
local pm = require("component").patternwand_manager

local patterns = pm.listPatterns()

-- Filter by author
local function findByAuthor(author)
    local results = {}
    for _, p in ipairs(patterns) do
        if p.metadata.author == author then
            table.insert(results, p)
        end
    end
    return results
end

local myPatterns = findByAuthor("BuilderBot")
print("Found " .. #myPatterns .. " patterns by BuilderBot")

-- Bulk operations
for _, p in ipairs(myPatterns) do
    print("Validating: " .. p.name)
    local ok, err = pm.validatePattern(p.source)
    if not ok then
        print("  ERROR: " .. err)
    end
end
```

**Benefit:** Organize and maintain large pattern libraries

## Configuration

```java
// In PatternWand config file
[opencomputers]
    # Enable OpenComputers integration
    enabled = true
    
    # Allow OC programs to write pattern files
    allowFileWrite = true
    
    # Allow pattern preview generation
    allowPreview = true
    
    # Maximum preview volume (width * height * depth)
    maxPreviewVolume = 262144  // 64³
    
    # Energy cost multiplier (1.0 = default)
    energyCostMultiplier = 1.0
```

## Security Considerations

### ✅ Protected
- File access limited to `config/patternwand/patterns/` only
- Example patterns cannot be modified or deleted
- Path traversal attacks prevented (no `..` in paths)
- Resource limits enforced (preview size, energy costs)
- Pattern validation before execution

### ⚠️ Known Limitations
- OC programs can still create patterns that lag servers (complex noise)
- Energy costs may need tuning based on server performance
- Preview generation is CPU-intensive for large volumes

### 🔒 Server Operator Controls
- `allowFileWrite` - disable pattern creation if needed
- `allowPreview` - disable preview generation if too resource-intensive
- `maxPreviewVolume` - limit preview size
- `energyCostMultiplier` - increase costs to discourage abuse

## Dependencies

### Runtime (Optional)
- OpenComputers (GTNH version 1.10.21 or later)
- **No hard dependency** - mod works without OC installed

### Development
```gradle
dependencies {
    // OC for dev environment
    devOnlyNonPublishable("com.github.GTNewHorizons:OpenComputers:1.10.21-GTNH:dev")
    compileOnlyApi("com.github.GTNewHorizons:OpenComputers:1.10.21-GTNH:dev")
}
```

### Build Time
- Uses `@Optional.Method` annotations (Forge)
- No OC classes loaded if mod not present

## Testing Strategy

### Unit Tests
- Pattern listing with various metadata formats
- File access security (path validation)
- Preview generation accuracy
- Parameter validation
- NBT read/write operations

### Integration Tests
- Component registration in OC network
- Energy consumption verification
- Multi-player concurrent access
- Memory leak detection
- Performance benchmarks (preview generation)

### Manual Tests
- Test with OC computers (T1, T2, T3)
- Test with OC robots
- Test without OC installed (soft dep)
- Test in multiplayer
- Test with large pattern libraries

## Performance Targets

| Operation | Target Time | Max Memory |
|-----------|-------------|------------|
| listPatterns() | < 100ms | 1 MB |
| validatePattern() | < 500ms | 5 MB |
| createPreview(16³) | < 500ms | 5 MB |
| createPreview(64³) | < 5s | 50 MB |
| preview.getBlock() | < 1ms | 0 |

## Documentation Deliverables

### Technical Documentation
- [x] OPENCOMPUTERS_INTEGRATION.md (design doc)
- [x] OPENCOMPUTERS_TASKS.md (implementation tasks)
- [x] opencomputers/QUICK_REFERENCE.md (API reference)
- [ ] opencomputers/API_REFERENCE.md (detailed API)
- [ ] opencomputers/TUTORIAL.md (getting started)
- [ ] opencomputers/SECURITY.md (security details)

### Example Programs
- [ ] pattern_generator.lua (generate patterns)
- [ ] pattern_browser.lua (GUI library browser)
- [ ] preview_renderer.lua (2D/3D visualization)
- [ ] robot_builder.lua (automated building)

### User Documentation
- [ ] Update main README.md with OC section
- [ ] Create in-game manual page (if possible)
- [ ] Video tutorial (optional)

## Success Criteria

### Must Have ✅
- [ ] All components register and appear in component.list()
- [ ] Pattern listing works with metadata
- [ ] Preview generation matches actual patterns
- [ ] Wand control (set pattern, seed) works
- [ ] No crashes when OC is not installed
- [ ] File operations are secure
- [ ] Documentation is complete

### Should Have 🎯
- [ ] All example programs work
- [ ] Energy costs are balanced
- [ ] Performance meets targets
- [ ] Error messages are clear
- [ ] Unit tests pass

### Nice to Have 🌟
- [ ] Hologram integration
- [ ] Color mapping optimization
- [ ] Pattern sharing system
- [ ] ComputerCraft support (future)

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| OC API changes | Pin to stable GTNH version, use @Optional |
| Performance issues | Implement limits, chunking, caching |
| Memory leaks | Proper cleanup, lifecycle management |
| Security exploits | Comprehensive sandboxing, validation |
| Complexity creep | Stick to design, avoid feature bloat |

## Future Enhancements

### Version 1.1 (Current Scope)
- Basic OC integration
- Pattern management
- Preview system
- Wand control

### Version 1.2 (Future)
- Hologram projector integration
- Pattern sharing network
- Advanced preview modes (cross-section, wireframe)
- Pattern templates library

### Version 1.3 (Future)
- ComputerCraft support (similar API)
- Pattern compiler optimization
- Real-time collaboration (multi-player preview)

## Changelog

### [Unreleased]
- Added OpenComputers optional dependency
- Created design documentation
- Created implementation task breakdown
- Added quick reference guide

## Contributors

- **Design:** Kiro AI (2026-08-08)
- **Implementation:** TBD

## License

LGPL-3.0 (same as PatternWand main project)

OpenComputers is MIT licensed.

---

**Next Steps:**
1. Review and approve design
2. Begin Phase 1 implementation (Foundation)
3. Set up testing environment with OC
4. Implement incrementally, test each phase
5. Document as we go
6. Release as beta for community feedback

**Questions/Feedback:** Open issue on GitHub or discuss in project channels
