# Minecraft Admin Logger Mod  
![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen)  
Um mod para administradores de servidores Minecraft que registra ações de jogadores (login, comandos, mortes, etc.) em arquivos de log.  

## ⚙️ Funcionalidades  
- ✅ Logs de login/logout com coordenadas  
- ✅ Registro de comandos executados  
- ✅ Log de mensagens do chat  
- ✅ Suporte a múltiplos idiomas (inglês/português)  
- 🔄 Rotação automática de logs (5 MB por arquivo)  

## 📥 Instalação  
1. Baixe o `.jar` mais recente na aba [Releases](https://github.com/jonatasperaza/Minecraft-admin-logger/releases).  
2. Coloque o arquivo na pasta `mods` do seu servidor.  
3. Reinicie o servidor.  

## ⚙️ Configuração  
Edite o arquivo `adminlogger-common.toml` (gerado na primeira execução):  
```toml  
# Idioma (en_us/pt_br)  
language = "pt_br"  

# Logar comandos?  
LOG_COMMANDS = true  

# Logar chat?  
LOG_CHAT = true  