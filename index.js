const express = require('express');
const fs = require('fs');
const path = require('path');
const app = express();
const port = 3000;

app.use(express.json());

// Rota para listar todos os jogadores
app.get('/players', (req, res) => {
    const logsPath = path.join('logs', 'adminlogger');
    try {
        const players = fs.readdirSync(logsPath);
        res.json({ players });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao ler diretório de logs' });
    }
});

// Rota para obter logs de um jogador específico
app.get('/players/:playerName/logs', (req, res) => {
    const { playerName } = req.params;
    const { type } = req.query; // 'chat' ou 'actions'
    const playerPath = path.join('logs', 'adminlogger', playerName);

    try {
        const files = fs.readdirSync(playerPath);
        const logs = {};

        files.forEach(file => {
            if (!type || file.includes(type)) {
                const content = fs.readFileSync(path.join(playerPath, file), 'utf8');
                logs[file] = content.split('\n').filter(line => line.trim());
            }
        });

        res.json({ logs });
    } catch (error) {
        res.status(404).json({ error: 'Jogador não encontrado ou erro ao ler logs' });
    }
});

app.listen(port, () => {
    console.log(`API rodando em http://localhost:${port}`);
});
