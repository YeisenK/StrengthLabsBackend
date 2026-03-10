docker version
docker run --rm hello-world
docker compose version
docker buildx version
docker info
sudo tee /etc/docker/daemon.json > /dev/null <<'EOF'
{
 "log-driver": "json-file",
 "log-opts": {
 "max-size": "50m",
 "max-file": "5",
 "compress": "true"
 },
 "storage-driver": "overlay2",
 "live-restore": true,
 "userland-proxy": false,
 "no-new-privileges": true,
 "default-ulimits": {
 "nofile": {
 "Name": "nofile",
 "Hard": 65536,
 "Soft": 65536
 }
 }
}
EO
f

sudo tee /etc/docker/daemon.json > /dev/null <<'EOF'
{
 "log-driver": "json-file",
 "log-opts": {
 "max-size": "50m",
 "max-file": "5",
 "compress": "true"
 },
 "storage-driver": "overlay2",
 "live-restore": true,
 "userland-proxy": false,
 "no-new-privileges": true,
 "default-ulimits": {
 "nofile": {
 "Name": "nofile",
 "Hard": 65536,
 "Soft": 65536
 }
 }
}
EOF

sudo systemctl reload docker
sudo systemctl status docker --no-pager
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt-get install -y nodejs
node --version # Expected: v22.x.x
npm --version 
npm config get prefix # Expected: /usr
npm config set cache ~/.npm --global
npm install -g pnpm
mkdir -p ~/.npm-global
npm config set prefix '~/.npm-global'
echo 'export PATH="$HOME/.npm-global/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
npm config get prefix
# Expected: /home/v0id/.npm-global
npm install -g pnpm
npm install -g pm2
pnpm --version
npm install -g pm2
pm2 --version 
node -e "
 const v8 = require('v8');
 const os = require('os');
 console.log('Node.js:', process.version);
 console.log('V8 Engine:', process.versions.v8);
 console.log('Platform:', process.platform, process.arch);
 console.log('Heap Limit:', v8.getHeapStatistics().heap_size_limit / 1024 / 1024, 'MB');
 console.log('CPU Cores:', os.cpus().length);
"
ire('crypto').createHash('sha256').update('test').digest('hex')" \
node -e "require('crypto').createHash('sha256').update('test').digest('hex')"  && echo "Crypto module OK"
sudo apt update
sudo apt install qemu-guest-agent -y
sudo systemctl enable --now qemu-guest-agent
sudo apt-get install -y  curl \ # HTTP client: used for Docker GPG key download, healthchecks
curl --version | head -1 # Expected: curl 8.x.x
git --version # Expected: git version 2.43.x
openssl version # Expected: OpenSSL 3.0.x
gcc --version | head -1 # Expected: gcc (Ubuntu 13.x)
make --version | head -1 # Expected: GNU Make 4.x
jq --version # Expected: jq-1.7.x
ufw --version # Expected: ufw 0.36.x
sudo apt install gcc{
sudo apt install gcc
sudo apt install make
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
 https://download.docker.com/linux/ubuntu \
 $(. /etc/os-release && echo "$VERSION_CODENAME") stable"  | sudo tee /etc/apt/sources.list.d/docker.list
sudo apt-get update
sudo apt-get install -y  docker-ce  docker-ce-cli  containerd.io  docker-buildx-plugin  docker-compose-plugin
sudo groupadd docker 2>/dev/null || true
sudo usermod -aG docker $USER
sudo systemctl enable docker.service
sudo systemctl enable containerd.service
sudo systemctl start docker.service
sudo systemctl start containerd.service
newgrp docker
