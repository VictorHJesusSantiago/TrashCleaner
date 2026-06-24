@echo off
rem ================================================================
rem  TrashCleaner - Launcher com elevacao de privilegios automatica
rem  Uso: run.bat               (menu interativo)
rem       run.bat --quick       (limpeza rapida)
rem       run.bat --deep        (limpeza profunda)
rem       run.bat --all         (limpeza total)
rem       run.bat --help        (ver todas as opcoes)
rem ================================================================

rem --- Verificar privilegios de Administrador ---
net session >nul 2>&1
if %errorlevel% equ 0 goto RUN

rem --- Solicitar elevacao (Vista+) ---
echo  [!] Solicitando privilegios de Administrador...
powershell -noprofile -Command ^
    "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c \"%~f0\" %*' -Verb RunAs" ^
    >nul 2>&1
if %errorlevel% equ 0 exit /b 0

rem --- Fallback via VBScript (compativel com XP) ---
(
    echo Set Shell = CreateObject^("Shell.Application"^)
    echo Shell.ShellExecute "cmd.exe", "/c ""%~f0"" %*", "", "runas", 1
) > "%TEMP%\_tc_run.vbs"
cscript //nologo "%TEMP%\_tc_run.vbs" >nul 2>&1
del /f /q "%TEMP%\_tc_run.vbs" >nul 2>&1
exit /b 0

:RUN
rem --- Verificar se o JAR existe ---
if not exist "%~dp0TrashCleaner.jar" (
    echo.
    echo  [ERRO] TrashCleaner.jar nao encontrado!
    echo  Execute primeiro: build.bat
    echo.
    pause
    exit /b 1
)

rem --- Verificar se o Java esta instalado ---
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo  [ERRO] Java nao encontrado!
    echo  Instale o Java 8 ou superior: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

rem --- Executar o programa ---
java -jar "%~dp0TrashCleaner.jar" %*
