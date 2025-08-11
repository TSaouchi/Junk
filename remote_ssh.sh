#!/bin/bash

CLUSTER1="user@cluster1"
CLUSTER2="user@cluster2"
REMOTE_CMD="myalias"

ssh "$CLUSTER1" bash -c "'
    echo Running alias on Cluster 1
    bash -i -c \"$REMOTE_CMD\" &

    echo Running alias on Cluster 2
    ssh $CLUSTER2 bash -i -c \"$REMOTE_CMD\" &

    wait
'"
