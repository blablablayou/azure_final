#!/bin/zsh
# Azure Wallet Run Script for macOS

set -e

echo "=========================================="
echo "  Azure Digital Wallet - Run Script"
echo "=========================================="
echo ""

# Check if JAR exists
if [ ! -f "target/azure-wallet-app.jar" ]; then
    echo "⚠️  JAR file not found. Building project first..."
    mvn clean package -DskipTests -q
fi

echo "🚀 Starting Azure Digital Wallet..."
echo ""

mvn javafx:run
