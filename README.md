# Maranhão de Memórias

Portal do Acervo Digital de Olinda Nova do Maranhão. O frontend Angular e a API Spring Boot estão no mesmo projeto, em `frontend/` e `backend/`.

## Executar com Docker

1. Instale o Docker Desktop com Docker Compose e inicie o Docker.
2. Copie `.env.example` para `.env` se ainda não existir. Preencha `DATABASE_PASSWORD`, `ADMIN_PASSWORD`, `S3_ACCESS_KEY` e `S3_SECRET_KEY`. Use uma senha administrativa de pelo menos 12 caracteres. As variáveis `S3_` configuram o MinIO local; não é necessário usar Amazon S3. O usuário do MinIO deve ter pelo menos 3 caracteres, e o segredo, pelo menos 8.
3. Coloque as três logomarcas originais em `frontend/public/identidade/`, com os nomes indicados no arquivo da pasta.
4. Na raiz do projeto, execute `docker compose config --quiet` e depois `docker compose up -d --build`.
5. Acesse `http://localhost:8088` e use **Entrar** para acessar a administração.

O console do MinIO fica em `http://localhost:9001`: entre com `S3_ACCESS_KEY` e `S3_SECRET_KEY` do `.env`. A porta `9000` é a API de armazenamento, não o console. Ambas são publicadas apenas na interface local; ajuste `MINIO_API_PORT` e `MINIO_CONSOLE_PORT` se já estiverem ocupadas. Entre containers, a API usa sempre `http://minio:9000`, independentemente dessas portas. O bucket definido por `S3_BUCKET` (padrão `memorias`) é criado automaticamente; `MC_HOST_STORAGE` não é mais necessário.

Para HTTP local, mantenha `SECURE_COOKIES=false`. Verifique o estado com `docker compose ps -a` e os erros com `docker compose logs --tail=100 api minio create-bucket`. O serviço `create-bucket` terminar com código 0 é esperado; os demais devem continuar em execução. Os arquivos já salvos em `local-data/files` não são migrados automaticamente para o MinIO.

O Compose usa PostgreSQL e MinIO com volumes persistentes. O Nginx atende somente pela interface local. Para HTTPS, adapte `infra/nginx-https.conf.example` ao domínio e certificado do servidor, e use `SECURE_COOKIES=true` no `.env`.

### Erro `401 UNAUTHORIZED` ao baixar MinIO

As imagens antigas `quay.io/minio/minio` e `quay.io/minio/mc` usadas anteriormente não estão acessíveis anonimamente. O projeto MinIO passou a distribuir a edição comunitária como código-fonte ([orientação oficial](https://github.com/minio/minio#source-only-distribution)). Fazer login no registro não é necessário para esta configuração.

O Compose agora compila o servidor e o cliente a partir dos módulos oficiais do MinIO, usando `infra/minio/Dockerfile`. As versões permanecem fixadas em `RELEASE.2025-04-22T22-12-26Z` (servidor) e `RELEASE.2025-04-16T18-13-26Z` (cliente). O primeiro build demora mais e precisa acessar o proxy de módulos Go (`proxy.golang.org` e os servidores de download indicados por ele) e o serviço de verificação `sum.golang.org`. Não é necessário instalar Go no Windows. Os volumes existentes são reutilizados.

Execute `docker compose up -d --build` para construir as imagens locais e iniciar os serviços. O MinIO comunitário está arquivado e sem manutenção; a compilação dessas versões resolve a disponibilidade das imagens, mas não atualiza o servidor para uma versão mantida.

### Timeout ao baixar imagens ou dependências

Erros `TLS handshake timeout` ao acessar `registry-1.docker.io` acontecem antes de a aplicação iniciar. As mensagens `No such image` e `Interrupted` podem ser consequência do download interrompido. Não é necessário apagar containers ou volumes para corrigir esse problema.

Para PostgreSQL e as imagens base dos Dockerfiles, o registro padrão é `docker.io/library`. Se o acesso ao Docker Hub estiver falhando, configure no `.env`:

```dotenv
DOCKER_LIBRARY_REGISTRY=mirror.gcr.io/library
```

Essa opção usa o cache público do Google para imagens do Docker Hub e vale tanto para PostgreSQL quanto para Maven, Java, Node, Nginx, Go e Alpine. Ela depende da disponibilidade das imagens no cache e não substitui a conexão aos serviços de módulos Go, ao Maven Central e ao npm. Para voltar ao registro padrão, use `DOCKER_LIBRARY_REGISTRY=docker.io/library`.

Verifique a configuração e execute novamente:

```powershell
docker compose config --quiet
docker compose pull postgres
docker compose up -d --build
```

Se também ocorrer `ETIMEDOUT` em `registry.npmjs.org` ou timeout no Maven Central, teste outra conexão (por exemplo, o roteador do celular) para comparar com a rede atual e confira as configurações de proxy do Docker Desktop. Esse cenário exige resolver a conectividade antes de concluir o build.

## Publicar na VPS

Para publicar com o domínio `memoriasolindanova.com.br` e o proxy HTTPS existente na VPS, siga [o roteiro de implantação](infra/VPS.md). A configuração adicional está em `compose.vps.yaml`; execute-a em conjunto com `compose.yaml` somente no servidor com HTTPS.

## Desenvolver no Windows

É necessário Java 21, PostgreSQL e Node.js 20.19 ou superior. A aplicação usa PostgreSQL em todos os perfis. O Maven Wrapper incluído em `backend/` baixa a versão necessária do Maven na primeira execução, sem exigir o comando `mvn` no PATH.

Configure uma vez:

1. Mantenha o PostgreSQL em execução e crie o banco `memorias`, caso ainda não exista. O Flyway cria e atualiza as tabelas automaticamente.
2. Copie `.env.example` para `.env` na raiz, se ainda não houver um `.env`. Informe `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` e uma `ADMIN_PASSWORD` de pelo menos 12 caracteres, sem aspas. O padrão de conexão é `jdbc:postgresql://localhost:5432/memorias`, com usuário `postgres`.
3. Para executar localmente com HTTP, mantenha `SECURE_COOKIES=false`. Deixe `S3_ENDPOINT` vazio para salvar os anexos em `./local-data/files`, relativo à pasta de trabalho.
4. Abra `backend/src/main/java/br/org/memorias/Application.java` e execute o método `main` pelo botão **Run/Debug** da IDE. Use a raiz do projeto ou `backend/` como pasta de trabalho. Não é necessário ativar um perfil.

O Spring carrega o `.env` automaticamente a cada inicialização. Variáveis de ambiente e argumentos da IDE têm prioridade sobre o arquivo. O `.env` está ignorado pelo Git; as credenciais ficam salvas apenas na sua máquina.

Frontend, com proxy para a API em `localhost:8080`:

```powershell
Set-Location frontend
npm install
npm start
```

Acesse `http://localhost:4200`. O mapa usa Leaflet e os tiles do OpenStreetMap e precisa de acesso à internet no navegador.

### Erros ao executar localmente

- **Erro 500 em `/api/...`:** mantenha o backend em execução e aguarde a mensagem `Started Application`. O frontend encaminha essas chamadas para `http://localhost:8080`; se a API estiver desligada, o proxy do Angular retorna 500. Verifique diretamente `http://localhost:8080/api/collections`: a resposta deve ser um JSON com as coleções. Se a API não iniciar, confira o erro no terminal do backend, a conexão PostgreSQL e a senha administrativa de pelo menos 12 caracteres no `.env`.
- **Erro 404 em `/identidade/...`:** as imagens institucionais originais precisam ser copiadas para `frontend/public/identidade/` com os nomes `logo_centro_educa_mais.jpeg`, `logo_maranhao.jpeg` e `ure_viana.jpeg`. O arquivo `LEIA-ME.txt` não substitui essas imagens. Sem elas, o rodapé exibe os nomes das instituições.

## Fluxo de publicação

O banco começa com as seis coleções e nenhum registro histórico. O formulário público gera um protocolo e envia o material para moderação. O administrador também pode cadastrar material em **Administração → Acervo → Cadastrar registro** e seguir pelo link **Revisar e publicar**. Em **Contribuições**, a equipe analisa o conteúdo e os anexos, preenche os metadados e aprova ou recusa. Ao aprovar, o material passa a aparecer no acervo, no mapa quando tem coordenadas e na linha do tempo quando tem ano histórico.

Os anexos passam pela API. Arquivos de contribuições pendentes ou recusadas exigem acesso administrativo. A conta administrativa é provisionada por variáveis de ambiente.

Cada envio aceita até cinco arquivos: vídeos MP4 de até 300 MB cada e imagens, documentos e áudios de até 20 MB cada. A API verifica o formato pelo conteúdo do arquivo. Os limites do formulário, da API e dos exemplos de Nginx estão alinhados para permitir esse envio; ao atualizar a VPS, aplique também os limites no proxy HTTPS existente, conforme [o roteiro de implantação](infra/VPS.md).

No cadastro administrativo de registros, somente **Tipo de material**, **Coleção** e **Título** são obrigatórios. Os demais campos e os anexos são opcionais. Se informado, o e-mail deve ser válido. **Local** e **Origem do material** oferecem sugestões cadastradas em **Administração → Locais e origens**, sem repetições, e também permitem digitar novos valores. O formulário público mantém suas regras de contribuição e autorização.

Em **Locais e origens**, a equipe inclui, edita e remove as opções de cada lista. A atualização do banco importa os locais e origens existentes nos registros publicados e pendentes. As listas ficam disponíveis no cadastro e na edição de registros; o link **Cadastrar locais e origens** abre a gestão em outra aba, e **Atualizar listas** carrega as alterações sem apagar o formulário em preenchimento. Alterar ou remover uma opção não modifica os registros já salvos. Valores digitados livremente nos registros não são incluídos automaticamente nas listas; cadastre-os nessa área para reutilização.

## Escopo da entrega

A interface pública, a moderação, o painel com dados persistidos e o relatório CSV compõem a aplicação. Em **Administração → Acervo**, é possível editar os metadados de registros publicados. As coordenadas e o ano histórico do registro determinam sua presença no mapa e na linha do tempo. A página de Usuários informa como a conta administrativa é provisionada.

Em **Administração → Coleções**, a equipe cadastra e edita títulos, descrições e fotos de capa. Ao deixar a seleção vazia, a coleção usa automaticamente a primeira imagem disponível em seus registros publicados. Em **Administração → Configurações**, a equipe edita os textos da página inicial, a foto de destaque, as páginas Sobre e Guardiões e o e-mail público de contato. A foto de destaque e as capas podem ser escolhidas pelo título de um registro publicado com imagem.

Quando não há uma foto de destaque selecionada, a capa da página inicial exibe a fotografia original de Olinda Nova do Maranhão em `frontend/public/imagens/capa.jpeg`, na moldura ao lado do texto principal.

As consultas e transações da API usam Spring JDBC, com Flyway para o esquema.

### História da cidade

A página pública **História da cidade** (`/historia-da-cidade`) reúne textos sobre história, tradições e cultura, bandeira, hino e outras informações de Olinda Nova do Maranhão. O acesso não exige login e está disponível no menu, na página inicial e no rodapé. Os visitantes podem filtrar por tema e período, ordenar dos mais antigos aos mais recentes ou no sentido inverso e agrupar os conteúdos por período. A linha do tempo do acervo continua disponível e também permite inverter a ordem cronológica.

Em **Administração → História da cidade**, a equipe cadastra, edita e remove os conteúdos. Ao salvar, a informação fica pública imediatamente. Cada conteúdo tem título, tema, texto, período, datas de referência opcionais e fontes. O período pode ser aproximado; as datas de referência determinam a ordenação, e os conteúdos sem data ficam ao final. Os grupos de períodos são ordenados pela menor data inicial de cada grupo. É possível vincular um registro publicado do acervo para exibir sua imagem e consultar seus arquivos, incluindo áudio do hino e documentos. A migração Flyway cria o espaço vazio, sem inserir informações históricas não verificadas.

### Arquivos institucionais

- [Centro Educa Mais Professor José Ribamar Santos Silva](https://chatgpt.com/library?file_id=libfile_0bfaef5e0c048191b45bd78cb339bf55&account_id=personal)
- [Governo do Maranhão / SEDUC](https://chatgpt.com/library?file_id=libfile_7ccf2305b7fc8191b462231e2054a51b&account_id=personal)
- [URE Viana](https://chatgpt.com/library?file_id=libfile_9a57344a360c819198c37522487c0038&account_id=personal)

### Verificar a implementação

- Em `backend/`, execute `.\mvnw.cmd verify` no Windows ou `./mvnw verify` no Linux/macOS. Os testes usam a mesma conexão PostgreSQL configurada no `.env`, com um schema temporário exclusivo por contexto, removido ao final. O usuário precisa ter permissão `CREATE` no banco. As tabelas e os registros da aplicação ficam separados dos testes; não é necessário instalar outro banco.
- Em `frontend/`, execute `npm run build` para compilar o Angular e validar seus templates.
- Em `frontend/`, execute `npm test` para verificar a ordenação por datas e períodos, incluindo conteúdos sem data.
