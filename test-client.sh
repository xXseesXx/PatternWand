#!/bin/bash
# Test client launcher with filtered output

echo "========================================="
echo "Starting PatternWand Test Client"
echo "========================================="
echo ""
echo "Wait for server to fully start before connecting!"
echo ""

# Give server time to start
sleep 5

# Run client with output filtering
./gradlew runClient --console=plain 2>&1 | grep -E "(PatternWand|pattern|Client thread|Connecting to|Connection|Joined)" --line-buffered
