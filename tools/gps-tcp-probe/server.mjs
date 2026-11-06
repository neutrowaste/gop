// Sonda de diagnóstico. Quem grava no mapa é o receptor GT06 da API, na mesma porta 5023.
// Não rode os dois ao mesmo tempo: o ngrok deve apontar para a API, não para esta sonda.
import fs from 'node:fs';
import net from 'node:net';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const porta = Number(process.env.GPS_TCP_PORT || 5023);
const host = process.env.GPS_TCP_HOST || '127.0.0.1';
const diretorio = path.dirname(fileURLToPath(import.meta.url));
const arquivoLog = path.join(diretorio, 'recebido.log');

function agora() {
  return new Date().toISOString();
}

function registrar(linha) {
  const texto = `[${agora()}] ${linha}`;
  console.log(texto);
  fs.appendFileSync(arquivoLog, `${texto}\n`);
}

function hexDump(buffer) {
  const hex = [...buffer].map((byte) => byte.toString(16).padStart(2, '0')).join(' ');
  const ascii = [...buffer]
    .map((byte) => (byte >= 32 && byte <= 126 ? String.fromCharCode(byte) : '.'))
    .join('');
  return `hex=${hex} ascii=${ascii}`;
}

const servidor = net.createServer((socket) => {
  const origem = `${socket.remoteAddress}:${socket.remotePort}`;
  registrar(`conexao aberta ${origem}`);

  socket.on('data', (dados) => {
    registrar(`dados ${origem} bytes=${dados.length} ${hexDump(dados)}`);
  });

  socket.on('close', () => {
    registrar(`conexao fechada ${origem}`);
  });

  socket.on('error', (erro) => {
    registrar(`erro ${origem} ${erro.message}`);
  });
});

servidor.listen(porta, host, () => {
  registrar(`escutando ${host}:${porta}`);
});
