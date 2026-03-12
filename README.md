# NutriTrack (MVC + DAO em Arquivo Binário)

Projeto exemplo para CRUD de **gerenciamento de valor nutricional consumido** usando:

- MVC + DAO
- Persistência em **arquivos binários** com **cabeçalho** (último ID + ponteiro lista removidos) e **lápide** (tombstone)
- Mínimo 3 "tabelas" (arquivos): **Alimento**, **Refeicao**, **Consumo**
- Relacionamentos:
  - 1:N -> Refeicao (1) : (N) Consumo
  - N:N -> Refeicao (N) <-> (N) Alimento via Consumo

## Tipos exigidos
- Data: `Refeicao.data` (LocalDate armazenada como `epochDay`)
- Real (float): macros e `Consumo.quantidadeGramas`
- String: nome/tipo/observacao
- String multivalorada: `Alimento.tags` (lista)

## Estrutura
- `src/app/model` -> Model (domínio)
- `src/app/dao` -> DAO (acesso ao arquivo binário)
- `src/app/controller` -> regras/validações (FK, etc.)
- `web/` -> View mínima (HTML/CSS)

## Como compilar e executar (exemplo)
Em um terminal na raiz do projeto:

```bash
mkdir -p out
javac -d out src/app/*.java src/app/model/*.java src/app/dao/*.java src/app/controller/*.java
java -cp out app.Main
```

Os arquivos serão criados em `./dados/`.

> Dica: apague a pasta `./dados/` para começar do zero.

## Modo de teste rápido (Main)
Ao executar `app.Main`, o programa pede os dados no console para criar 2 alimentos, 1 refeição e 2 consumos, e depois faz leituras para mostrar os registros.

Se você preferir um menu completo de CRUD, execute `app.ConsoleApp`.

## Tabela adicional: Usuario
Foi adicionada a tabela/arquivo `Usuario` (`./dados/usuario/usuario.db`) para suportar a FK `Refeicao.usuarioId`.
