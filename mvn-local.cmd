@echo off
set "MVN=%~dp0.tools\apache-maven-3.9.6\bin\mvn.cmd"
if exist "%MVN%" (
  "%MVN%" %*
) else (
  mvn %*
)
