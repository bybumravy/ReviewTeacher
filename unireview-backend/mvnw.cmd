@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License meens it is distributed
@REM on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.2.0
@REM
@REM Required ENV vars:
@REM JAVA_HOME - location of a JDK home directory
@REM
@REM Optional ENV vars
@REM MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM MAVEN_BATCH_PAUSE - set to 'on' to wait for a key press, before ending
@REM MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@if "%MAVEN_BATCH_ECHO%" == "on"  echo %MAVEN_BATCH_ECHO%

@setlocal

@rem set %~dp0 is the directory of this script file
@set MAVEN_PROJECTBASEDIR=%~dp0
@if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

@if NOT "%MAVEN_SKIP_RC%" == "" goto skipRcPre
@if exist "%USERPROFILE%\mavenrc_pre.bat" call "%USERPROFILE%\mavenrc_pre.bat"
@if exist "%USERPROFILE%\mavenrc_pre.cmd" call "%USERPROFILE%\mavenrc_pre.cmd"
:skipRcPre

@rem Find java.exe
@if defined JAVA_HOME goto findJavaFromJavaHome

@set JAVA_EXE=java.exe
@for %%I in (%JAVA_EXE%) do set JAVA_HOME=%%~$PATH:I
@if NOT "%JAVA_HOME%" == "" set JAVA_HOME=%JAVA_HOME:\bin\java.exe=%
@if NOT "%JAVA_HOME%" == "" set JAVA_HOME=%JAVA_HOME:\bin=%
@goto stripQuotes

:findJavaFromJavaHome
@set JAVA_HOME=%JAVA_HOME:"=%
@if "%JAVA_HOME:~-13%"=="\bin\java.exe" set JAVA_HOME=%JAVA_HOME:~0,-13%
@if "%JAVA_HOME:~-4%"=="\bin" set JAVA_HOME=%JAVA_HOME:~0,-4%
@set JAVA_EXE=%JAVA_HOME%\bin\java.exe

:stripQuotes
@if not exist "%JAVA_EXE%" (
  echo The JAVA_HOME environment variable is not defined correctly >&2
  echo This environment variable is needed to run this program >&2
  echo NB: JAVA_HOME should point to a JDK not a JRE >&2
  goto error
)

:findWrapperJar
@set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@if exist %WRAPPER_JAR% goto run

@set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@rem Download maven-wrapper.jar if not present
@set DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
@echo Downloading %DOWNLOAD_URL%
@mkdir "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper" 2>NUL
@powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile(%DOWNLOAD_URL%, %WRAPPER_JAR%)"

:run
@set WRAPPER_SHA_256_SUM=""
@set MAVEN_CMD=line

"%JAVA_EXE%" %MAVEN_OPTS% -classpath %WRAPPER_JAR% "-Dmaven.home=%MAVEN_HOME%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
@if ERRORLEVEL 1 goto error
@goto end

:error
@set ERRORLEVEL=1

:end
@if NOT "%MAVEN_SKIP_RC%" == "" goto skipRcPost
@if exist "%USERPROFILE%\mavenrc_post.bat" call "%USERPROFILE%\mavenrc_post.bat"
@if exist "%USERPROFILE%\mavenrc_post.cmd" call "%USERPROFILE%\mavenrc_post.cmd"
:skipRcPost

@if "%MAVEN_BATCH_PAUSE%" == "on" pause

@exit /b %ERRORLEVEL%
