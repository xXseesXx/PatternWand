# Pattern Execution Timing Feature

## Overview
Automatic execution timing tracking has been added to debug mode. When debug mode is enabled with `/patternwand debug on`, the mod now automatically tracks and reports pattern execution performance.

## What's Tracked

- **Total time**: Complete time from start to finish of pattern placement (in milliseconds)
- **Pattern execution time**: Time spent executing pattern logic only (in milliseconds)
- **Per-block time**: Average time per block in nanoseconds for detailed performance analysis
- **Block count**: Total number of blocks processed

## Example Output

```
[PatternWand Debug] Pattern execution complete: 127 blocks in 45.234 ms (38.567 ms pattern time, 303.7 ns/block)
```

This shows:
- 127 blocks were placed
- Total operation took 45.234 ms
- Pure pattern execution took 38.567 ms (the rest is block placement overhead)
- Average of 303.7 nanoseconds per block for pattern logic

## Implementation Details

### Modified Files

1. **DebugAPI.java** - Added timing infrastructure:
   - `startPatternTiming()` - Starts timing when pattern placement begins
   - `recordBlockExecution(long)` - Records time for each block
   - `finishPatternTiming()` - Calculates and prints summary
   - Static fields to track timing state

2. **ScriptEngine.java** - Per-block timing:
   - Captures start time before pattern execution
   - Records execution time after completion
   - Zero overhead when debug mode is disabled (check happens first)

3. **PatternWandWorker.java** - Overall timing:
   - Calls `startPatternTiming()` at start of pattern placement
   - Calls `finishPatternTiming()` at end to print summary

4. **PatternWandCommand.java** - Updated debug command:
   - Informs users that timing will be tracked when enabling debug mode

## Performance Impact

- **When debug disabled**: Zero overhead (timing checks are skipped)
- **When debug enabled**: Minimal overhead (~100-200 ns per block for timing calls)
- Uses `System.nanoTime()` for high-precision measurements

## Usage

```bash
# Enable debug mode (timing starts automatically)
/patternwand debug on

# Use your pattern wand normally
# Click to place blocks with active pattern

# Check console output for timing summary
# Timing info appears after each pattern placement

# Disable debug mode when done
/patternwand debug off
```

## Use Cases

- **Performance optimization**: Identify slow patterns
- **Pattern development**: See execution characteristics
- **Troubleshooting**: Detect performance issues
- **Benchmarking**: Compare different pattern approaches
