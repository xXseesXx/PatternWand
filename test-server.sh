#!/bin/bash
# Test server launcher with filtered output

echo "========================================="
echo "Starting PatternWand Test Server"
echo "========================================="
echo ""

# Run server with output filtering
./gradlew runServer --console=plain 2>&1 | grep -E "(PatternWand|pattern|Server thread|Listening on|Starting minecraft server|Done \(|eula\.txt)" --line-buffered
