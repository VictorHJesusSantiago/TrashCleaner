# TrashCleaner

Limpador e otimizador de Windows compatível com **XP, Vista, 7, 8, 8.1, 10 e 11**.

Disponível em **duas versões**:
- **Java** (`run.bat` / `java -jar TrashCleaner.jar`) — menu interativo + linha de comando
- **Batch puro** (`TrashCleaner.bat`) — funciona sem Java instalado

---

## Versao Java (recomendada)

### Requisitos
- Java 8 ou superior ([adoptium.net](https://adoptium.net/))
- Windows XP ao 11

### Como usar — modo interativo

```
run.bat
```

Ou se preferir rodar o JAR diretamente:
```
java -jar TrashCleaner.jar
```

### Como usar — linha de comando (CLI)

```
run.bat --quick          # Limpeza rapida
run.bat --deep           # Limpeza profunda
run.bat --optimize       # Otimizar sistema
run.bat --network        # Otimizar rede
run.bat --check          # Verificar integridade (SFC + DISM)
run.bat --all            # Tudo de uma vez
run.bat --help           # Ver todas as opcoes
```

Multiplas opcoes em sequencia:
```
run.bat --deep --network
run.bat --quick --optimize
```

### Como compilar (primeira vez ou apos alterar o codigo)

```
build.bat
```

Isso roda `javac` + `jar` e gera o `TrashCleaner.jar`.

---

## Versao Batch pura

Funciona em qualquer Windows sem nenhuma dependencia:

```
Clique com o botao direito em TrashCleaner.bat
→ Executar como administrador
```

---

## Menu de opcoes (identico nas duas versoes)

| Opcao | O que faz |
|-------|-----------|
| **Limpeza Rapida** | Temp do usuario/sistema, lixeira, IE/Edge, cache de miniaturas, DNS |
| **Limpeza Profunda** | Tudo da rapida + Chrome/Firefox/Edge/Brave/Opera/Vivaldi, Prefetch, Windows Update, logs, WER, dumps, Font Cache, Shader Cache, Defender, cleanmgr |
| **Otimizar Sistema** | Efeitos visuais, plano Alto Desempenho, CPU prioridade, MRU/historicos, Defrag (HDD) ou TRIM (SSD), logs de eventos, cache de icones |
| **Otimizar Rede** | DNS, ARP, NetBIOS, Winsock, TCP/IP IPv4+IPv6, TCP avancado (Vista+), DHCP |
| **Verificar Sistema** | `sfc /scannow` + DISM checkhealth/scanhealth/restorehealth |
| **Limpeza Total** | Todas as opcoes acima em sequencia |

---

## Estrutura do projeto Java

```
src/
  TrashCleaner.java    # Ponto de entrada: menu interativo + parser CLI
  SystemInfo.java      # Deteccao de versao Windows e caminhos de ambiente
  Logger.java          # Logging duplo (console + arquivo .log)
  Utils.java           # exec(), wipeDir(), deleteGlob(), reg(), regDelete()
  Cleaner.java         # quickClean() e deepClean()
  Optimizer.java       # optimizeSystem() e optimizeNetwork()
  SystemChecker.java   # check() — SFC + DISM

MANIFEST.MF            # Main-Class para o JAR
build.bat              # javac + jar
run.bat                # Elevacao + java -jar TrashCleaner.jar
TrashCleaner.bat       # Versao batch pura (sem Java)
```

---

## Compatibilidade

| Windows | Limpeza | Otimizacao | SFC | DISM | Cores ANSI |
|---------|---------|------------|-----|------|------------|
| XP / 2003 | ✅ | ✅ | ✅ | ❌ | ❌ |
| Vista / 7 | ✅ | ✅ | ✅ | ❌ | ❌ |
| 8 / 8.1   | ✅ | ✅ | ✅ | ✅ | ❌ |
| 10 / 11   | ✅ | ✅ | ✅ | ✅ | ✅ |

Detalhes de compatibilidade:
- **Elevacao automatica**: PowerShell (Vista+) com fallback VBScript (XP)
- **`LOCALAPPDATA`**: definido manualmente no XP (`Local Settings\Application Data`)
- **Logs de eventos**: `wevtutil` (Vista+) com fallback WMI/VBScript (XP)
- **TRIM/SSD**: detectado via `Get-PhysicalDisk` (Win8+) ou `fsutil` (fallback)
- **Lixeira**: suporta `$Recycle.Bin` (Vista+) e `RECYCLER` (XP)

---

## Log de sessao

Cada execucao gera `TrashCleaner_AAAAMMDD_HHMMSS.log` na pasta do programa,
com horario, espaco liberado e resultado de cada operacao.

## Seguranca

- Nunca apaga arquivos de sistema
- Servicos sao apenas pausados durante limpeza e reiniciados logo apos
- Arquivos em uso sao ignorados silenciosamente (sem erros fatais)
- Solicita confirmacao antes de reiniciar o computador
