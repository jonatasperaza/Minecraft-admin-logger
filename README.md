# Admin Logger Mod

Um mod server-side para Minecraft que registra as atividades dos jogadores em arquivos de log.

## Início Rápido

### Pré-requisitos
- Java Development Kit (JDK) 17
- Git instalado
- IDE de sua preferência (recomendado: IntelliJ IDEA)

### Configuração Inicial

1. **Clone e Configure o Git:**
```bash
git init
git add .
git commit -m "Commit inicial"
git branch -M main
git remote add origin https://github.com/jonatasperaza/Minecraft-admin-logger
git push -u origin main
```

2. **Configure o Ambiente:**
```bash
# Windows
gradlew.bat genIntellijRuns

# Linux/Mac
./gradlew genIntellijRuns
```

3. **Abra na IDE:**
   - Abra o IntelliJ IDEA
   - Importe como projeto Gradle
   - Aguarde o download das dependências

4. **Comandos Úteis:**
```bash
# Compilar o mod
gradlew build

# Testar o servidor
gradlew runServer

# Atualizar dependências
gradlew --refresh-dependencies

# Limpar build
gradlew clean
```

### Comandos Git Básicos
```bash
git status              # Ver alterações
git add .               # Adicionar arquivos
git commit -m "msg"     # Criar commit
git push               # Enviar alterações
git pull               # Atualizar repositório
```

## Funcionalidades

- Registra entrada e saída de jogadores
- Registra mensagens do chat
- Registra comandos executados
- Organiza logs por data em arquivos separados

## Requisitos

- Minecraft 1.20.1, 1.21
- Forge 47 ou superior

## Instalação

1. Instale o Forge no servidor Minecraft
2. Baixe o arquivo .jar do mod
3. Coloque na pasta `mods` do servidor
4. Inicie o servidor

## Formato do Log

```
[HH:mm:ss] NomeJogador - ação
```

Exemplo:
```
[14:30:45] Steve - entrou no servidor
[14:31:00] Steve - chat: Olá pessoal!
[14:31:15] Steve - comando: /help
[14:35:20] Steve - saiu do servidor
```

## Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.
