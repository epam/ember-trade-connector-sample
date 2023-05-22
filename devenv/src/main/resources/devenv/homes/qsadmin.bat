@echo off

call setup-env.bat
set DELTIX_HOME=%DELTIX_HOME_52%

start /d "%DELTIX_HOME_52%\bin" qsadmin.cmd -home "%ANVIL_HOME%\qshome"
exit 0