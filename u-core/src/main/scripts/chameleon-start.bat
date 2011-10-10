@ECHO OFF
REM =========================================================================
REM 
REM Licensed to the Apache Software Foundation (ASF) under one or more
REM contributor license agreements.  See the NOTICE file distributed with
REM this work for additional information regarding copyright ownership.
REM The ASF licenses this file to You under the Apache License, Version 2.0
REM (the "License"); you may not use this file except in compliance with
REM the License.  You may obtain a copy of the License at
REM 
REM    http://www.apache.org/licenses/LICENSE-2.0
REM 
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM 
REM =========================================================================
@ECHO ON
SetLocal EnableDelayedExpansion
rem Launching OW2 Chameleon Core

rem set the classpath
CP=%CLASSPATH%
CP=!CP!;.\logback.xml

for %%F in (core\*.jar) do (
	set CP=!CP!;%%F%
)

for %%F in (runtime\*.jar) do (
	set CP=!CP!;%%F%
)

for %%F in (application\*.jar) do (
	set CP=!CP!;%%F%
)


start "OW2 Chameleon Core" java -cp %CP% org.ow2.chameleon.core.ucore.Main %1 %2 %3 %4 %5 %6 %7 %8 %9

echo OW2 Chameleon is starting ...
echo Wait few seconds before continuing
pause