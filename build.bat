@echo off
title TrashCleaner - Build
echo.
echo  ================================================================
echo   TRASHCLEANER - Build do projeto Java
echo  ================================================================
echo.

rem --- Verificar se o Java esta instalado ---
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo  [ERRO] Java nao encontrado no PATH!
    echo.
    echo  Instale o Java 8 ou superior:
    echo    https://adoptium.net/
    echo.
    pause
    exit /b 1
)

rem --- Verificar se o javac esta disponivel (JDK, nao apenas JRE) ---
javac -version >nul 2>&1
if %errorlevel% neq 0 (
    echo  [ERRO] javac nao encontrado. Instale o JDK (nao apenas o JRE).
    echo  https://adoptium.net/
    echo.
    pause
    exit /b 1
)

rem --- Criar pasta de saida ---
if not exist out mkdir out

rem --- Compilar ---
echo  [-] Compilando fontes Java...
javac -encoding UTF-8 -d out src\*.java
if %errorlevel% neq 0 (
    echo.
    echo  [ERRO] Compilacao falhou. Veja os erros acima.
    pause
    exit /b 1
)
echo  [OK] Compilacao concluida.

rem --- Empacotar em JAR executavel ---
echo  [-] Criando TrashCleaner.jar...
jar cfm TrashCleaner.jar MANIFEST.MF -C out .
if %errorlevel% neq 0 (
    echo.
    echo  [ERRO] Empacotamento JAR falhou.
    pause
    exit /b 1
)
echo  [OK] TrashCleaner.jar criado com sucesso.

echo.
echo  ================================================================
echo   Build finalizado!
echo   Execute: run.bat              (modo interativo)
echo   Execute: run.bat --quick      (linha de comando)
echo   Execute: run.bat --help       (ver todas as opcoes)
echo  ================================================================
echo.
pause
