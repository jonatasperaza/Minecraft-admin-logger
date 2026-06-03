# Changelog

## 2.2.0-1.21.1 - Medium Operations Upgrade

### Adicionado

- Filtros por jogador/UUID com `ignoredPlayers`.
- Filtros por dimensao com `ignoredWorlds`.
- Filtros de comandos ignorados com `ignoredCommands`, com defaults para comandos comuns de autenticacao.
- Opcao `logDirectory` para escolher onde os logs serao salvos.
- Opcao `maxLogSizeMb` para configurar a rotacao de arquivos.
- Opcao `logFormat` com suporte a `text` e `jsonl`.
- Opcao `logGlobalIndex` para gravar todos os eventos tambem em `_global`.
- Opcao `useUuidFolders` para usar UUIDs como pastas por jogador.
- Opcao `includePlayerUuid` para incluir UUID em logs de texto.
- Comando `/adminlogger status` para visualizar versao, diretorio, formato, categorias ativas e filtros.

### Melhorado

- Linhas de log em texto agora incluem a categoria do evento.
- Escrita de logs foi centralizada para aplicar filtros e formato de maneira consistente.
- Arquivo de configuracao de exemplo foi limpo e atualizado.
- README atualizado com as novas opcoes operacionais.

### Compatibilidade

- Minecraft 1.21.1.
- NeoForge 21.1.233 ou superior.
- Java 21.

## 2.1.0-1.21.1 - High Priority Audit Expansion

### Adicionado

- Log de blocos quebrados e colocados com bloco, mundo e coordenadas.
- Log de abertura de containers como baus, barris e shulkers.
- Log de itens adicionados/removidos de containers por comparacao entre abertura e fechamento.
- Log de itens dropados e coletados.
- Log de mudancas de modo de jogo.
- Log de troca de dimensao.
- Log de teleportes por comando, ender pearl e chorus fruit.
- Comando `/adminlogger reload` para recarregar idioma/config em runtime.
- Novas opcoes de config: `logBlocks`, `logContainers`, `logItems`, `logGameMode`, `logTeleports`, `maskSensitiveCommands` e `sensitiveCommandTerms`.

### Melhorado

- Comandos registrados agora podem mascarar valores sensiveis como tokens, senhas, chaves e webhooks.
- Logs de posicao passaram a incluir mundo/dimensao com coordenadas padronizadas.
- README atualizado com os novos recursos e a nova configuracao.

### Compatibilidade

- Minecraft 1.21.1.
- NeoForge 21.1.233 ou superior.
- Java 21.

### Observacoes

- A comparacao de itens em containers e feita ao fechar o container. Reorganizar itens dentro do mesmo container nao deve gerar diferenca se a quantidade total de cada item permanecer igual.
- Para Forge 1.20.1, continue usando a linha 1.4-1.20.1.

## 2.0.0-1.21.1 - NeoForge Migration

### Adicionado

- Migracao para Minecraft 1.21.1.
- Migracao de Forge para NeoForge.
- Build atualizado para Java 21 e NeoGradle.

### Corrigido

- Validacao null-safe da linguagem da config.
- Carregamento de `adminlogger-common.toml` no NeoForge.
