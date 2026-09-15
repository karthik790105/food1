#!/usr/bin/env bash
set -e

echo "=========================================="
echo "  BiteMart Platform Multi-APK Builder    "
echo "=========================================="

mkdir -p apks

echo "1/4 Building Standalone Admin Portal APK..."
# Admin is currently configured
gradle :app:assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk apks/bitemart-admin.apk
echo "✓ Saved apks/bitemart-admin.apk"

echo "=========================================="
echo "All APKs are located in the /apks/ directory:"
ls -lh apks/
echo "=========================================="
