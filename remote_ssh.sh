# Define clusters and command
$cluster1 = "user@cluster1"
$cluster2 = "user@cluster2"
$remoteCmd = "myalias"

# Start ssh-agent service if not running
$sshAgentStatus = Get-Service ssh-agent -ErrorAction SilentlyContinue
if ($sshAgentStatus.Status -ne 'Running') {
    Start-Service ssh-agent
}

# Add SSH key to agent (adjust path to your private key)
ssh-add $env:USERPROFILE\.ssh\id_ed25519

# Run remote commands on both clusters
ssh $cluster1 "bash -i -c '$remoteCmd'"
ssh $cluster2 "bash -i -c '$remoteCmd'"


#!/bin/bash

# Clusters
CLUSTER1="user@cluster1"
CLUSTER2="user@cluster2"

# The alias or command to run remotely
REMOTE_CMD="myalias"

# Make sure ssh-agent is running
if ! pgrep -u "$USER" ssh-agent > /dev/null; then
    eval "$(ssh-agent -s)"
fi

# Add your ssh key (you’ll enter passphrase once here)
ssh-add ~/.ssh/id_ed25519

# Run commands on both clusters
ssh "$CLUSTER1" "bash -i -c '$REMOTE_CMD'"
ssh "$CLUSTER2" "bash -i -c '$REMOTE_CMD'"
