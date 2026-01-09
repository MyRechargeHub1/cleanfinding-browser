#!/bin/bash
# CleanFinding Browser - GitHub Setup Script
# Run this script to push the browser code to GitHub

echo "🚀 CleanFinding Browser - GitHub Setup"
echo "======================================="

# Initialize git
git init

# Add all files
git add -A

# Commit
git commit -m "Initial release - CleanFinding Browser v1.0.0

Features:
- SafeSearch enforcement on all search engines
- Built-in tracker and ad blocking
- Adult content filtering
- Dark theme UI with tab support
- Keyboard shortcuts (Ctrl+T, Ctrl+W, etc.)
- Cross-platform (Windows, macOS, Linux)"

# Add remote
git remote add origin https://github.com/MyRechargeHub1/cleanfinding-browser.git

# Push to main branch
git branch -M main
git push -u origin main

# Create version tag to trigger build
git tag v1.0.0
git push origin v1.0.0

echo ""
echo "✅ Done! Check GitHub Actions for build progress:"
echo "   https://github.com/MyRechargeHub1/cleanfinding-browser/actions"
