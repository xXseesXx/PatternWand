# Pattern Wand Command Reference

## Quick Reference

| Command | Description | Example |
|---------|-------------|---------|
| `/patternwand list` | List all loaded patterns | `/patternwand list` |
| `/patternwand set <pattern>` | Set active pattern on wand | `/patternwand set checkerboard` |
| `/patternwand info` | Show wand pattern & seed | `/patternwand info` |
| `/patternwand seed <value>` | Set custom seed | `/patternwand seed 12345` |
| `/patternwand clearseed` | Clear custom seed | `/patternwand clearseed` |
| `/patternwand reload` | Reload all patterns | `/patternwand reload` |

## Detailed Usage

### Set Pattern
```
/patternwand set noise_terrain
```
**Output:**
```
Set active pattern to: noise_terrain
```

### Set Custom Seed
```
/patternwand seed 42424242
```
**Output:**
```
Set pattern seed to: 42424242
This seed will be used for all pattern noise generation
```

**Use Cases:**
- Create pattern variations without editing scripts
- Share reproducible patterns with others
- Generate consistent patterns across worlds

### Clear Seed
```
/patternwand clearseed
```
**Output:**
```
Cleared custom seed
Now using world seed: 8675309
```

**When to use:**
- Return to world-deterministic patterns
- Remove per-wand customization
- Default behavior

### Show Info
```
/patternwand info
```
**Output (with custom seed):**
```
Active pattern: noise_terrain
Custom seed: 42424242
```

**Output (using world seed):**
```
Active pattern: checkerboard
Using world seed: 8675309
```

## Seed Behavior

### Default (No Custom Seed)
- Uses **world seed**
- Patterns are deterministic by world coordinates
- Same position = same pattern, always
- Consistent across the entire world

### With Custom Seed
- Uses **wand-specific seed**
- Patterns are still deterministic by coordinates
- Different wands can have different seeds
- Allows pattern variations

## Pattern Workflow

### Basic Pattern Placement
```bash
# 1. List available patterns
/patternwand list

# 2. Set pattern on held wand
/patternwand set organic_terrain

# 3. Use wand (right-click on blocks)
# Pattern is placed using world seed
```

### Creating Pattern Variations
```bash
# Create 3 wands with same pattern but different seeds
/patternwand set noise_terrain
/patternwand seed 1111    # Wand 1

# Switch to second wand
/patternwand set noise_terrain
/patternwand seed 2222    # Wand 2

# Switch to third wand
/patternwand set noise_terrain
/patternwand seed 3333    # Wand 3

# Now you have 3 variations of the same pattern!
```

### Sharing Reproducible Patterns
```bash
# Share this info with others:
Pattern: organic_terrain
Seed: 42424242
Command: /patternwand seed 42424242

# Others can reproduce your exact pattern:
/patternwand set organic_terrain
/patternwand seed 42424242
```

## Tips & Tricks

### 1. Pattern Testing
```bash
# Test a pattern at multiple locations
/patternwand set test_pattern
# Place at location A
# Break blocks
# Place at location B
# Overlapping coords should match!
```

### 2. Seed Experimentation
```bash
# Try different seeds to find interesting variations
/patternwand seed 1000
# Place pattern, observe
/patternwand seed 2000
# Place pattern, compare
/patternwand seed 3000
# Place pattern, choose favorite
```

### 3. World-Consistent Patterns
```bash
# For large projects spanning multiple sessions
/patternwand clearseed
# Now patterns are always consistent with world coordinates
```

### 4. Per-Structure Customization
```bash
# Different structures can use different seeds
# Structure A
/patternwand seed 1111
# Place walls

# Structure B
/patternwand seed 2222
# Place walls

# Each structure has its own pattern variation
```

## Tab Completion

Commands support tab completion:
- `/patternwand <TAB>` → shows all subcommands
- `/patternwand set <TAB>` → shows all pattern names
- Pattern names auto-complete without `.lua` extension

## Common Questions

**Q: What's the default seed?**
A: The world seed. Use `/patternwand info` to see it.

**Q: Can I use negative seeds?**
A: Yes! `/patternwand seed -12345` works fine.

**Q: Do I need to set seed every time?**
A: No, it's stored in the wand's NBT data and persists.

**Q: Can I copy seed from one wand to another?**
A: Not directly, but you can use `/patternwand info` to see the seed, then `/patternwand seed <value>` on the other wand.

**Q: Will patterns look the same in different worlds?**
A: Only if they have the same world seed, or you use a custom seed.

**Q: How do I get back to random-looking patterns?**
A: The patterns are always deterministic now (which is correct). For variations, use different custom seeds.

## Examples

### Example 1: Simple Checkerboard
```bash
/patternwand set checkerboard
# Right-click on blocks
# Checkerboard pattern appears, deterministic by position
```

### Example 2: Natural Terrain with Variations
```bash
# Variant 1: Mountain biome feel
/patternwand set organic_terrain
/patternwand seed 111111

# Variant 2: Plains biome feel  
/patternwand set organic_terrain
/patternwand seed 222222

# Variant 3: Desert biome feel
/patternwand set organic_terrain
/patternwand seed 333333
```

### Example 3: Consistent Multi-Day Build
```bash
# Day 1
/patternwand set bricks
/patternwand clearseed  # Use world seed
# Build foundation

# Day 2 (different session)
/patternwand set bricks
# Pattern still matches foundation perfectly!
```

## Troubleshooting

### Pattern looks different than expected
- Check seed with `/patternwand info`
- Try `/patternwand clearseed` to use world seed
- Reload patterns with `/patternwand reload`

### Pattern not loading
- Check pattern exists: `/patternwand list`
- Check filename has `.lua` extension
- Check pattern script for errors
- Look in logs for error messages

### Command not working
- Hold Pattern Wand in hand
- Check spelling with tab completion
- Verify you have permission (default: all players)
