#!/bin/zsh
# Azure Wallet Build & Run Script for macOS

set -e

echo "=========================================="
echo "  Azure Digital Wallet - Build Script"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Java installation
echo "${YELLOW}Checking Java installation...${NC}"
if ! command -v java &> /dev/null; then
    echo "${RED}❌ Java not found. Please install Java 17 or higher.${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | grep 'version' | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "${RED}❌ Java version is $JAVA_VERSION. Requires Java 17+${NC}"
    exit 1
fi
echo "${GREEN}✓ Java $JAVA_VERSION found${NC}"
echo ""

# Check Maven installation
echo "${YELLOW}Checking Maven installation...${NC}"
if ! command -v mvn &> /dev/null; then
    echo "${RED}❌ Maven not found. Please install Maven 3.8+${NC}"
    exit 1
fi

MVN_VERSION=$(mvn -v | grep 'Apache Maven' | awk '{print $3}')
echo "${GREEN}✓ Maven $MVN_VERSION found${NC}"
echo ""

# Create data directory
echo "${YELLOW}Creating data directory...${NC}"
mkdir -p src/main/resources/data
echo "${GREEN}✓ Data directory ready${NC}"
echo ""

# Build project
echo "${YELLOW}Building project...${NC}"
mvn clean package -DskipTests -q
echo "${GREEN}✓ Build successful${NC}"
echo ""

# Ask user if they want to run
echo "${YELLOW}Build completed successfully!${NC}"
read -p "Would you like to run the application now? (y/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "${YELLOW}Starting Azure Digital Wallet...${NC}"
    echo ""
    mvn javafx:run
else
    echo "${GREEN}To run the application later, use:${NC}"
    echo "  mvn javafx:run"
    echo ""
    echo "Or run the JAR directly:"
    echo "  java -jar target/azure-wallet-app.jar"
fi
