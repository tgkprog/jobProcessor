#!/bin/bash
# Install jobproc1 service
SERVICE_NAME="jobproc1"
SERVICE_FILE="${SERVICE_NAME}.service"
SOURCE_DIR=$(dirname "$(readlink -f "$0")")

echo "Installing $SERVICE_NAME service..."
sudo cp "$SOURCE_DIR/$SERVICE_FILE" /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable "$SERVICE_NAME"
echo "Service $SERVICE_NAME installed and enabled."
