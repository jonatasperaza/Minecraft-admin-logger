# Minecraft Admin Logger Mod

![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![NeoForge](https://img.shields.io/badge/Loader-NeoForge-orange)

Admin Logger e um mod para administradores de servidores Minecraft que registra acoes importantes de jogadores em arquivos de log.

## Funcionalidades

- Logs de login/logout com coordenadas
- Registro de comandos executados com mascara para argumentos sensiveis
- Log de mensagens do chat
- Log de mortes
- Log de blocos quebrados e colocados
- Log de containers abertos e diferencas de itens ao fechar
- Log de itens dropados e coletados
- Log de mudancas de gamemode
- Log de teleportes por comando, ender pearl, chorus fruit e troca de dimensao
- Filtros por jogador, mundo/dimensao e comandos ignorados
- Logs em formato texto ou JSON Lines (`jsonl`)
- Indice global em `logs/adminlogger/_global/` com todos os eventos do servidor
- Diretorio, tamanho maximo de arquivo e pastas por UUID configuraveis
- Sistema de alertas por categoria, jogador observado ou comando observado
- Alertas para operadores online
- Alertas opcionais via Discord webhook
- Estatisticas da sessao por categoria e jogador
- Comandos `/adminlogger reload`, `/adminlogger status` e `/adminlogger stats`
- Suporte a multiplos idiomas: ingles e portugues
- Rotacao automatica de logs por tamanho configuravel

## Requisitos

- Minecraft 1.21.1
- NeoForge 21.1.233 ou superior
- Java 21

## Instalacao

1. Baixe o `.jar` mais recente na aba [Releases](https://github.com/jonatasperaza/Minecraft-admin-logger/releases).
2. Coloque o arquivo na pasta `mods` do seu servidor NeoForge.
3. Reinicie o servidor.

## Configuracao

Edite o arquivo `adminlogger-common.toml`, gerado na primeira execucao:

```toml
[general]
language = "pt_br"
logDirectory = "logs/adminlogger"
maxLogSizeMb = 5
logFormat = "text"
logGlobalIndex = true
useUuidFolders = false
includePlayerUuid = false
logChat = true
logCommands = true
logInventory = false
logBlocks = true
logContainers = true
logItems = true
logGameMode = true
logTeleports = true
ignoredPlayers = []
ignoredWorlds = []
ignoredCommands = ["login", "register", "l", "reg"]
enableAlerts = false
alertEventTypes = ["commands", "containers"]
watchedPlayers = []
watchedCommands = ["op", "deop", "ban", "pardon", "kick", "stop", "reload", "gamemode", "tp", "teleport"]
broadcastAlertsToOps = true
writeAlertLog = true
discordWebhookEnabled = false
discordWebhookUrl = ""
discordWebhookUsername = "Admin Logger"
maskSensitiveCommands = true
sensitiveCommandTerms = ["password", "passwd", "senha", "token", "secret", "key", "apikey", "api_key", "webhook"]
```

Os logs por jogador sao salvos em `logs/adminlogger/<jogador>/` por padrao. Se `useUuidFolders = true`, os logs passam para `logs/adminlogger/<uuid>/`.

Quando `logGlobalIndex = true`, todo evento tambem e escrito em `logs/adminlogger/_global/`, o que facilita auditoria rapida sem abrir pasta por pasta.

Use `logFormat = "jsonl"` se quiser processar os logs por ferramenta externa, dashboard ou script.

Para ativar alertas, defina `enableAlerts = true`. Com `broadcastAlertsToOps = true`, operadores online recebem alertas no chat. Para enviar ao Discord, preencha `discordWebhookUrl` e ligue `discordWebhookEnabled = true`.

Mantenha `discordWebhookUrl` privado. Se essa URL vazar, qualquer pessoa pode enviar mensagens no canal configurado.

## Build

```powershell
.\gradlew.bat clean build
```

O arquivo final fica em `build/libs/adminlogger-2.3.0.jar`.

## Atualizando para novas versoes

As versoes principais ficam centralizadas em `gradle.properties`:

- `minecraft_version`
- `minecraft_version_range`
- `neo_version`
- `loader_version_range`

Para portar para uma versao mais nova, atualize esses valores usando uma versao NeoForge compativel, rode `.\gradlew.bat build` e revise principalmente os eventos usados em `AdminLogger`.
