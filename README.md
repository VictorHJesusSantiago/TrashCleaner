# TrashCleaner v2.0

Limpador e otimizador de Windows compatível com **XP, Vista, 7, 8, 8.1, 10 e 11**.

Disponível em **duas versões**:
- **Java** (`run.bat` / `java -jar TrashCleaner.jar`) — menu interativo + linha de comando
- **Batch puro** (`TrashCleaner.bat`) — funciona sem Java instalado

---

## Versao Java (recomendada)

### Requisitos
- Java 8 ou superior ([adoptium.net](https://adoptium.net/))
- Windows XP ao 11
- Executar como **Administrador**

### Como usar — modo interativo

```
run.bat
```

Ou diretamente:
```
java -jar TrashCleaner.jar
```

### Como usar — linha de comando (CLI)

Flags globais (podem ser combinadas com qualquer operacao):
```
--dry-run        Simular operacoes sem executar nada
--silent         Sem prompts interativos (para scripts/agendamentos)
```

#### LIMPEZA
```
run.bat --quick          # Temp, lixeira, IE/Edge, DNS, thumbnails
run.bat --deep           # Browsers, WinUpdate, logs, dumps, apps cache
run.bat --apps           # Cache de Teams, Discord, Spotify, Slack, VS Code...
run.bat --shadows-old    # Remover shadow copies antigas (manter a mais recente)
run.bat --shadows-all    # Remover TODAS as shadow copies
run.bat --winsxs         # Limpeza WinSxS via DISM
run.bat --lang-packs     # Verificar pacotes de idioma nao usados
run.bat --all            # deep + apps + sistema + rede + privacidade + performance
```

#### OTIMIZACAO
```
run.bat --optimize          # Efeitos visuais, energia, defrag/TRIM, MRU
run.bat --network           # DNS, ARP, Winsock, TCP/IP, IPv4+IPv6
run.bat --privacy           # Telemetria, Cortana, ads, localizacao
run.bat --performance       # Game Bar, transparencia, background apps
run.bat --tweaks            # Privacidade + Performance juntos
run.bat --sounds-off        # Desabilitar efeitos de som
run.bat --pagefile-auto     # Configurar pagefile como automatico
run.bat --pagefile-custom 1024,4096   # Pagefile min/max em MB
run.bat --pagefile-off      # Desabilitar pagefile (CUIDADO)
run.bat --search-rebuild    # Reconstruir indice de pesquisa
run.bat --write-cache       # Otimizar write-caching de disco
run.bat --winupdate-repair  # Reparar Windows Update (sequencia completa)
run.bat --dotnet-repair     # Verificar/reparar .NET Framework
```

#### SISTEMA
```
run.bat --check          # SFC + DISM (verificar/corrigir integridade)
run.bat --info           # CPU, GPU, RAM, Disco, BIOS, rede
run.bat --programs       # Listar programas instalados (64-bit, 32-bit, usuario)
run.bat --drivers        # Listar drivers instalados
run.bat --updates        # Listar atualizacoes instaladas
run.bat --startup        # Listar programas de inicializacao
run.bat --disk-health    # Saude dos discos (SMART)
```

#### SERVICOS E TAREFAS
```
run.bat --services          # Listar servicos opcionais e status
run.bat --services-gaming   # Perfil Gaming (desabilitar Xbox, Maps, etc.)
run.bat --services-min      # Perfil Minimo (desabilitar todos os opcionais)
run.bat --services-restart  # Reiniciar servicos criticos (Spooler, WU, etc.)
run.bat --tasks-list        # Listar tarefas de telemetria
run.bat --tasks-disable     # Desabilitar todas as tarefas de telemetria
run.bat --tasks-enable      # Habilitar todas as tarefas de telemetria
run.bat --boot-diag         # Diagnostico de tempo de boot (Get-WinEvent)
```

#### DISCO
```
run.bat --disk-top 20        # Top 20 pastas maiores
run.bat --disk-large 100     # Arquivos maiores que 100 MB
run.bat --disk-dupes [path]  # Localizador de duplicados (por nome+tamanho)
run.bat --disk-frag C:       # Verificar fragmentacao
run.bat --chkdsk C:          # Agendar CHKDSK no proximo boot
run.bat --shadows-list       # Listar shadow copies (VSS)
run.bat --ntfs-compress [path]   # Compressao NTFS de pasta
run.bat --backup [src] [dst]     # Backup de arquivos
```

#### REDE AVANCADA
```
run.bat --net-test              # Ping para 8.8.8.8, 1.1.1.1, google.com...
run.bat --net-connections       # Conexoes ativas (netstat)
run.bat --net-ports             # Portas abertas com processo responsavel
run.bat --net-dns-set google    # Mudar DNS (google/cloudflare/opendns/quad9/auto)
run.bat --net-dns-set 1.1.1.1,1.0.0.1   # DNS personalizado
run.bat --net-wifi              # Gerenciar perfis Wi-Fi
run.bat --net-adapters          # Gerenciar adaptadores de rede
run.bat --net-proxy-reset       # Redefinir configuracoes de proxy
run.bat --net-trace google.com  # Traceroute
run.bat --net-info              # ipconfig /all + route + arp
```

#### SEGURANCA
```
run.bat --firewall          # Gerenciar Firewall do Windows
run.bat --defender-scan     # Windows Defender scan rapido
run.bat --credentials       # Gerenciar Credential Manager
run.bat --users             # Listar usuarios do sistema
run.bat --check-updates     # Verificar atualizacoes pendentes
run.bat --bsod-history      # Historico de BSOD e erros criticos
run.bat --processes         # Top processos por CPU e RAM
run.bat --secure-boot       # Verificar Secure Boot e TPM
run.bat --driver-check      # Verificar integridade de drivers
```

#### RECUPERACAO
```
run.bat --restore-list          # Listar pontos de restauracao
run.bat --restore-point         # Criar ponto de restauracao
run.bat --restore-apply 3       # Restaurar para o ponto 3
run.bat --mbr-repair all        # Reparar MBR/Boot (all/fixmbr/fixboot/rebuildbcd)
run.bat --perms [path]          # Reparar permissoes de arquivo
run.bat --file-assoc            # Reparar associacoes (.exe, .bat, .com, .reg)
run.bat --dotnet-check          # Verificar .NET Framework instalado
```

#### FERRAMENTAS
```
run.bat --ram               # Liberar RAM (flush working sets)
run.bat --battery           # Relatorio HTML de bateria
run.bat --clipboard         # Limpar area de transferencia
run.bat --hibernate-on      # Habilitar hibernacao
run.bat --hibernate-off     # Desabilitar hibernacao (libera hiberfil.sys)
run.bat --reg-check         # Verificar registro (RunOnce, assoc)
```

#### RELATORIOS E AUTOMACAO
```
run.bat --report-html                        # Gerar relatorio HTML da sessao
run.bat --history                            # Ver sessoes anteriores
run.bat --schedule-daily 03:00 --quick       # Agendar limpeza diaria
run.bat --schedule-weekly SUN 03:00 --quick  # Agendar limpeza semanal
run.bat --schedule-list                      # Listar agendamentos
run.bat --schedule-remove daily              # Remover agendamento diario
run.bat --schedule-remove all                # Remover todos os agendamentos
```

Multiplas opcoes em sequencia:
```
run.bat --deep --network --privacy
run.bat --all --report-html
run.bat --quick --privacy --dry-run
```

### Como compilar

```
build.bat
```

---

## Versao Batch pura

Funciona em qualquer Windows sem nenhuma dependencia:

```
Clique com o botao direito em TrashCleaner.bat
→ Executar como administrador
```

---

## Menu interativo (modo terminal)

### Menu Principal

| Tecla | Funcao |
|-------|--------|
| `1` | Limpeza Rapida — temp, lixeira, IE/Edge, DNS |
| `2` | Limpeza Profunda — browsers, WinUpdate, logs, dumps, apps |
| `3` | Cache de Aplicativos — Teams, Discord, Spotify, Slack, VS Code |
| `4` | Otimizar Sistema — efeitos, energia, defrag/TRIM |
| `5` | Otimizar Rede — DNS, ARP, Winsock, TCP/IP |
| `6` | Privacidade — telemetria, Cortana, ads |
| `7` | Performance — Game Bar, transparencia, background apps |
| `8` | Verificar Integridade — SFC + DISM |
| `9` | LIMPEZA TOTAL — tudo de uma vez + relatorio HTML automatico |
| `I` | Informacoes do Sistema — CPU, GPU, RAM, Disco, BIOS |
| `D` | Analisador de Disco — top pastas, grandes, duplicados, backup |
| `N` | Rede Avancada — ping, netstat, DNS, Wi-Fi, portas |
| `S` | Seguranca — Firewall, Defender, BSOD, usuarios |
| `G` | Servicos do Windows — gerenciar servicos por perfil |
| `T` | Tarefas Agendadas — desabilitar telemetria, boot diag |
| `F` | Ferramentas Avancadas — RAM, bateria, hibernacao, startup |
| `R` | Recuperacao e Reparo — MBR, permissoes, pontos de restauracao |
| `O` | Otimizacao Avancada — pagefile, som, WU repair, .NET |
| `H` | Historico de Sessoes — ver e exportar logs anteriores |
| `V` | Ver Relatorio Atual |
| `A` | Agendar Limpeza Automatica |
| `?` | Ajuda contextual |
| `0` | Sair |

---

## Estrutura do projeto Java

```
src/
  TrashCleaner.java         # Ponto de entrada: menu interativo + parser CLI
  Config.java               # Flags globais (dryRun, silent) e estatisticas de sessao
  SystemInfo.java           # Deteccao de versao Windows e caminhos de ambiente
  Logger.java               # Logging duplo (console + arquivo .log) com barra de progresso
  Utils.java                # exec(), wipeDir(), execCapture(), calcDirSize(), copyDir()
  Cleaner.java              # quickClean(), deepClean(), cleanAppCache(), cleanWinSxS()
  Optimizer.java            # optimizeSystem(), optimizeNetwork(), repairWindowsUpdate()
  SystemChecker.java        # check() — SFC + DISM
  SystemInfoDisplay.java    # show() — CPU, GPU, RAM, BIOS, discos, rede via WMIC/PS
  PrivacyOptimizer.java     # applyPrivacy(), applyPerformance(), applyAll()
  StartupManager.java       # listStartup(), manage()
  SystemTools.java          # flushRam, createRestorePoint, batteryReport, etc.
  DiskAnalyzer.java         # topFolders, findLargeFiles, findDuplicates, backupFiles
  NetworkTools.java         # testConnectivity, listOpenPorts, configureDns, manageWifi
  SecurityTools.java        # manageFirewall, defenderScan, showBsodHistory, checkDrivers
  ServiceManager.java       # listServices, applyGamingProfile, applyMinimalProfile
  ScheduledTaskManager.java # listTasks, disableTelemetryTasks, bootDiagnostic
  RecoveryTools.java        # listRestorePoints, repairBoot, repairPermissions
  ReportGenerator.java      # generate() — relatorio HTML dark theme com graficos
  SessionHistory.java       # listSessions(), exportLogToHtml()
  AutoScheduler.java        # scheduleDaily(), scheduleWeekly(), listSchedules()
```

---

## Compatibilidade

| Windows    | Limpeza | Otimizacao | Privacidade | SFC | DISM | Firewall | ANSI |
|------------|---------|------------|-------------|-----|------|----------|------|
| XP / 2003  | ✅ | ✅ | parcial | ✅ | ❌ | netsh (parcial) | ❌ |
| Vista / 7  | ✅ | ✅ | parcial | ✅ | ❌ | ✅ advfirewall | ❌ |
| 8 / 8.1    | ✅ | ✅ | parcial | ✅ | ✅ | ✅ advfirewall | ❌ |
| 10 / 11    | ✅ | ✅ | ✅ completa | ✅ | ✅ | ✅ advfirewall | ✅ |

- **Dry-run**: todas as operacoes simuladas com `--dry-run` antes de executar de fato
- **Relatorio HTML**: gerado automaticamente na Limpeza Total ou com `--report-html`
- **Agendamento**: integrado ao Task Scheduler do Windows via `schtasks`
- **Shadow Copies**: gerenciamento via `vssadmin`
- **Startup Manager**: `StartupApproved` (Win8+, identico ao Gerenciador de Tarefas)

---

## Log de sessao

Cada execucao gera `TrashCleaner_AAAAMMDD_HHMMSS.log` na pasta do programa.
O menu `[H]` permite abrir, exportar para HTML ou deletar logs anteriores.

---

## Seguranca

- Nunca apaga arquivos de sistema
- Modo `--dry-run` simula tudo sem executar nada
- Servicos sao apenas pausados durante limpeza e reiniciados logo apos
- Solicita confirmacao antes de reiniciar o computador
- Startup Manager usa `StartupApproved` (nao-destrutivo) no Win8+
