@echo off
setlocal enabledelayedexpansion
title TrashCleaner v1.0 - Limpador e Otimizador de Windows
mode con: cols=72 lines=45
color 0A

rem =====================================================================
rem  TRASHCLEANER v1.0
rem  Limpador e Otimizador de Windows
rem  Compativel: Windows XP / Vista / 7 / 8 / 8.1 / 10 / 11
rem =====================================================================

rem ----- ELEVACAO DE PRIVILEGIOS -----
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo  [!] Este programa requer privilegios de Administrador.
    echo  [!] Solicitando elevacao de permissoes...
    echo.

    rem Tentar via PowerShell (Vista e superior)
    powershell -noprofile -Command ^
        "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c \"%~f0\"' -Verb RunAs" ^
        >nul 2>&1
    if !errorlevel! equ 0 exit /b 0

    rem Fallback via VBScript (compativel com XP)
    (
        echo Set Shell = CreateObject^("Shell.Application"^)
        echo Shell.ShellExecute "cmd.exe", "/c ""%~f0""", "", "runas", 1
    ) > "%TEMP%\_tc_elev.vbs"
    cscript //nologo "%TEMP%\_tc_elev.vbs" >nul 2>&1
    del /f /q "%TEMP%\_tc_elev.vbs" >nul 2>&1
    exit /b 0
)

rem ----- VARIAVEIS INICIAIS -----
set "TC_VER=1.0"
set "TC_DIR=%~dp0"
set "TC_SILENT=0"

rem Construir nome do log com data e hora sem espacos
set "_D=%date%"
set "_T=%time%"
set "_DT=%_D:~-4%%_D:~3,2%%_D:~0,2%_%_T:~0,2%%_T:~3,2%%_T:~6,2%"
set "_DT=%_DT: =0%"
set "TC_LOG=%TC_DIR%TrashCleaner_%_DT%.log"

rem Compatibilidade com XP: LOCALAPPDATA e ProgramData nao existem
if not defined LOCALAPPDATA (
    set "LOCALAPPDATA=%USERPROFILE%\Local Settings\Application Data"
)
if not defined ProgramData (
    if defined ALLUSERSPROFILE (
        set "ProgramData=%ALLUSERSPROFILE%\Application Data"
    ) else (
        set "ProgramData=C:\ProgramData"
    )
)

rem ----- DETECTAR VERSAO DO WINDOWS -----
set "WIN_NAME=Windows Desconhecido"
set "WIN_MAJOR=0"
set "WIN_MINOR=0"
set "WIN_BUILD=0"

for /f "tokens=4-6 delims=[]. " %%a in ('ver 2^>nul') do (
    set "WIN_MAJOR=%%a"
    set "WIN_MINOR=%%b"
    set "WIN_BUILD=%%c"
)
rem Limpar possiveis ] no build
if defined WIN_BUILD set "WIN_BUILD=%WIN_BUILD:]=% "
for /f %%x in ("%WIN_BUILD%") do set "WIN_BUILD=%%x"

if "%WIN_MAJOR%"=="5" (
    if "%WIN_MINOR%"=="0" set "WIN_NAME=Windows 2000"
    if "%WIN_MINOR%"=="1" set "WIN_NAME=Windows XP"
    if "%WIN_MINOR%"=="2" set "WIN_NAME=Windows XP x64 / Server 2003"
)
if "%WIN_MAJOR%"=="6" (
    if "%WIN_MINOR%"=="0" set "WIN_NAME=Windows Vista"
    if "%WIN_MINOR%"=="1" set "WIN_NAME=Windows 7"
    if "%WIN_MINOR%"=="2" set "WIN_NAME=Windows 8"
    if "%WIN_MINOR%"=="3" set "WIN_NAME=Windows 8.1"
)
if "%WIN_MAJOR%"=="10" (
    set "WIN_NAME=Windows 10"
    if %WIN_BUILD% geq 22000 set "WIN_NAME=Windows 11"
)

rem ----- INICIAR LOG -----
(
    echo ================================================================
    echo  TRASHCLEANER v%TC_VER% - Relatorio de Limpeza e Otimizacao
    echo  Sistema: %WIN_NAME% ^(Kernel %WIN_MAJOR%.%WIN_MINOR%.%WIN_BUILD%^)
    echo  Computador: %COMPUTERNAME%   Usuario: %USERNAME%
    echo  Data/Hora: %date% %time%
    echo ================================================================
    echo.
) > "%TC_LOG%"

rem ----- CALCULAR ESPACO LIVRE ANTES -----
set "FREE_BEFORE=N/A"
for /f "tokens=3" %%b in ('dir "%SystemDrive%\" 2^>nul ^| findstr /i "bytes free"') do (
    set "FREE_BEFORE=%%b"
)
echo  Espaco livre ANTES da limpeza: %FREE_BEFORE% bytes >> "%TC_LOG%"
echo. >> "%TC_LOG%"

rem =====================================================================
rem  MENU PRINCIPAL
rem =====================================================================
:MENU
cls
color 0A
echo.
echo  ================================================================
echo   TRASHCLEANER v%TC_VER% - Limpador e Otimizador de Windows
echo   Sistema: %WIN_NAME%
echo  ================================================================
echo.
echo   [1] Limpeza Rapida      Temp, Lixeira, Cache de IE/Edge
echo   [2] Limpeza Profunda    Browsers, WinUpdate, Logs, Dumps
echo   [3] Otimizar Sistema    Defrag/TRIM, Efeitos, Plano Energia
echo   [4] Otimizar Rede       DNS, ARP, Winsock, TCP/IP
echo   [5] Verificar Sistema   SFC + DISM (checa corrucao)
echo   [6] LIMPEZA TOTAL       Todas as opcoes acima
echo   [7] Ver Relatorio       Abre o log da sessao atual
echo   [0] Sair
echo.
echo  ================================================================
echo.
set "OPT="
set /p "OPT=  >>> Escolha uma opcao: "
echo.

if "%OPT%"=="1" ( call :QUICK_CLEAN  & goto MENU )
if "%OPT%"=="2" ( call :DEEP_CLEAN   & goto MENU )
if "%OPT%"=="3" ( call :OPT_SYSTEM   & goto MENU )
if "%OPT%"=="4" ( call :OPT_NETWORK  & goto MENU )
if "%OPT%"=="5" ( call :CHECK_SYSTEM & goto MENU )
if "%OPT%"=="6" ( call :TOTAL_CLEAN  & goto MENU )
if "%OPT%"=="7" ( call :VIEW_REPORT  & goto MENU )
if "%OPT%"=="0" goto :SAIR
goto MENU

rem =====================================================================
rem  [1] LIMPEZA RAPIDA
rem =====================================================================
:QUICK_CLEAN
if "%TC_SILENT%"=="0" (
    cls
    echo.
    echo  ----------------------------------------------------------------
    echo   LIMPEZA RAPIDA
    echo  ----------------------------------------------------------------
    echo.
)
echo [%date% %time%] === LIMPEZA RAPIDA ============================ >> "%TC_LOG%"
call :CORE_QUICK_CLEAN
if "%TC_SILENT%"=="0" (
    call :SHOW_FREED
    echo.
    echo  [OK] Limpeza Rapida concluida!
    echo.
    pause
)
goto :EOF

rem ----- Nucleo da limpeza rapida (chamado internamente tambem) -----
:CORE_QUICK_CLEAN
echo  [-] Temp do usuario...
call :WIPE "%TEMP%"
call :WIPE "%TMP%"
call :WIPE "%LOCALAPPDATA%\Temp"
call :WIPE "%USERPROFILE%\Local Settings\Temp"

echo  [-] Temp do sistema...
call :WIPE "%SystemRoot%\Temp"

echo  [-] Esvaziando Lixeira...
call :EMPTY_RECYCLE

echo  [-] Cache de Internet Explorer / Edge Legacy...
call :WIPE "%LOCALAPPDATA%\Microsoft\Windows\INetCache"
call :WIPE "%USERPROFILE%\Local Settings\Temporary Internet Files"
call :WIPE "%LOCALAPPDATA%\Microsoft\Windows\History\History.IE5"
call :WIPE "%LOCALAPPDATA%\Microsoft\Windows\IECompatCache"
call :WIPE "%LOCALAPPDATA%\Microsoft\Windows\IECompatUaCache"

echo  [-] Cache de miniaturas...
del /f /q "%LOCALAPPDATA%\Microsoft\Windows\Explorer\thumbcache_*.db" >nul 2>&1
del /f /q "%LOCALAPPDATA%\Microsoft\Windows\Explorer\iconcache_*.db"  >nul 2>&1

echo  [-] Cache DNS...
ipconfig /flushdns >nul 2>&1
echo  [LOG] Core limpeza rapida concluida. >> "%TC_LOG%"
goto :EOF

rem =====================================================================
rem  [2] LIMPEZA PROFUNDA
rem =====================================================================
:DEEP_CLEAN
if "%TC_SILENT%"=="0" (
    cls
    echo.
    echo  ----------------------------------------------------------------
    echo   LIMPEZA PROFUNDA
    echo  ----------------------------------------------------------------
    echo.
)
echo [%date% %time%] === LIMPEZA PROFUNDA =========================== >> "%TC_LOG%"

rem Inclui tudo da limpeza rapida
call :CORE_QUICK_CLEAN

echo  [-] Prefetch do Windows...
del /f /q "%SystemRoot%\Prefetch\*.pf"  >nul 2>&1
del /f /q "%SystemRoot%\Prefetch\*.db"  >nul 2>&1
echo  [LOG] Prefetch limpo. >> "%TC_LOG%"

echo  [-] Cache do Windows Update...
net stop wuauserv  >nul 2>&1
net stop bits      >nul 2>&1
net stop cryptsvc  >nul 2>&1
call :WIPE "%SystemRoot%\SoftwareDistribution\Download"
del /f /q "%SystemRoot%\SoftwareDistribution\DataStore\Logs\*" >nul 2>&1
net start cryptsvc >nul 2>&1
net start bits     >nul 2>&1
net start wuauserv >nul 2>&1
echo  [LOG] Cache Windows Update limpo. >> "%TC_LOG%"

echo  [-] Logs do sistema Windows...
del /f /s /q "%SystemRoot%\Logs\*.log" >nul 2>&1
del /f /s /q "%SystemRoot%\Logs\*.etl" >nul 2>&1
del /f /s /q "%SystemRoot%\Logs\CBS\*.log" >nul 2>&1
del /f /s /q "%SystemRoot%\Logs\CBS\*.cab" >nul 2>&1
del /f /s /q "%SystemRoot%\inf\*.log" >nul 2>&1
del /f /s /q "%SystemRoot%\Debug\*.log" >nul 2>&1
echo  [LOG] Logs do sistema limpos. >> "%TC_LOG%"

echo  [-] Relatorios de Erro do Windows (WER)...
call :WIPE "%LOCALAPPDATA%\Microsoft\Windows\WER\ReportQueue"
call :WIPE "%LOCALAPPDATA%\Microsoft\Windows\WER\ReportArchive"
call :WIPE "%ProgramData%\Microsoft\Windows\WER\ReportQueue"
call :WIPE "%ProgramData%\Microsoft\Windows\WER\ReportArchive"
echo  [LOG] WER limpo. >> "%TC_LOG%"

echo  [-] Dumps de memoria (crash dumps)...
if exist "%SystemRoot%\memory.dmp" del /f /q "%SystemRoot%\memory.dmp" >nul 2>&1
del /f /q "%SystemRoot%\Minidump\*.dmp" >nul 2>&1
call :WIPE "%LOCALAPPDATA%\CrashDumps"
echo  [LOG] Crash dumps limpos. >> "%TC_LOG%"

echo  [-] Cache do Google Chrome...
if exist "%LOCALAPPDATA%\Google\Chrome\User Data" (
    for /d %%P in ("%LOCALAPPDATA%\Google\Chrome\User Data\*") do (
        call :WIPE "%%P\Cache"
        call :WIPE "%%P\Cache2"
        call :WIPE "%%P\Code Cache"
        call :WIPE "%%P\GPUCache"
        call :WIPE "%%P\Media Cache"
        call :WIPE "%%P\ShaderCache"
        call :WIPE "%%P\Service Worker\CacheStorage"
        call :WIPE "%%P\Service Worker\ScriptCache"
    )
)
echo  [LOG] Chrome limpo. >> "%TC_LOG%"

echo  [-] Cache do Mozilla Firefox...
for %%R in ("%LOCALAPPDATA%\Mozilla\Firefox\Profiles" "%APPDATA%\Mozilla\Firefox\Profiles") do (
    if exist "%%~R" (
        for /d %%P in ("%%~R\*") do (
            call :WIPE "%%P\cache2"
            call :WIPE "%%P\thumbnails"
            call :WIPE "%%P\startupCache"
            call :WIPE "%%P\shader-cache"
            call :WIPE "%%P\storage\default"
        )
    )
)
echo  [LOG] Firefox limpo. >> "%TC_LOG%"

echo  [-] Cache do Microsoft Edge (Chromium)...
if exist "%LOCALAPPDATA%\Microsoft\Edge\User Data" (
    for /d %%P in ("%LOCALAPPDATA%\Microsoft\Edge\User Data\*") do (
        call :WIPE "%%P\Cache"
        call :WIPE "%%P\Code Cache"
        call :WIPE "%%P\GPUCache"
        call :WIPE "%%P\ShaderCache"
        call :WIPE "%%P\Service Worker\CacheStorage"
    )
)
echo  [LOG] Edge limpo. >> "%TC_LOG%"

echo  [-] Cache do Opera e Opera GX...
call :WIPE "%APPDATA%\Opera Software\Opera Stable\Cache"
call :WIPE "%APPDATA%\Opera Software\Opera GX Stable\Cache"
call :WIPE "%LOCALAPPDATA%\Opera Software\Opera Stable\Cache"
echo  [LOG] Opera limpo. >> "%TC_LOG%"

echo  [-] Cache do Brave Browser...
if exist "%LOCALAPPDATA%\BraveSoftware\Brave-Browser\User Data" (
    for /d %%P in ("%LOCALAPPDATA%\BraveSoftware\Brave-Browser\User Data\*") do (
        call :WIPE "%%P\Cache"
        call :WIPE "%%P\Code Cache"
        call :WIPE "%%P\GPUCache"
        call :WIPE "%%P\ShaderCache"
    )
)
echo  [LOG] Brave limpo. >> "%TC_LOG%"

echo  [-] Cache do Vivaldi...
if exist "%LOCALAPPDATA%\Vivaldi\User Data" (
    for /d %%P in ("%LOCALAPPDATA%\Vivaldi\User Data\*") do (
        call :WIPE "%%P\Cache"
        call :WIPE "%%P\Code Cache"
        call :WIPE "%%P\GPUCache"
    )
)
echo  [LOG] Vivaldi limpo. >> "%TC_LOG%"

echo  [-] Font Cache do Windows...
net stop FontCache >nul 2>&1
net stop "Windows Font Cache Service" >nul 2>&1
del /f /q "%SystemRoot%\ServiceProfiles\LocalService\AppData\Local\FontCache\*" >nul 2>&1
del /f /q "%SystemRoot%\System32\FNTCACHE.DAT" >nul 2>&1
net start FontCache >nul 2>&1
echo  [LOG] Font cache limpo. >> "%TC_LOG%"

echo  [-] DirectX Shader Cache...
call :WIPE "%LOCALAPPDATA%\Microsoft\DirectX Shader Cache"
call :WIPE "%LOCALAPPDATA%\D3DSCache"
echo  [LOG] Shader cache limpo. >> "%TC_LOG%"

echo  [-] Historico do Windows Defender...
call :WIPE "%ProgramData%\Microsoft\Windows Defender\Scans\History\Service\DetectionHistory"
call :WIPE "%ProgramData%\Microsoft\Windows Defender\Scans\History\Results\Quick"
call :WIPE "%ProgramData%\Microsoft\Windows Defender\Scans\History\Results\Full"
echo  [LOG] Defender history limpo. >> "%TC_LOG%"

echo  [-] Instaladores temporarios...
del /f /s /q "%SystemRoot%\$hf_mig$\*" >nul 2>&1
del /f /q   "%SystemRoot%\$NtUninstall*" >nul 2>&1
for /d %%D in ("%SystemRoot%\$NtUninstall*") do rd /s /q "%%D" >nul 2>&1
echo  [LOG] Instaladores antigos limpos. >> "%TC_LOG%"

echo  [-] Limpeza de Disco automatica (cleanmgr)...
call :RUN_CLEANMGR
echo  [LOG] Cleanmgr concluido. >> "%TC_LOG%"

if "%TC_SILENT%"=="0" (
    call :SHOW_FREED
    echo.
    echo  [OK] Limpeza Profunda concluida!
    echo.
    pause
)
goto :EOF

rem =====================================================================
rem  [3] OTIMIZAR SISTEMA
rem =====================================================================
:OPT_SYSTEM
if "%TC_SILENT%"=="0" (
    cls
    echo.
    echo  ----------------------------------------------------------------
    echo   OTIMIZACAO DE SISTEMA
    echo  ----------------------------------------------------------------
    echo.
)
echo [%date% %time%] === OTIMIZACAO DE SISTEMA ====================== >> "%TC_LOG%"

echo  [-] Otimizando efeitos visuais para desempenho...
rem Modo 3 = Personalizado; desativa animacoes sem perder funcionalidade
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\VisualEffects" ^
    /v VisualFXSetting /t REG_DWORD /d 3 /f >nul 2>&1
rem Sem animacao ao minimizar/maximizar janelas
reg add "HKCU\Control Panel\Desktop\WindowMetrics" ^
    /v MinAnimate /t REG_SZ /d "0" /f >nul 2>&1
rem Delay de menu zerado
reg add "HKCU\Control Panel\Desktop" ^
    /v MenuShowDelay /t REG_SZ /d "0" /f >nul 2>&1
rem Sem animacao na taskbar
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\Advanced" ^
    /v TaskbarAnimations /t REG_DWORD /d 0 /f >nul 2>&1
rem Sem animacoes no DWM
reg add "HKCU\Software\Microsoft\Windows\DWM" ^
    /v AnimationsShiftKey /t REG_DWORD /d 0 /f >nul 2>&1
echo  [LOG] Efeitos visuais otimizados. >> "%TC_LOG%"

echo  [-] Ativando plano de energia Alto Desempenho...
rem GUID do plano Alto Desempenho (builtin no Windows)
powercfg /setactive 8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c >nul 2>&1
rem Fallback: nome ingles e portugues
if %errorlevel% neq 0 (
    powercfg /setactive SCHEME_MIN >nul 2>&1
)
echo  [LOG] Plano Alto Desempenho ativado. >> "%TC_LOG%"

echo  [-] Otimizando gerenciamento de memoria...
rem Manter codigo do kernel na RAM (melhora desempenho em sistemas com bastante RAM)
reg add "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Memory Management" ^
    /v DisablePagingExecutive /t REG_DWORD /d 1 /f >nul 2>&1
rem Desabilitar compressao de memoria (Win10+, desativa para CPU com folga de RAM)
reg add "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Memory Management" ^
    /v EnableSuperfetch /t REG_DWORD /d 3 /f >nul 2>&1
echo  [LOG] Memoria otimizada. >> "%TC_LOG%"

echo  [-] Otimizando prioridade de processos em foreground...
reg add "HKLM\SYSTEM\CurrentControlSet\Control\PriorityControl" ^
    /v Win32PrioritySeparation /t REG_DWORD /d 26 /f >nul 2>&1
echo  [LOG] Prioridade CPU otimizada. >> "%TC_LOG%"

echo  [-] Limpando historicos de documentos recentes e MRU...
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\RunMRU"              /f >nul 2>&1
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\TypedPaths"          /f >nul 2>&1
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\RecentDocs"          /f >nul 2>&1
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\ComDlg32\LastVisitedPidlMRU"  /f >nul 2>&1
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\ComDlg32\OpenSavePidlMRU"     /f >nul 2>&1
del /f /q "%APPDATA%\Microsoft\Windows\Recent\*"                 >nul 2>&1
del /f /q "%APPDATA%\Microsoft\Windows\Recent\AutomaticDestinations\*"  >nul 2>&1
del /f /q "%APPDATA%\Microsoft\Windows\Recent\CustomDestinations\*"     >nul 2>&1
echo  [LOG] Historicos limpos. >> "%TC_LOG%"

echo  [-] Limpando logs de eventos do Windows...
call :CLEAR_EVENTLOGS

echo  [-] Otimizando disco (Defrag ou TRIM)...
call :OPTIMIZE_DISK

echo  [-] Reconstruindo cache de icones...
call :REBUILD_ICON_CACHE

if "%TC_SILENT%"=="0" (
    echo.
    echo  [OK] Otimizacao de Sistema concluida!
    echo  ATENCAO: Reinicie o computador para aplicar todas as mudancas.
    echo.
    pause
)
goto :EOF

rem =====================================================================
rem  [4] OTIMIZAR REDE
rem =====================================================================
:OPT_NETWORK
if "%TC_SILENT%"=="0" (
    cls
    echo.
    echo  ----------------------------------------------------------------
    echo   OTIMIZACAO DE REDE
    echo  ----------------------------------------------------------------
    echo.
)
echo [%date% %time%] === OTIMIZACAO DE REDE ========================= >> "%TC_LOG%"

echo  [-] Limpando cache DNS...
ipconfig /flushdns >nul 2>&1
echo  [LOG] DNS limpo. >> "%TC_LOG%"

echo  [-] Limpando cache ARP...
arp -d * >nul 2>&1
echo  [LOG] ARP limpo. >> "%TC_LOG%"

echo  [-] Limpando cache NetBIOS...
nbtstat -R  >nul 2>&1
nbtstat -RR >nul 2>&1
echo  [LOG] NetBIOS limpo. >> "%TC_LOG%"

echo  [-] Resetando Winsock (restaura padrao)...
netsh winsock reset >nul 2>&1
echo  [LOG] Winsock resetado. >> "%TC_LOG%"

echo  [-] Resetando pilha TCP/IP IPv4...
netsh int ip reset >nul 2>&1
echo  [LOG] TCP/IP IPv4 resetado. >> "%TC_LOG%"

echo  [-] Resetando pilha TCP/IP IPv6...
netsh int ipv6 reset >nul 2>&1
echo  [LOG] TCP/IP IPv6 resetado. >> "%TC_LOG%"

rem Otimizacoes avancadas TCP (Vista e superior)
if %WIN_MAJOR% geq 6 (
    echo  [-] Aplicando otimizacoes avancadas TCP/IP...
    netsh int tcp set global autotuninglevel=normal >nul 2>&1
    netsh int tcp set global ecncapability=enabled  >nul 2>&1
    netsh int tcp set global timestamps=disabled    >nul 2>&1
    netsh int tcp set global rss=enabled            >nul 2>&1
    netsh int tcp set global nonsackrttresiliency=disabled >nul 2>&1
    echo  [LOG] TCP avancado otimizado. >> "%TC_LOG%"
)

echo  [-] Otimizando cliente DNS...
reg add "HKLM\SYSTEM\CurrentControlSet\Services\Dnscache\Parameters" ^
    /v MaxCacheEntryTtlLimit /t REG_DWORD /d 86400 /f >nul 2>&1
reg add "HKLM\SYSTEM\CurrentControlSet\Services\Dnscache\Parameters" ^
    /v MaxSOACacheEntryTtlLimit /t REG_DWORD /d 300 /f >nul 2>&1
reg add "HKLM\SYSTEM\CurrentControlSet\Services\Dnscache\Parameters" ^
    /v CacheHashTableSize /t REG_DWORD /d 256 /f >nul 2>&1
echo  [LOG] DNS client otimizado. >> "%TC_LOG%"

rem Renovar IP via DHCP (causa desconexao momentanea)
echo  [-] Renovando configuracao DHCP...
ipconfig /release >nul 2>&1
ipconfig /renew   >nul 2>&1
echo  [LOG] DHCP renovado. >> "%TC_LOG%"

if "%TC_SILENT%"=="0" (
    echo.
    echo  [OK] Otimizacao de Rede concluida!
    echo  ATENCAO: Reinicie para consolidar as mudancas na pilha de rede.
    echo.
    pause
)
goto :EOF

rem =====================================================================
rem  [5] VERIFICAR SISTEMA (SFC + DISM)
rem =====================================================================
:CHECK_SYSTEM
cls
echo.
echo  ----------------------------------------------------------------
echo   VERIFICACAO E REPARO DO SISTEMA
echo  ----------------------------------------------------------------
echo  (Isso pode levar de 10 a 30 minutos - nao feche esta janela)
echo.
echo [%date% %time%] === VERIFICACAO DE SISTEMA ==================== >> "%TC_LOG%"

echo  [-] Verificando integridade dos arquivos do sistema (SFC)...
echo  Por favor, aguarde...
echo.
sfc /scannow
echo. >> "%TC_LOG%"
echo  [LOG] SFC concluido. >> "%TC_LOG%"

rem DISM disponivel a partir do Windows 8 / Server 2012
if %WIN_MAJOR% geq 10 (
    echo.
    echo  [-] Verificando e reparando imagem do Windows (DISM)...
    echo  Por favor, aguarde...
    dism /online /cleanup-image /checkhealth  >> "%TC_LOG%" 2>&1
    dism /online /cleanup-image /scanhealth   >> "%TC_LOG%" 2>&1
    dism /online /cleanup-image /restorehealth >> "%TC_LOG%" 2>&1
    echo  [LOG] DISM concluido. >> "%TC_LOG%"
) else (
    if %WIN_MAJOR% equ 6 (
        if %WIN_MINOR% geq 2 (
            echo.
            echo  [-] Verificando imagem do Windows (DISM)...
            dism /online /cleanup-image /restorehealth >> "%TC_LOG%" 2>&1
            echo  [LOG] DISM concluido. >> "%TC_LOG%"
        )
    )
)

echo.
echo  [OK] Verificacao de sistema concluida!
echo  Consulte o relatorio para ver os detalhes completos.
echo.
pause
goto :EOF

rem =====================================================================
rem  [6] LIMPEZA TOTAL
rem =====================================================================
:TOTAL_CLEAN
cls
echo.
echo  ================================================================
echo   LIMPEZA TOTAL - Aguarde, isso pode demorar varios minutos...
echo  ================================================================
echo.
echo [%date% %time%] === LIMPEZA TOTAL ============================== >> "%TC_LOG%"

set "TC_SILENT=1"

echo  [FASE 1/4] Limpeza Profunda...
call :DEEP_CLEAN

echo  [FASE 2/4] Otimizando Sistema...
call :OPT_SYSTEM

echo  [FASE 3/4] Otimizando Rede...
call :OPT_NETWORK

set "TC_SILENT=0"

echo  [FASE 4/4] Calculando espaco liberado...
call :SHOW_FREED

echo.
echo  ================================================================
echo   LIMPEZA TOTAL CONCLUIDA COM SUCESSO!
echo  ================================================================
echo.
echo  Log completo salvo em:
echo  %TC_LOG%
echo.
echo [%date% %time%] LIMPEZA TOTAL CONCLUIDA >> "%TC_LOG%"

set "RB="
set /p "RB=  Deseja reiniciar o computador agora? (S = Sim): "
if /i "%RB%"=="S" (
    echo  Reiniciando em 60 segundos... (shutdown /a para cancelar)
    shutdown /r /t 60 /c "TrashCleaner: Reinicio para aplicar otimizacoes."
)
pause
goto :EOF

rem =====================================================================
rem  [7] VER RELATORIO
rem =====================================================================
:VIEW_REPORT
if exist "%TC_LOG%" (
    start notepad.exe "%TC_LOG%"
) else (
    echo  Nenhum relatorio encontrado nesta sessao.
    echo  Execute uma limpeza primeiro.
    echo.
    pause
)
goto :EOF

rem =====================================================================
rem  SAIDA
rem =====================================================================
:SAIR
cls
echo.
echo  ================================================================
echo   Obrigado por usar o TrashCleaner v%TC_VER%!
echo   Log salvo em: %TC_LOG%
echo  ================================================================
echo.
call :WAIT 2
exit /b 0

rem =====================================================================
rem  FUNCOES AUXILIARES
rem =====================================================================

rem ----- WIPE: Apaga o conteudo de um diretorio com seguranca -----
:WIPE
if "%~1"=="" goto :EOF
if not exist "%~1" goto :EOF
echo  [LOG] Limpando: %~1 >> "%TC_LOG%"
del /f /s /q "%~1\*"      >nul 2>&1
for /d %%D in ("%~1\*") do rd /s /q "%%D" >nul 2>&1
goto :EOF

rem ----- EMPTY_RECYCLE: Esvazia a Lixeira em todos os drives -----
:EMPTY_RECYCLE
echo  [LOG] Esvaziando lixeira em todos os drives... >> "%TC_LOG%"

rem PowerShell (Vista+): mais confiavel
powershell -noprofile -Command ^
    "Clear-RecycleBin -Force -ErrorAction SilentlyContinue" >nul 2>&1

rem Fallback direto para todos os drives de A a Z
for %%D in (A B C D E F G H I J K L M N O P Q R S T U V W X Y Z) do (
    rem Windows Vista+ usa $Recycle.Bin
    if exist "%%D:\$Recycle.Bin" (
        for /d %%R in ("%%D:\$Recycle.Bin\*") do rd /s /q "%%R" >nul 2>&1
    )
    rem Windows XP usa RECYCLER
    if exist "%%D:\RECYCLER" (
        for /d %%R in ("%%D:\RECYCLER\*") do rd /s /q "%%R" >nul 2>&1
    )
)
echo  [LOG] Lixeira esvaziada. >> "%TC_LOG%"
goto :EOF

rem ----- OPTIMIZE_DISK: Detecta SSD ou HDD e otimiza corretamente -----
:OPTIMIZE_DISK
set "_SSD=0"

rem Detectar SSD via PowerShell (Win8+)
if %WIN_MAJOR% geq 6 (
    if %WIN_MINOR% geq 2 (
        for /f %%S in ('powershell -noprofile -Command ^
            "try{(Get-PhysicalDisk|Where{$_.MediaType-eq'SSD'}|Measure).Count}catch{0}" ^
            2^>nul') do set "_SSD=%%S"
    )
)
if %WIN_MAJOR% geq 10 (
    for /f %%S in ('powershell -noprofile -Command ^
        "try{(Get-PhysicalDisk|Where{$_.MediaType-eq'SSD'}|Measure).Count}catch{0}" ^
        2^>nul') do set "_SSD=%%S"
)

if "%_SSD%"=="0" (
    rem SSD nao detectado via PS, tentar via fsutil
    fsutil behavior query DisableDeleteNotify 2>nul | findstr /i "= 0" >nul 2>&1
    if %errorlevel% equ 0 set "_SSD=1"
)

if "%_SSD%"=="1" (
    echo  [DISCO] SSD detectado - executando TRIM/Optimize...
    if %WIN_MAJOR% geq 10 (
        defrag "%SystemDrive%" /O /U >nul 2>&1
    ) else (
        fsutil behavior set DisableDeleteNotify 0 >nul 2>&1
    )
    echo  [LOG] SSD otimizado via TRIM. >> "%TC_LOG%"
) else (
    echo  [DISCO] HDD detectado - executando desfragmentacao...
    echo  (Aguarde - pode levar varios minutos...)
    defrag "%SystemDrive%" /U >nul 2>&1
    echo  [LOG] HDD desfragmentado. >> "%TC_LOG%"
)
goto :EOF

rem ----- CLEAR_EVENTLOGS: Limpa todos os logs de eventos do Windows -----
:CLEAR_EVENTLOGS
rem Vista+ usa wevtutil
if %WIN_MAJOR% geq 6 (
    for /f "tokens=*" %%L in ('wevtutil el 2^>nul') do (
        wevtutil cl "%%L" >nul 2>&1
    )
    echo  [LOG] Logs de eventos limpos (wevtutil). >> "%TC_LOG%"
    goto :EOF
)
rem XP: usar WMI via VBScript
(
    echo Set oWMI = GetObject^("winmgmts:root\cimv2"^)
    echo Set oLogs = oWMI.ExecQuery^("SELECT * FROM Win32_NTEventLogFile"^)
    echo For Each oL in oLogs : oL.ClearEventLog^(^) : Next
) > "%TEMP%\_tc_evtlog.vbs"
cscript //nologo "%TEMP%\_tc_evtlog.vbs" >nul 2>&1
del /f /q "%TEMP%\_tc_evtlog.vbs" >nul 2>&1
echo  [LOG] Logs de eventos limpos (WMI/XP). >> "%TC_LOG%"
goto :EOF

rem ----- REBUILD_ICON_CACHE: Reconstroi cache de icones do Explorer -----
:REBUILD_ICON_CACHE
taskkill /f /im explorer.exe >nul 2>&1
del /f /q "%LOCALAPPDATA%\Microsoft\Windows\Explorer\iconcache_*.db" >nul 2>&1
del /f /q "%LOCALAPPDATA%\IconCache.db" >nul 2>&1
del /f /q "%APPDATA%\Microsoft\Windows\Recent\AutomaticDestinations\*" >nul 2>&1
del /f /q "%APPDATA%\Microsoft\Windows\Recent\CustomDestinations\*" >nul 2>&1
start "" explorer.exe >nul 2>&1
echo  [LOG] Cache de icones reconstruido. >> "%TC_LOG%"
goto :EOF

rem ----- RUN_CLEANMGR: Configura e executa a Limpeza de Disco silenciosa -----
:RUN_CLEANMGR
if not exist "%SystemRoot%\System32\cleanmgr.exe" goto :EOF
rem Configurar todas as categorias de limpeza (StateFlags0099 = preset 99)
set "_CG=HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches"
for %%K in (
    "Active Setup Temp Folders"
    "BranchCache"
    "Content Indexer Cleaner"
    "D3D Shader Cache"
    "Delivery Optimization Files"
    "Device Driver Packages"
    "Diagnostic Data Viewer database files"
    "Downloaded Program Files"
    "Internet Cache Files"
    "Memory Dump Files"
    "Offline Pages Files"
    "Old ChkDsk Files"
    "Recycle Bin"
    "RetailDemo Offline Content"
    "Setup Log Files"
    "System error memory dump files"
    "System error minidump files"
    "Temporary Files"
    "Temporary Setup Files"
    "Temporary Sync Files"
    "Thumbnail Cache"
    "Update Cleanup"
    "Upgrade Discarded Files"
    "User file versions"
    "Windows Defender"
    "Windows Error Reporting Archive Files"
    "Windows Error Reporting Files"
    "Windows Error Reporting Queue Files"
    "Windows Error Reporting Temp Files"
    "Windows ESD installation files"
    "Windows Upgrade Log Files"
) do (
    reg add "%_CG%\%%~K" /v StateFlags0099 /t REG_DWORD /d 2 /f >nul 2>&1
)
cleanmgr /sagerun:99 >nul 2>&1
goto :EOF

rem ----- SHOW_FREED: Mostra o espaco livre antes e depois -----
:SHOW_FREED
set "FREE_AFTER=N/A"
for /f "tokens=3" %%b in ('dir "%SystemDrive%\" 2^>nul ^| findstr /i "bytes free"') do (
    set "FREE_AFTER=%%b"
)
echo.
echo  ----------------------------------------------------------------
echo   Espaco livre ANTES: %FREE_BEFORE% bytes
echo   Espaco livre APOS:  %FREE_AFTER% bytes
echo  ----------------------------------------------------------------
echo  [LOG] Espaco livre APOS: %FREE_AFTER% bytes >> "%TC_LOG%"
goto :EOF

rem ----- WAIT: Aguarda N segundos (compativel com XP - sem timeout) -----
:WAIT
ping -n %1 127.0.0.1 >nul 2>&1
goto :EOF
