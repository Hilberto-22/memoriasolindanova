# Publicar em memoriasolindanova.com.br

Este roteiro usa o proxy HTTPS já existente na VPS. O arquivo `compose.vps.yaml` complementa `compose.yaml`: ativa cookies de sessão seguros, reconhece os cabeçalhos HTTPS do proxy, configura reinicialização dos serviços, limita logs e verifica a disponibilidade do portal e da API. O serviço `create-bucket` continua sendo uma tarefa única.

## DNS e proxy

No provedor de DNS do domínio, configure um registro `A` para `memoriasolindanova.com.br` apontando para o IPv4 público da VPS. Um registro `AAAA` só deve ser usado se o IPv6 da VPS também estiver configurado para atender ao site. Se quiser usar `www`, configure-o no DNS e no proxy, preferencialmente redirecionando para `https://memoriasolindanova.com.br`.

No proxy existente:

- Cadastre `memoriasolindanova.com.br`.
- Emita e configure o certificado HTTPS e sua renovação automática.
- Redirecione HTTP para HTTPS.
- Encaminhe todas as rotas para o portal, incluindo `/api/`.
- Preserve `Host` e envie `X-Forwarded-Proto: https` nas requisições HTTPS. O proxy de entrada deve substituir os cabeçalhos de encaminhamento recebidos dos clientes.
- Permita requisições de pelo menos `1505 MiB` e um tempo de resposta de `600s`, acompanhando a configuração interna. A aplicação aceita até cinco arquivos por envio: vídeos MP4 de até `300 MiB` cada e os demais formatos de até `20 MiB` cada. O limite total inclui margem para os campos e cabeçalhos multipart.

O Nginx existente na VPS deve encaminhar para `http://127.0.0.1:8088` (ou a porta definida em `PORTAL_PORT`). Use um bloco exclusivo para este domínio, mantendo os blocos dos outros dois sistemas. O arquivo `nginx-memoriasolindanova.conf` é o bloco HTTP inicial; `nginx-https.conf.example` mostra a configuração final para certificados nos caminhos padrão do Let's Encrypt.

Se o proxy está em outro container, `127.0.0.1` aponta para o próprio container do proxy. Nesse caso, conecte o portal a uma rede Docker compartilhada com o proxy e use o nome de rede do portal, porta `80`, como destino. Não é necessário publicar a API Java nem as portas do MinIO na internet.

## Preparar os arquivos e as credenciais

Instale Docker Engine e o plugin Docker Compose na VPS. Mantenha o projeto em uma pasta fixa, como `/opt/maranhao-de-memorias`, para manter o nome do projeto Compose e os volumes consistentes entre atualizações. Preserve `infra/minio/`: ele contém o Dockerfile usado para compilar o MinIO e o cliente.

Na raiz do projeto **na VPS**:

```bash
cp .env.example .env
chmod 600 .env
```

Preencha `DATABASE_PASSWORD`, `ADMIN_USER`, `ADMIN_PASSWORD`, `S3_ACCESS_KEY` e `S3_SECRET_KEY` com credenciais próprias do servidor. A senha administrativa precisa de pelo menos 12 caracteres. Mantenha essas credenciais estáveis entre reinicializações; alterar `DATABASE_PASSWORD` no `.env` não troca a senha de um banco que já foi inicializado.

Configure também:

```dotenv
SECURE_COOKIES=true
S3_BUCKET=memorias
PORTAL_PORT=8088
DOCKER_LIBRARY_REGISTRY=docker.io/library
```

Como a VPS já hospeda outros sistemas, verifique antes se as portas locais `8088`, `9000` e `9001` estão livres:

```bash
sudo ss -ltnp '( sport = :8088 or sport = :9000 or sport = :9001 )'
```

Se alguma estiver ocupada, altere `PORTAL_PORT`, `MINIO_API_PORT` ou `MINIO_CONSOLE_PORT` no `.env`. Se alterar `PORTAL_PORT`, ajuste também o `proxy_pass` no arquivo Nginx. As portas internas dos containers continuam sendo `80`, `9000` e `9001`.

Use `mirror.gcr.io/library` se o acesso ao Docker Hub da VPS precisar do cache do Google. A aplicação e o bucket usam os endereços internos já definidos no Compose. O domínio não precisa ser colocado no endereço do PostgreSQL ou do MinIO.

## Iniciar e verificar

Execute todos os comandos de administração na mesma pasta e com os mesmos dois arquivos Compose:

```bash
docker compose -f compose.yaml -f compose.vps.yaml config --quiet
docker compose -f compose.yaml -f compose.vps.yaml up -d --build
docker compose -f compose.yaml -f compose.vps.yaml ps -a
docker compose -f compose.yaml -f compose.vps.yaml logs --tail=100 api portal minio create-bucket
```

O primeiro build precisa de acesso aos registros de imagens, Maven Central, npm e serviços de módulos Go usados pelo MinIO. O serviço `create-bucket` deve terminar com código `0`; os outros devem permanecer em execução. O portal deverá ficar `healthy` após a API iniciar.

## Ativar o domínio no Nginx existente

Depois de apontar o DNS e iniciar os containers, instale apenas o novo bloco. Em instalações que usam `sites-available` e `sites-enabled` (como Ubuntu/Debian), execute na raiz do projeto:

```bash
sudo cp infra/nginx-memoriasolindanova.conf /etc/nginx/sites-available/memoriasolindanova.com.br
sudo ln -s /etc/nginx/sites-available/memoriasolindanova.com.br /etc/nginx/sites-enabled/memoriasolindanova.com.br
sudo nginx -t && sudo systemctl reload nginx
```

Se sua instalação usa `conf.d`, coloque o arquivo somente nesse diretório, com extensão `.conf`, e valide antes de recarregar. Se já houver configuração para este domínio, edite o bloco existente em vez de criar outro. Não substitua os arquivos dos outros sistemas.

Use o gerenciador de certificados que já atende os sites da VPS. Se ele for Certbot com o plugin Nginx instalado:

```bash
sudo certbot --nginx -d memoriasolindanova.com.br --redirect
sudo nginx -t && sudo systemctl reload nginx
sudo certbot renew --dry-run
```

O Certbot obtém o certificado e adapta o bloco do domínio para HTTPS. Confirme que a renovação automática já está agendada pelo serviço/timer da instalação. Se preferir configurar o TLS manualmente depois de obter o certificado, use `nginx-https.conf.example`; não ative seus caminhos de certificado antes de os arquivos existirem.

O bloco inicial atende HTTP até o certificado ser instalado. O login administrativo na configuração VPS deve ser testado somente depois de o HTTPS estar ativo.

Valide a conexão interna e depois o domínio:

```bash
curl -f http://127.0.0.1:8088/api/collections
curl -I http://memoriasolindanova.com.br
curl -f https://memoriasolindanova.com.br/api/collections
```

O teste HTTP local verifica a API; para testar login, use o domínio com HTTPS, pois os cookies de sessão da VPS são seguros.

Confira login, envio de contribuição com anexo, revisão, publicação, consulta e download do arquivo. Confirme que o redirecionamento HTTP leva a HTTPS. Reinicie os containers e confirme que os dados permanecem disponíveis.

## Persistência e operação

Os volumes `database` e `objects` armazenam, respectivamente, PostgreSQL e anexos do MinIO. Enviar apenas o código para a VPS cria um ambiente novo: os dados cadastrados no computador local não são transferidos automaticamente. Caso queira preservá-los, faça a migração do banco e dos objetos em conjunto.

Configure backups regulares do banco (com `pg_dump`) e dos objetos do MinIO, com cópia fora da VPS, e teste a restauração. Preserve a correspondência entre o banco e os arquivos no backup. `docker compose down -v` apaga os volumes.

Mantenha abertas externamente as portas necessárias ao proxy (`80` e `443`) e ao SSH. PostgreSQL e API não têm portas publicadas; portal e MinIO permanecem acessíveis apenas na interface local da VPS. Para abrir o console do MinIO no seu computador, use um túnel SSH:

```bash
ssh -L 9001:127.0.0.1:9001 usuario@IP_DA_VPS
```

Com o túnel aberto, acesse `http://localhost:9001`. Adapte a porta remota se tiver alterado `MINIO_CONSOLE_PORT`.

Depois de atualizar o código, reaplique `docker compose -f compose.yaml -f compose.vps.yaml up -d --build`. Um `restart` isolado não aplica mudanças em variáveis ou no Compose.

O MinIO usado aqui é compilado a partir das versões fixadas no projeto. A limitação de manutenção da edição comunitária está descrita no README; a configuração de publicação não atualiza essas versões.

## Upload retorna 413

O Nginx instalado na VPS e o Nginx do container são configurações independentes. Atualizar o código e reconstruir o portal não altera o arquivo ativo em `/etc/nginx`. Sem `client_max_body_size` configurado, o Nginx limita o envio a 1 MiB, antes de a requisição chegar à API.

Confira os blocos ativos, incluindo o endereço com `www`:

```bash
sudo nginx -T 2>&1 | grep -nE 'configuration file|server_name|client_max_body_size|proxy_read_timeout'
```

No arquivo ativo deste domínio (por exemplo, `/etc/nginx/sites-available/memoriasolindanova.com.br`), acrescente ou atualize `client_max_body_size 1505m;` dentro do bloco HTTPS (`server` com `listen 443 ssl`). Mantenha `proxy_read_timeout 600s;` no servidor ou na localização que encaminha para o portal. Se houver limite menor em `location /` ou `location /api/`, atualize-o também, pois ele substitui o valor do servidor. Preserve as diretivas de certificado e os blocos dos outros sites.

```bash
sudo nginx -t && sudo systemctl reload nginx
docker compose -f compose.yaml -f compose.vps.yaml exec -T portal nginx -T 2>&1 | grep -nE 'client_max_body_size|proxy_read_timeout'
```

O Nginx do container também deve mostrar `1505m` e `600s`. Se ainda mostrar valores antigos, reconstrua os serviços com `docker compose -f compose.yaml -f compose.vps.yaml up -d --build`. Teste novamente o upload com o mesmo domínio que o navegador utiliza.
