#!/bin/bash

if [[ ! -f SingleTestMethodRunner.java ]]; then
    echo "Missing SingleTestMethodRunner.java"
fi


# ==== Required Inputs ====
TEST_FULL=$1                 # e.g. org.apache.hadoop.hdfs.server.namenode.snapshot.TestOpenFilesWithSnapshot
TEST_METHOD=$2               # e.g. testOpenFileWritingAcrossSnapDeletion
START_VERSION=$3
END_VERSION=$4
MODE=$5
PARTIAL=$6
LOG_FILE=$7
DEBUG_PORT=$8                # Optional: JVM debug port (e.g., 5005)

# ==== Validate Inputs ====
if [[ -z "$TEST_FULL" || -z "$TEST_METHOD" ]]; then
  echo "Usage: $0 <TestClass> <TestMethod> [START_VERSION] [END_VERSION] [MODE] [LOG_FILE] [DEBUG_PORT]"
  exit 1
fi

if [[ ! -f SingleTestMethodRunner.class || ! -f classpath.txt ]]; then
    bash build-single-runner.sh
fi

CP=$(cat classpath.txt):target/classes:target/test-classes

# ==== Run the test method ====
echo "Running test ${TEST_FULL}#${TEST_METHOD}..."

# Get total system memory in MB
TOTAL_MEM_MB=$(free -m | awk '/^Mem:/ {print $2}')

# Use 25% for JVM heap
JVM_MEM_MB=$((TOTAL_MEM_MB / 2))

# JVM memory flags
JVM_HEAP_OPTS="-Xmx${JVM_MEM_MB}m -Xms${JVM_MEM_MB}m"

# GC tuning flags
GC_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=500 -XX:+ParallelRefProcEnabled -XX:+ExplicitGCInvokesConcurrent"

# Debug options (if DEBUG_PORT is provided)
DEBUG_OPTS=""
if [[ -n "$DEBUG_PORT" ]]; then
    # Check if port is available, if not try next port
    PORT_TO_USE=$DEBUG_PORT
    while netstat -tuln 2>/dev/null | grep -q ":$PORT_TO_USE "; do
        echo "Port $PORT_TO_USE is in use, trying next port..."
        PORT_TO_USE=$((PORT_TO_USE + 1))
    done
    
    if [[ $PORT_TO_USE -ne $DEBUG_PORT ]]; then
        echo "Using debug port $PORT_TO_USE instead of $DEBUG_PORT"
    fi
    
    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=0.0.0.0:$PORT_TO_USE"
fi

# Combine all JVM options
JAVA_OPTS="$JVM_HEAP_OPTS $GC_OPTS $DEBUG_OPTS"

echo timeout 10m java $JAVA_OPTS \
  -cp "$CP:." \
  -Dupgt.start.version="$START_VERSION" \
  -Dupgt.upgrade.version="$END_VERSION" \
  -Dupgt.datanode.${MODE}=true \
  -Dupgt.namenode.${MODE}=true \
  -Dupgt.upgrade.${PARTIAL}=true \
  SingleTestMethodRunner "$TEST_FULL" "$TEST_METHOD" |& tee "$LOG_FILE"


# Run the test and capture exit code
timeout 10m java $JAVA_OPTS \
  -cp "$CP:." \
  -Dupgt.start.version="$START_VERSION" \
  -Dupgt.upgrade.version="$END_VERSION" \
  -Dupgt.datanode.${MODE}=true \
  -Dupgt.namenode.${MODE}=true \
  -Dupgt.upgrade.${PARTIAL}=true \
  SingleTestMethodRunner "$TEST_FULL" "$TEST_METHOD" |& tee "$LOG_FILE"


exit_code=${PIPESTATUS[0]}  # Capture the actual exit code of `java`

if [ $exit_code -eq 124 ]; then
    echo "[UPGT-TEST-RESULT] Test Timed Out" >> "$LOG_FILE"
elif [ $exit_code -ne 0 ]; then
    echo "[UPGT-TEST-RESULT] Test Failed with code $exit_code" >> "$LOG_FILE"
fi
