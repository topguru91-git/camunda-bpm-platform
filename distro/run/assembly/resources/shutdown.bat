@echo off

REM set environment parameters
SET appName="Camunda Run"

REM shut down Camunda Run
ECHO Camunda Run is shutting down.
TASKKILL /FI "WINDOWTITLE eq %appName%"