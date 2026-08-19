#!/usr/bin/env bash
# Creates the 16 input files and host directories the docker-compose volumes mount.
set -euo pipefail

mkdir -p data/input data/output-oom data/output-fixed
mkdir -p dumps/cache dumps/processor-oom dumps/processor-fixed

for i in $(seq -f "%02g" 0 15); do
    echo "input payload for file_${i}.dat" > data/input/file_${i}.dat
done

echo "Created $(ls data/input | wc -l | tr -d ' ') input files in data/input/"
echo "  file_00.dat .. file_15.dat"
echo ""
echo "Next steps:"
echo "  # Build images"
echo "  docker compose build"
echo ""
echo "  # Start Redis + cache-service (waits for Redis, then pre-loads 16x20MB into Redis)"
echo "  docker compose up -d redis cache-service"
echo ""
echo "  # Run the OOM demo (expect: java.lang.OutOfMemoryError, heapdump in dumps/processor-oom/)"
echo "  docker compose --profile oom up processor-oom"
echo ""
echo "  # Run the fixed version (semaphore=4, completes without OOM)"
echo "  docker compose --profile fixed up processor-fixed"