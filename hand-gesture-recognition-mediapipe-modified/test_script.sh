#!/bin/bash


# URL of the Flask app
URL="http://127.0.0.1:8080/classify"

# Path to the test image
IMAGE_PATH="testCase.png"

# Send POST request with the image
curl -X POST "$URL" -F "image=@$IMAGE_PATH"
