@echo off
title TrashCleaner - Testes
echo.
echo  ================================================================
echo   TRASHCLEANER - Suite de Testes
echo  ================================================================
echo.

rem --- Verificar JDK ---
javac -version >nul 2>&1
if %errorlevel% neq 0 (
    echo  [ERRO] javac nao encontrado. Instale o JDK.
    exit /b 1
)

rem --- Compilar fontes principais ---
echo  [-] Compilando fontes...
javac -encoding UTF-8 -d out src\*.java
if %errorlevel% neq 0 (
    echo  [ERRO] Compilacao dos fontes falhou.
    exit /b 1
)

rem --- Compilar testes (classpath inclui out/ para acesso ao codigo) ---
echo  [-] Compilando testes...
javac -encoding UTF-8 -cp out -d out test\TrashCleanerTest.java
if %errorlevel% neq 0 (
    echo  [ERRO] Compilacao dos testes falhou.
    exit /b 1
)

rem --- Executar ---
echo  [-] Executando testes...
echo.
java -cp out TrashCleanerTest
if %errorlevel% neq 0 (
    echo.
    echo  [ERRO] TESTES FALHARAM!
    exit /b 1
)

echo.
echo  ================================================================
echo   TODOS OS TESTES PASSARAM
echo  ================================================================
echo.
