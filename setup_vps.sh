#!/usr/bin/env bash
# setup_vps.sh - Configura um servidor SOCKS5 (Dante) no VPS em Ararangua.
# Objetivo: todo trafego de saida do app saia com o IP publico do VPS,
# garantindo identidade de rede consistente para auditoria.

set -e

echo "[*] Atualizando pacotes..."
apt-get update -y
apt-get install -y dante-server

# Usuario sem privilegios usado pelo daemon
id -u sockd >/dev/null 2>&1 || useradd -r -s /usr/sbin/nologin sockd

echo "[*] Escrevendo /etc/danted.conf..."
cat > /etc/danted.conf <<'EOF'
logoutput: /var/log/sockd.log
internal: eth0 port = 1080
external: eth0

# Metodo de autenticacao: none (restrinja por IP via firewall) ou username.
method: username none

user.privileged: root
user.notprivileged: nobody

# Whitelist de clientes: ajuste para a rede interna / IPs dos tecnicos.
client pass {
    from: 0.0.0.0/0 to: 0.0.0.0/0
    log: connect disconnect
}

pass {
    from: 0.0.0.0/0 to: 0.0.0.0/0
    protocol: tcp udp
    method: none
    log: connect disconnect
}
EOF

echo "[*] Habilitando e iniciando o servico..."
systemctl enable danted
systemctl restart danted

echo "[*] Abrindo porta 1080 no firewall (UFW)..."
ufw allow 1080/tcp || true

echo "[*] Status:"
systemctl status danted --no-pager || true
ss -lntp | grep 1080 || true

echo "[OK] SOCKS5 ativo na porta 1080."