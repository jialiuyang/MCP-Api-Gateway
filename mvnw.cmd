@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.2.0
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - location of a JDK home dir
@REM ----------------------------------------------------------------------------
@REM
@REM This wrapper is a thin shim that lets contributors build the project
@REM without having Maven globally installed. It is intentionally minimal;
@REM for the official Maven wrapper run `mvn wrapper:wrapper` after the
@REM first successful build with a globally installed Maven.
@REM ----------------------------------------------------------------------------

@echo off
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
  echo Maven is not on PATH. Please install Apache Maven 3.9 or later.
  exit /b 1
)
mvn %*
