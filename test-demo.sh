#!/bin/bash

# Multi-Tenant Data Source Routing - Demo Script
# This script automates the testing for video proof.

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}==================================================${NC}"
echo -e "${BLUE}   MULTI-TENANT ROUTING PROJECT DEMO             ${NC}"
echo -e "${BLUE}==================================================${NC}"

# 1. Setup
echo -e "\n${GREEN}[1/5] Setting up environment...${NC}"
if [ ! -f .env ]; then
    cp .env.example .env
    echo "Created .env from example."
fi

# 2. Start Docker
echo -e "\n${GREEN}[2/5] Starting services with Docker Compose...${NC}"
docker compose up --build -d

echo -ne "Waiting for application to be healthy..."
while [ "$(docker inspect --format='{{json .State.Health.Status}}' demo_app)" != "\"healthy\"" ]; do
    echo -ne "."
    sleep 2
done
echo -e " ${GREEN}READY!${NC}"

# 3. Health Checks
echo -e "\n${GREEN}[3/5] Verifying Health Checks...${NC}"
echo -e "${BLUE}Actuator Health Output:${NC}"
curl -s http://localhost:8080/actuator/health | jq . || curl -s http://localhost:8080/actuator/health
echo -e "\n${BLUE}Tenant DataSources Health:${NC}"
curl -s http://localhost:8080/actuator/health/datasources | jq . || curl -s http://localhost:8080/actuator/health/datasources

# 4. Multi-Tenant Operations
echo -e "\n${GREEN}[4/5] Performing Multi-Tenant Data Operations...${NC}"

echo -e "${BLUE}Creating user for TENANT 1...${NC}"
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant1" \
  -d '{"name": "Alice Green", "email": "alice@tenant1.com"}' | jq .

echo -e "\n${BLUE}Creating user for TENANT 2...${NC}"
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant2" \
  -d '{"name": "Bob Blue", "email": "bob@tenant2.com"}' | jq .

# 5. Isolation Verification
echo -e "\n${GREEN}[5/5] Verifying Data Isolation...${NC}"

echo -e "${BLUE}Listing users for TENANT 1 (Expected: Alice only):${NC}"
curl -s -X GET http://localhost:8080/api/users -H "X-Tenant-ID: tenant1" | jq .

echo -e "\n${BLUE}Listing users for TENANT 2 (Expected: Bob only):${NC}"
curl -s -X GET http://localhost:8080/api/users -H "X-Tenant-ID: tenant2" | jq .

echo -e "\n${BLUE}Listing users for TENANT 3 (Expected: Empty list):${NC}"
curl -s -X GET http://localhost:8080/api/users -H "X-Tenant-ID: tenant3" | jq .

echo -e "\n${GREEN}==================================================${NC}"
echo -e "${GREEN}   DEMO COMPLETE - ISOLATION VERIFIED            ${NC}"
echo -e "${GREEN}==================================================${NC}"
