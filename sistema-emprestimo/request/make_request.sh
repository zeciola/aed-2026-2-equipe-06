#!/usr/bin/env bash

usage() {
  echo "Usage: $0 <request_file>"
  echo
  echo "Arguments:"
  echo "  request_file    JSON file containing the request body"
  echo
  echo "Example:"
  echo "  $0 request.json"
}

REQUEST_FILE=$1

if [ -z "$REQUEST_FILE" ]; then
  echo "Usage: $0 <request_file>"
  exit 1
fi

if [ ! -f "$REQUEST_FILE" ]; then
  echo "Request file not found: $REQUEST_FILE"
  exit 1
fi

response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
    -X POST http://localhost:8080/emprestimos/solicitar \
    -H "Content-Type: application/json" \
    -d @"$REQUEST_FILE")

echo "$response"

