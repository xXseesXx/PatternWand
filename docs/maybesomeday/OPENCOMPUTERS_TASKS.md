# OpenComputers Integration - Implementation Tasks

## Task Breakdown

### Phase 1: Foundation & Setup ⏱️ 2-3 hours

#### Task 1.1: Add OpenComputers Dependency
- [x] Research GTNH OpenComputers version
- [ ] Add OC to dependencies.gradle (optional, devOnlyNonPublishable)
- [ ] Verify OC API imports work
- [ ] Test build with and without OC present

**Files to modify:**
- `dependencies.gradle`

**Acceptance criteria:**
- Mod builds successfully with OC in dev environment
- Mod builds successfully without OC (optional dep)
- No runtime crashes when OC is missing

---

#### Task 1.2: Create OCIntegration Module
- [ ] Create `com.xXseesXx.patternwand.integration` package
- [ ] Create `OCIntegration.java` with soft dependency check
- [ ] Implement `@Optional.Method` annotations
- [ ] Register integration in mod initialization

**Files to create:**
- `src/main/java/com/xXseesXx/patternwand/integration/OCIntegration.java`
- `src/main/java/com/xXseesXx/patternwand/integration/oc/package-info.java`

**Acceptance criteria:**
- Integration only loads when OC is present
- No class loading issues when OC is missing
- Logged message confirms OC integration status

---

#### Task 1.3: Component Registration Framework
- [ ] Study OC component API and Environment interface
- [ ] Create base component class structure
- [ ] Implement component registration in OC's Network
- [ ] Add component lifecycle management

**Files to create:**
- `src/main/java/com/xXseesXx/patternwand/integration/oc/ComponentBase.java`
- `src/main/java/com/xXseesXx/patternwand/integration/oc/ComponentPatternManager.java`

**Acceptance criteria:**
- Component appears in OC `component.list()`
- Component is accessible via `component.patternwand_manager`
- No memory leaks on component add/remove

---

### Phase 2: Pattern Management Component ⏱️ 4-5 hours

#### Task 2.1: Pattern Listing
- [ ] Implement `listPatterns()` callback
- [ ] Extract pattern metadata (name, author, parameters)
- [ ] Format data for OC Lua tables
- [ ] Add energy cost calculation

**Files to modify:**
- `ComponentPatternManager.java`
- `PatternScriptLoader.java` (if needed for metadata access)

**Test cases:**
- Empty pattern directory
- Patterns with/without metadata
- Invalid patterns (should be filtered)

---

#### Task 2.2: Pattern Information Retrieval
- [ ] Implement `getPatternInfo(name)` callback
- [ ] Return full metadata including parameters
- [ ] Include parameter types, defaults, min/max
- [ ] Add pattern source code retrieval (optional)

**Files to modify:**
- `ComponentPatternManager.java`

**Test cases:**
- Valid pattern name
- Invalid pattern name
- Pattern with all metadata fields
- Pattern with minimal metadata

---

#### Task 2.3: Pattern Reload
- [ ] Implement `reload()` callback
- [ ] Trigger PatternScriptLoader.reload()
- [ ] Return success/failure with error messages
- [ ] Add energy cost

**Files to modify:**
- `ComponentPatternManager.java`
- `PatternScriptLoader.java` (add reload errors return)

**Test cases:**
- Successful reload
- Reload with compilation errors
- Concurrent reload attempts

---

#### Task 2.4: Pattern Validation
- [ ] Implement `validatePattern(luaCode)` callback
- [ ] Use ScriptEngine.compile() without saving
- [ ] Return detailed error messages
- [ ] Add energy cost for compilation

**Files to modify:**
- `ComponentPatternManager.java`

**Test cases:**
- Valid pattern code
- Syntax errors
- Runtime errors (missing return)
- Invalid metadata format

---

### Phase 3: File Operations ⏱️ 3-4 hours

#### Task 3.1: Sandboxed File Access
- [ ] Create FileAccessManager for pattern directory
- [ ] Implement path validation (prevent directory traversal)
- [ ] Add whitelist/blacklist for protected patterns
- [ ] Implement file size limits

**Files to create:**
- `src/main/java/com/xXseesXx/patternwand/integration/oc/FileAccessManager.java`

**Security tests:**
- Reject paths with `..`
- Reject absolute paths
- Reject paths outside patterns directory
- Allow only `.lua` files

---

#### Task 3.2: Pattern Writing
- [ ] Implement `writePattern(name, luaCode)` callback
- [ ] Validate code before writing
- [ ] Create backup of existing pattern (if overwrite)
- [ ] Add config option to disable (security)

**Files to modify:**
- `ComponentPatternManager.java`
- `Config.java` (add OC file write permission)

**Test cases:**
- Write new pattern
- Overwrite existing user pattern
- Attempt to overwrite example pattern (reject)
- Invalid pattern code (reject before write)

---

#### Task 3.3: Pattern Deletion
- [ ] Implement `deletePattern(name)` callback
- [ ] Only allow deletion of user patterns
- [ ] Move to trash instead of permanent delete (optional)
- [ ] Return success/error messages

**Files to modify:**
- `ComponentPatternManager.java`

**Test cases:**
- Delete user pattern
- Attempt to delete example pattern (reject)
- Delete non-existent pattern

---

### Phase 4: Preview System ⏱️ 6-8 hours

#### Task 4.1: Preview Generator Core
- [ ] Create PreviewGenerator class
- [ ] Implement virtual pattern execution (no world)
- [ ] Store results in 3D array (palette indices)
- [ ] Add memory management and cleanup

**Files to create:**
- `src/main/java/com/xXseesXx/patternwand/integration/oc/PreviewGenerator.java`
- `src/main/java/com/xXseesXx/patternwand/integration/oc/PreviewContext.java`

**Technical details:**
- Use `byte[][][]` for compact storage (palette 0-26)
- Implement virtual palette (no real blocks needed)
- Add timeout protection (reuse ScriptEngine timeout)

---

#### Task 4.2: Color Mapping System
- [ ] Create block color extraction
- [ ] Implement RGB color calculation
- [ ] Add color quantization for OC palette
- [ ] Cache colors for performance

**Files to create:**
- `src/main/java/com/xXseesXx/patternwand/integration/oc/ColorMapper.java`

**Technical details:**
- Use Minecraft's MapColor system
- Support custom color overrides via config
- Fallback to neutral color for unknown blocks

---

#### Task 4.3: Preview Component API
- [ ] Implement `createPreview()` callback
- [ ] Return preview handle with methods
- [ ] Implement `getBlock(x,y,z)` callback
- [ ] Implement `getPaletteColor(index)` callback
- [ ] Add `close()` for cleanup
- [ ] Enforce volume limits

**Files to modify:**
- `ComponentPatternManager.java`
- `PreviewGenerator.java`

**Test cases:**
- Small preview (8x8x8)
- Large preview (64x64x64)
- Exceed volume limit (reject)
- Multiple concurrent previews
- Preview after component disconnect

---

#### Task 4.4: Preview Optimization
- [ ] Lazy evaluation (compute on demand)
- [ ] Chunk-based caching
- [ ] Memory pooling for reuse
- [ ] Progress tracking for large previews

**Files to modify:**
- `PreviewGenerator.java`

**Performance targets:**
- 16x16x16 preview: < 500ms
- 64x64x64 preview: < 5s
- Memory: < 10MB per preview

---

### Phase 5: Wand Control Component ⏱️ 4-5 hours

#### Task 5.1: Wand NBT Access
- [ ] Create ComponentPatternWandController
- [ ] Implement wand detection (inventory/adjacent)
- [ ] Read wand NBT data
- [ ] Write wand NBT data safely

**Files to create:**
- `src/main/java/com/xXseesXx/patternwand/integration/oc/ComponentPatternWandController.java`

**Technical details:**
- Search computer inventory slots
- Check adjacent inventories (chests, etc.)
- Validate wand item type
- Sync NBT changes to client

---

#### Task 5.2: Wand State Queries
- [ ] Implement `getWandInfo()` callback
- [ ] Return active pattern, seed, blocks, palette
- [ ] Format palette as Lua table
- [ ] Add energy cost

**Files to modify:**
- `ComponentPatternWandController.java`

**Test cases:**
- Wand in computer inventory
- Wand in adjacent inventory
- No wand present
- Multiple wands present

---

#### Task 5.3: Pattern Selection
- [ ] Implement `setPattern(name, parameters)` callback
- [ ] Validate pattern exists
- [ ] Validate parameters against metadata
- [ ] Update wand NBT
- [ ] Add energy cost

**Files to modify:**
- `ComponentPatternWandController.java`
- `ItemPatternWandUnbreakable.java` (may need NBT helper methods)

**Test cases:**
- Valid pattern with parameters
- Valid pattern without parameters
- Invalid pattern name
- Invalid parameter values
- Parameter type validation

---

#### Task 5.4: Seed Control
- [ ] Implement `setSeed(seed)` callback
- [ ] Implement `clearSeed()` callback
- [ ] Update wand NBT
- [ ] Add energy cost

**Files to modify:**
- `ComponentPatternWandController.java`

**Test cases:**
- Set valid seed
- Set negative seed
- Clear seed
- Get seed value

---

### Phase 6: Configuration & Polish ⏱️ 2-3 hours

#### Task 6.1: Configuration Options
- [ ] Add OC integration toggle
- [ ] Add file write permission
- [ ] Add preview permission
- [ ] Add max preview volume limit
- [ ] Add energy cost multipliers

**Files to modify:**
- `Config.java`

**Config structure:**
```java
// OpenComputers Integration
public static boolean ocIntegrationEnabled = true;
public static boolean ocAllowFileWrite = true;
public static boolean ocAllowPreview = true;
public static int ocMaxPreviewVolume = 262144; // 64³
public static double ocEnergyCostMultiplier = 1.0;
```

---

#### Task 6.2: Error Handling & Logging
- [ ] Add comprehensive error messages
- [ ] Log component lifecycle events
- [ ] Add debug logging for development
- [ ] Implement OC error reporting (callbacks throw)

**Files to modify:**
- All component files

**Error categories:**
- Security errors (file access denied)
- Validation errors (invalid input)
- Resource errors (out of memory)
- State errors (no wand found)

---

#### Task 6.3: Energy System
- [ ] Implement energy consumption for operations
- [ ] Add energy cost calculations
- [ ] Reject operations if insufficient energy
- [ ] Document energy costs

**Files to modify:**
- All component files

**Energy costs (defaults):**
- listPatterns: 50
- getPatternInfo: 100
- reload: 500
- validatePattern: 200
- writePattern: 100
- deletePattern: 50
- createPreview: 1000 + (volume / 1000)
- getWandInfo: 50
- setPattern: 100
- setSeed: 50

---

### Phase 7: Testing & Documentation ⏱️ 4-5 hours

#### Task 7.1: Unit Tests
- [ ] Test pattern listing with various scenarios
- [ ] Test file operations and security
- [ ] Test preview generation accuracy
- [ ] Test wand NBT access
- [ ] Test parameter validation

**Files to create:**
- `src/test/java/com/xXseesXx/patternwand/integration/OCIntegrationTest.java`
- `src/test/java/com/xXseesXx/patternwand/integration/PreviewGeneratorTest.java`

---

#### Task 7.2: Integration Tests
- [ ] Test with actual OC computers
- [ ] Test robot interaction
- [ ] Test multi-player scenarios
- [ ] Test performance with large patterns

**Test scenarios:**
- Pattern generation workflow
- Preview rendering on screen
- Robot using wand
- Concurrent access by multiple computers

---

#### Task 7.3: Example Programs
- [ ] Create pattern_generator.lua
- [ ] Create pattern_browser.lua
- [ ] Create preview_renderer.lua
- [ ] Create robot_builder.lua

**Files to create:**
- `docs/opencomputers/examples/pattern_generator.lua`
- `docs/opencomputers/examples/pattern_browser.lua`
- `docs/opencomputers/examples/preview_renderer.lua`
- `docs/opencomputers/examples/robot_builder.lua`

---

#### Task 7.4: Documentation
- [ ] Write API reference for each component
- [ ] Create tutorial: "Getting Started with OC Integration"
- [ ] Document security considerations
- [ ] Add troubleshooting guide

**Files to create:**
- `docs/opencomputers/API_REFERENCE.md`
- `docs/opencomputers/TUTORIAL.md`
- `docs/opencomputers/SECURITY.md`
- Update `README.md` with OC section

---

## Total Estimated Time: 25-35 hours

## Priority Implementation Order

1. **Phase 1** (Foundation) - Required for everything else
2. **Phase 2** (Pattern Management) - High value, relatively simple
3. **Phase 4** (Preview System) - Core feature, most complex
4. **Phase 5** (Wand Control) - Nice to have, extends functionality
5. **Phase 3** (File Operations) - Optional, security concerns
6. **Phase 6** (Polish) - Throughout development
7. **Phase 7** (Testing/Docs) - Throughout development

## Milestone Checklist

- [ ] **Milestone 1:** OC components appear in component.list()
- [ ] **Milestone 2:** Can list patterns from OC computer
- [ ] **Milestone 3:** Can generate and preview simple pattern
- [ ] **Milestone 4:** Can control wand from OC computer
- [ ] **Milestone 5:** Full API implemented and documented
- [ ] **Milestone 6:** Example programs working
- [ ] **Milestone 7:** Ready for release

## Dependencies

### Required
- OpenComputers (GTNH version) - dev dependency
- LuaJ 3.0.1 (already present)

### Optional
- Hologram projector integration (future enhancement)

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| OC API breaking changes | Low | High | Pin to stable GTNH version |
| Performance issues (large previews) | Medium | Medium | Implement chunking, limits |
| Memory leaks in components | Medium | High | Proper cleanup, lifecycle mgmt |
| Security vulnerabilities | Low | High | Comprehensive sandboxing |
| Complex error handling | High | Low | Good logging, clear errors |

## Success Criteria

1. All components register and function correctly
2. No crashes when OC is missing (soft dependency)
3. Preview generation matches actual placement
4. File operations are secure (no directory traversal)
5. Energy costs are balanced
6. Documentation is clear and complete
7. Example programs demonstrate key features

## Notes

- Keep implementation modular for easy maintenance
- Use @Optional.Method for all OC-specific code
- Test with and without OC present
- Consider future ComputerCraft integration (similar API)
