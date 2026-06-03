# Minecraft Admin Logger Mod

![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![NeoForge](https://img.shields.io/badge/Loader-NeoForge-orange)

Um mod para administradores de servidores Minecraft que registra ações de jogadores em arquivos de log.

## Funcionalidades

- Logs de login/logout com coordenadas
- Registro de comandos executados
- Log de mensagens do chat
- Log de mortes
- Suporte a múltiplos idiomas: inglês e português
- Rotação automática de logs: 5 MB por arquivo

## Requisitos

- Minecraft 1.21.1
- NeoForge 21.1.233 ou superior
- Java 21

## Instalação

1. Baixe o `.jar` mais recente na aba [Releases](https://github.com/jonatasperaza/Minecraft-admin-logger/releases).
2. Coloque o arquivo na pasta `mods` do seu servidor NeoForge.
3. Reinicie o servidor.

## Configuração

Edite o arquivo `adminlogger-common.toml`, gerado na primeira execução:

```toml
[general]
language = "pt_br"
logChat = true
logCommands = true
logInventory = false
```

Os logs são salvos em `logs/adminlogger/<jogador>/`.

## Build

```powershell
.\gradlew.bat build
```

## Atualizando para novas versões

As versões principais ficam centralizadas em `gradle.properties`:

- `minecraft_version`
- `minecraft_version_range`
- `neo_version`
- `loader_version_range`

Para portar para uma versão mais nova, atualize esses valores usando uma versão NeoForge compatível, rode `.\gradlew.bat build` e revise principalmente os eventos usados em `AdminLogger`: login/logout, chat, comandos e morte. Mantenha o `minecraft_version_range` fechado até testar em servidor, para evitar o mod carregar em uma versão ainda não validada.
