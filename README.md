# NutriTrack — TP AEDS III (Fases 1 e 2)

Sistema de **gerenciamento de consumo nutricional** com persistência em **arquivos
binários** (cabeçalho + lápide + lista de espaços livres) e **índices em Hash Extensível**.

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

---

##  Estrutura

```
TP-AEDS-III/
├── dados/                                # gerado em runtime
│   ├── usuario/usuario.db + usuario.idx.{d,c}.db
│   ├── alimento/alimento.db + ...
│   ├── refeicao/refeicao.db + ...
│   ├── consumo/consumo.db + ...
│   └── consumo_por_refeicao/consumo_por_refeicao.idx.{d,c}.db   # índice 1:N
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
│       ├── ParIDID.java                  # entrada do índice 1:N
│       └── *DAO.java
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
