# NutriTrack — TP AEDS III (Fases 1, 2, 3 e 4)

Sistema de **gerenciamento de consumo nutricional** com persistência em **arquivos
binários** (cabeçalho + lápide + lista de espaços livres), **índices em Hash Extensível**
e **Árvore B+** para consultas ordenadas.

> Trabalho Prático — Algoritmos e Estrutura de Dados III · PUC Minas
> Componentes do grupo: Pedro Augusto Silva Ferreira, Luis Felipe Almeida Rodrigues

---

## O que está implementado

### Fase 1
- MVC + DAO em Java puro
- 4 entidades: **Usuario**, **Alimento**, **Refeicao**, **Consumo**
- Persistência em arquivo binário com **cabeçalho** (último ID + ponteiro de removidos) e **exclusão lógica** por lápide (`'*'`)
- Lista encadeada de espaços livres com estratégia *best-fit*
- Diagramas DCU, DER e Arquitetura em [`web/documentacao.html`](web/documentacao.html)

### Fase 2
- **Índice primário** em **Hash Extensível** (id → endereço) para cada tabela — `read/update/delete` em O(1) médio
- **Relacionamento 1:N** (Refeição → Consumos) via **Hash Extensível** (FK → PK)
- **Cascade delete**: ao remover uma refeição, os consumos vinculados são apagados automaticamente
- **Validações**: PK inexistente em update/delete, FK inválida, campos obrigatórios, valores negativos
- **Front-end web** (HTML + CSS + JS) consumindo API REST JSON
- **Servidor HTTP embutido** (`com.sun.net.httpserver`, sem dependências externas) na porta `8080`
- **Reconstrução automática** dos índices se os arquivos `.idx.*.db` forem apagados

### Fase 3
- **Árvore B+ genérica** implementada do zero (`app.dao.ArvoreBMais<T>`) com folhas encadeadas, ordem **8**, página de **595 bytes**
- Persistente em `./dados/alimento/alimento.idx.bmais.db` — indexa `nome → id` do **Alimento**
- **Consulta ordenada por nome resolvida pela B+** percorrendo as folhas encadeadas — **sem ordenação em memória** (`GET /api/alimento/ordenado`)
- **Novo relacionamento N:N** — *Alimentos Favoritos* — entre **Usuário** e **Alimento**
- Tabela intermediária `Favorito` com **chave primária composta** `(usuarioId, alimentoId)` validada antes da inserção
- **Dois índices Hash Extensível bidirecionais** (`favorito_por_usuario`, `favorito_por_alimento`)
- **Cascade**: remover Usuário/Alimento remove os favoritos vinculados e suas entradas nos dois índices
- Front-end com novas telas: *Favoritos* (CRUD + filtros por usuário/alimento) e *Formulário Fase 3* (8 questões)

### Fase 4 — Compressão
- **Compressão de todos os arquivos de dados** de `./dados/` num **único arquivo compactado** que funciona como **backup completo**
- **Huffman** implementado do zero (`app.compression.Huffman`) — codificação por prefixos de comprimento variável; a tabela de frequências é persistida no próprio fluxo para reconstruir a árvore
- **LZW** implementado do zero (`app.compression.LZW`) — dicionário adaptativo com códigos de 16 bits
- **Empacotador estilo TAR** (`app.compression.Backup`) reúne todos os arquivos num só fluxo antes de comprimir; a restauração recria a árvore de diretórios
- **Verificação de integridade (round-trip)** automática a cada geração: comprime → descomprime → compara byte a byte
- Arquivos gerados em `./backups/nutritrack_huffman.huff` e `./backups/nutritrack_lzw.lzw`
- Front-end com novas telas: *Backup & Compressão* (gerar / baixar / restaurar + taxa) e *Formulário Fase 4* (3 questões)
- API REST: `/api/backup` · menu de console: opção **5) Backup / Compressão**

---

##  Estrutura

```
TP-AEDS-III/
├── dados/                                # gerado em runtime
│   ├── usuario/usuario.db + usuario.idx.{d,c}.db
│   ├── alimento/alimento.db + ...
│   ├── refeicao/refeicao.db + ...
│   ├── consumo/consumo.db + ...
│   ├── consumo_por_refeicao/consumo_por_refeicao.idx.{d,c}.db   # índice 1:N
│   ├── favorito/favorito.db + favorito.idx.{d,c}.db             # Fase 3 — tabela N:N
│   ├── favorito_por_usuario/...idx.{d,c}.db                     # Fase 3 — índice N:N
│   ├── favorito_por_alimento/...idx.{d,c}.db                    # Fase 3 — índice N:N
│   └── alimento/alimento.idx.bmais.db                           # Fase 3 — Árvore B+
├── src/app/
│   ├── Servidor.java                     # servidor HTTP embutido (porta 8080)
│   ├── Main.java                         # demo guiado (console)
│   ├── ConsoleApp.java                   # CRUD via menu (console)
│   ├── Arquivo.java                      # persistência genérica + índice primário
│   ├── Registro.java
│   ├── model/                            # Usuario, Alimento, Refeicao, Consumo
│   ├── controller/                       # validações + cascade
│   └── dao/
│       ├── HashExtensivel.java           # estrutura genérica
│       ├── RegistroHashExtensivel.java
│       ├── ParIDEndereco.java            # entrada do índice primário
│       ├── ParIDID.java                  # entrada do índice 1:N e dos N:N
│       ├── ArvoreBMais.java              # Árvore B+ genérica (Fase 3)
│       ├── RegistroArvoreBMais.java
│       ├── ParNomeID.java                # entrada da B+ (nome 60B + id 4B)
│       ├── FavoritoDAO.java              # tabela N:N + 2 índices Hash (Fase 3)
│       └── *DAO.java
├── src/app/compression/                  # Fase 4 — compressão
│   ├── Huffman.java                      # algoritmo de Huffman
│   ├── LZW.java                          # algoritmo LZW
│   ├── Backup.java                       # empacotador (TAR) de ./dados
│   ├── CompressaoService.java            # orquestra empacotar + comprimir + verificar
│   └── ResultadoCompressao.java          # tamanhos, taxa e integridade
├── backups/                              # gerado em runtime (arquivos compactados)
└── web/
    ├── index.html  app.js  styles.css    # SPA
    └── documentacao.html                 # documentação completa
```

---

##  Como executar

### Pré-requisitos
- **JDK 17+** (testado com OpenJDK 21) no PATH

### Passo a passo (Windows PowerShell)

```powershell
# 1. Compilar (gera ./out)
if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory out | Out-Null
javac -d out (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName })

# 2. Executar — escolha UMA das 3 opções:

#  (a) Servidor HTTP + front-end (RECOMENDADO)
java -cp out app.Servidor
#       depois abra: http://localhost:8080

#  (b) CRUD completo via console
java -cp out app.ConsoleApp

#  (c) Demo guiado (cria 1 usuário + 2 alimentos + 1 refeição + 2 consumos)
java -cp out app.Main
```

### Linux / macOS

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out app.Servidor   # ou app.ConsoleApp / app.Main
```

>  Para começar do zero apague a pasta `./dados/` (os arquivos serão recriados).
>  Para forçar reconstrução dos índices apague apenas os `*.idx.*.db` — eles serão recriados a partir dos `.db` de dados na próxima execução.

---

##  API REST (servidor)

| Método | Rota | Descrição |
|---|---|---|
| GET / POST | `/api/usuario` | listar / criar |
| GET / PUT / DELETE | `/api/usuario/{id}` | CRUD por ID |
| GET / POST | `/api/alimento` | listar / criar |
| GET / PUT / DELETE | `/api/alimento/{id}` | CRUD por ID |
| GET / POST | `/api/refeicao` | listar / criar (valida FK usuarioId) |
| GET / PUT / DELETE | `/api/refeicao/{id}` | CRUD por ID; *delete* faz cascade |
| **GET** | **`/api/refeicao/{id}/consumos`** | **demonstra o índice 1:N (Hash Extensível)** |
| GET / POST | `/api/consumo` | listar / criar (valida FKs) |
| GET / PUT / DELETE | `/api/consumo/{id}` | CRUD por ID |
| **GET** | **`/api/backup`** | **status (tamanho da origem e dos backups)** |
| **POST** | **`/api/backup/huffman`** | **gera o backup compactado com Huffman + taxa** |
| **POST** | **`/api/backup/lzw`** | **gera o backup compactado com LZW + taxa** |
| **GET** | **`/api/backup/download/{huffman\|lzw}`** | **baixa o arquivo único compactado** |
| **POST** | **`/api/backup/restaurar/{huffman\|lzw}`** | **restaura o backup em `./dados_restaurado/`** |

---

##  Documentação técnica

Abra [`web/documentacao.html`](web/documentacao.html) (ou clique em " Documentação"
na barra do front-end). O documento inclui:

- Descrição do problema, objetivos e requisitos (Fase 1)
- DCU, DER e diagrama de arquitetura (Mermaid renderizado)
- Estrutura do arquivo binário (cabeçalho, lápide, espaço livre)
- **Fase 2:** estrutura física do Hash Extensível, fluxo de acesso ao 1:N, persistência, reconstrução
- API REST e validações
- **Formulário de Projeto (a–h) com respostas completas**

---

##  Smoke test rápido (PowerShell, com servidor rodando)

```powershell
$u = irm -Method Post -Uri http://localhost:8080/api/usuario -ContentType 'application/json' `
        -Body '{"nome":"Maria","email":"m@x","dataNascimento":"1995-01-01","telefones":["31999"]}'
$a = irm -Method Post -Uri http://localhost:8080/api/alimento -ContentType 'application/json' `
        -Body '{"nome":"Aveia","kcalPor100g":389,"proteinaPor100g":17,"carboPor100g":66,"gorduraPor100g":7,"tags":["graos"]}'
$r = irm -Method Post -Uri http://localhost:8080/api/refeicao -ContentType 'application/json' `
        -Body ('{"usuarioId":'+$u.id+',"data":"2026-04-23","tipo":"Café da manhã"}')
$c = irm -Method Post -Uri http://localhost:8080/api/consumo  -ContentType 'application/json' `
        -Body ('{"refeicaoId":'+$r.id+',"alimentoId":'+$a.id+',"quantidadeGramas":50}')

# consulta via índice 1:N (Hash Extensível)
irm http://localhost:8080/api/refeicao/$($r.id)/consumos
```

---

##  Fase IV — Compressão (Backup com Huffman e LZW)

O sistema gera um **único arquivo compactado** com **todos** os arquivos de `./dados/`,
funcionando como backup completo. Há duas formas de usar:

### Pela interface web
1. Inicie o servidor (`java -cp out app.Servidor`) e abra `http://localhost:8080`.
2. No menu lateral, abra **Backup & Compressão**.
3. Clique em **Gerar backup Huffman** e/ou **Gerar backup LZW** — a taxa de compressão e a
   verificação de integridade aparecem na hora.
4. Use **Baixar** para obter o arquivo `.huff`/`.lzw` ou **Restaurar** para extrair em `./dados_restaurado/`.
5. A aba **Formulário Fase 4** é preenchida automaticamente com os tamanhos e a taxa.

### Pelo console
```powershell
java -cp out app.ConsoleApp
#  -> 5) Backup / Compressão (Huffman e LZW)
#     1) Gerar Huffman | 2) Gerar LZW | 3) Comparar | 4/5) Restaurar
```

### Pela API (com o servidor rodando)
```powershell
# gera e mostra a taxa
irm -Method Post http://localhost:8080/api/backup/huffman
irm -Method Post http://localhost:8080/api/backup/lzw

# baixa o arquivo único compactado
irm http://localhost:8080/api/backup/download/huffman -OutFile nutritrack_huffman.huff
irm http://localhost:8080/api/backup/download/lzw     -OutFile nutritrack_lzw.lzw

# restaura em ./dados_restaurado/
irm -Method Post http://localhost:8080/api/backup/restaurar/huffman
```

### Formulário técnico (Fase IV)

> Os valores variam conforme a quantidade de dados em `./dados/` no momento da geração.
> Os números abaixo são um exemplo real obtido com a base de demonstração (`seed.ps1`).

**1. Taxa de compressão com Huffman**
- **a) Tamanho original:** soma dos bytes do pacote com todos os arquivos de `./dados/` (ex.: `8.545 bytes`)
- **b) Tamanho comprimido:** tamanho do arquivo `nutritrack_huffman.huff` (ex.: `4.951 bytes`)
- **c) Cálculo da taxa:** `taxa = (1 − 4951 / 8545) × 100 ≈ 42,06 %`
- **d) Interpretação:** o Huffman explora a **frequência** dos bytes. Como os arquivos `.db` têm muitos
  bytes de preenchimento (lápides, espaços livres, campos fixos), há boa redundância estatística. Ele
  atinge a entropia de ordem 0, mas não captura padrões de **sequências** repetidas.

**2. Taxa de compressão com LZW**
- **a) Tamanho original:** mesmo pacote da origem (ex.: `8.545 bytes`)
- **b) Tamanho comprimido:** tamanho do arquivo `nutritrack_lzw.lzw` (ex.: `4.139 bytes`)
- **c) Cálculo da taxa:** `taxa = (1 − 4139 / 8545) × 100 ≈ 51,56 %`
- **d) Interpretação:** o LZW monta um **dicionário** de sequências recorrentes. Em arquivos estruturados
  como os `.db` (cabeçalhos, sequências de bytes nulos, strings comuns), ele substitui sequências inteiras
  por um único código, alcançando taxa **igual ou superior** à do Huffman.

**3. Dificuldades e soluções**
- **Arquivo único:** os dados estão espalhados em várias pastas → empacotador estilo TAR (`Backup.empacotar`).
- **Huffman / persistir a árvore:** grava-se a **tabela de frequências** no cabeçalho do fluxo e reconstrói-se
  a árvore de forma determinística (fila de prioridade).
- **Huffman / empacotamento de bits:** códigos de tamanho variável → *bit buffer* que descarrega de 8 em 8 bits.
- **Huffman / símbolo único:** arquivo com um só byte distinto gera árvore de um nó → tratamento especial (código “0”).
- **LZW / sincronizar dicionário:** compressor e descompressor param de crescer no mesmo limite (65.536) e tratam o caso especial *KwKwK*.
- **Integridade:** verificação **round-trip** automática (comprime → descomprime → compara) antes de gravar.

---

