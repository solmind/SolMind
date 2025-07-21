#!/bin/bash

# Test script to verify network connectivity and Hugging Face API access

echo "Testing network connectivity for Hugging Face downloads..."
echo "================================================"

# Test basic connectivity
echo "1. Testing basic internet connectivity..."
ping -c 3 google.com
echo ""

# Test Hugging Face main site
echo "2. Testing Hugging Face main site..."
curl -I https://huggingface.co/ --connect-timeout 10
echo ""

# Test model download URL (FLAN-T5 Small)
echo "3. Testing FLAN-T5 Small model URL..."
curl -I "https://huggingface.co/google/flan-t5-small/resolve/main/pytorch_model.bin" --connect-timeout 10
echo ""

# Test model info API
echo "4. Testing model info API..."
curl -I "https://huggingface.co/api/models/google/flan-t5-small" --connect-timeout 10
echo ""

# Test smaller model (config.json)
echo "5. Testing smaller file download (config.json)..."
curl -I "https://huggingface.co/google/flan-t5-small/resolve/main/config.json" --connect-timeout 10
echo ""

echo "Network test completed!"
echo "If all tests show 200 or 302 responses, the network connectivity is working."
echo "If you see timeouts or connection errors, there may be a network issue."