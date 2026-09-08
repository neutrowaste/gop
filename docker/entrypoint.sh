#!/bin/bash
set -e

echo "=== Iniciando GOP Container ==="

# Ajuste da porta pública do Nginx conforme variável de ambiente PORT (padrão 80)
PORT="${PORT:-80}"
echo "Configurando Nginx na porta ${PORT}..."
sed -i "s/listen 80 default_server;/listen ${PORT} default_server;/g" /etc/nginx/nginx.conf
sed -i "s/listen \[::\]:80 default_server;/listen [::]:${PORT} default_server;/g" /etc/nginx/nginx.conf

# Se GOP_API_URL estiver definida, exporta para window.__GOP_API_URL__ no frontend
if [ -n "${GOP_API_URL}" ]; then
    echo "Injetando GOP_API_URL (${GOP_API_URL}) no frontend..."
    cat <<EOF > /usr/share/nginx/html/env-config.js
window.__GOP_API_URL__ = "${GOP_API_URL}";
EOF
    # Se index.html não tiver script do env-config, injeta antes de </head>
    if ! grep -q "env-config.js" /usr/share/nginx/html/index.html; then
        sed -i 's|</head>|<script src="/env-config.js"></script></head>|' /usr/share/nginx/html/index.html
    fi
fi

# Garante que o profile postgres seja ativado por padrão caso não tenha sido especificado
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-postgres}"
echo "Perfil Spring ativo: ${SPRING_PROFILES_ACTIVE}"

# Inicia Spring Boot API na porta interna 8081
echo "Iniciando Spring Boot API na porta interna 8081..."
java -Dserver.port=8081 \
     -Djava.security.egd=file:/dev/./urandom \
     -XX:+UseContainerSupport \
     -XX:MaxRAMPercentage=75.0 \
     -jar /app/app.jar &
SPRING_PID=$!

# Inicia Nginx em segundo plano
echo "Iniciando Nginx na porta ${PORT}..."
nginx -g "daemon off;" &
NGINX_PID=$!

cleanup() {
    echo "Sinal de interrupção recebido. Encerrando processos com segurança..."
    kill -TERM "$SPRING_PID" 2>/dev/null || true
    kill -TERM "$NGINX_PID" 2>/dev/null || true
    wait "$SPRING_PID" 2>/dev/null || true
    wait "$NGINX_PID" 2>/dev/null || true
    echo "Processos encerrados com sucesso."
    exit 0
}

trap cleanup SIGTERM SIGINT

# Monitor de processos: se Spring Boot ou Nginx cair, encerra o contêiner (fail-fast para o Docker/Easypanel)
while true; do
    if ! kill -0 "$SPRING_PID" 2>/dev/null; then
        echo "[ALERTA] Processo Spring Boot encerrou!"
        kill -TERM "$NGINX_PID" 2>/dev/null || true
        wait "$SPRING_PID" 2>/dev/null || true
        exit 1
    fi
    if ! kill -0 "$NGINX_PID" 2>/dev/null; then
        echo "[ALERTA] Processo Nginx encerrou!"
        kill -TERM "$SPRING_PID" 2>/dev/null || true
        wait "$NGINX_PID" 2>/dev/null || true
        exit 1
    fi
    sleep 3
done
