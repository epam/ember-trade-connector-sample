@echo off
title "TimeBase on @devenvTimeBasePort@"
set DELTIX_HOME=@devenvDeltixHome@
set PATH=%DELTIX_HOME%/bin;%DELTIX_HOME%/build/dotnet;%PATH%
:start
set JAVA_OPTS=-verbose:gc -XX:+HeapDumpOnOutOfMemoryError -Xmx@devenvTimeBaseMx@ -Xms@devenvTimeBaseMs@
call "%DELTIX_HOME%/bin/tomcat.cmd" -home "@devenvQsHome@" -tb
if not ERRORLEVEL 0 pause
exit