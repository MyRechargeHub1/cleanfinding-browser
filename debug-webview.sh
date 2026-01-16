#!/bin/bash

# Debug script for CleanFinding Android Browser
# This will show JavaScript console messages from the WebView

echo "=== CleanFinding Browser Debug ==="
echo "Starting logcat monitoring..."
echo "1. Make sure your Android device is connected via USB"
echo "2. Enable USB debugging on your device"
echo "3. Open CleanFinding Browser app"
echo "4. Try a search query"
echo ""
echo "JavaScript console messages will appear below:"
echo "================================================"
echo ""

adb logcat -c  # Clear old logs
adb logcat -s WebView:D | grep --line-buffered -E "WebView|Search|error|Error|failed|Failed"
