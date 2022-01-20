#!/bin/bash

BASEDIR=$(dirname "$0")
pidPath=$BASEDIR/internal/run.pid

if [ -s "$pidPath" ]; then
  # stop Camunda Run if the process is still running
  kill $(cat $pidPath)

  # remove process ID file
  rm $pidPath

  echo "Camunda Run is shutting down."
else
  echo "There is no instance of Camunda Run to shut down."
  exit 1
fi
