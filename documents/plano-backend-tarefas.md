# Plano de desenvolvimento do backend de tarefas

## Objetivo

Planejar o backend da parte de tarefas do Mais Um Todo, cobrindo somente o que e necessario para uma lista de tarefas simples organizada por projetos.

Este plano nao cobre cadastro, login, sessao, senha, token, permissoes ou qualquer outro fluxo de autenticacao.

## Escopo

O backend de tarefas precisa permitir:

- Criar, listar, editar e excluir projetos.
- Criar, listar, editar e excluir tarefas dentro de um projeto.
- Exibir as tarefas de um projeto em formato de lista.
- Exibir as mesmas tarefas em formato de kanban.
- Mudar o status de uma tarefa entre as colunas do kanban.
- Manter uma ordenacao simples das tarefas.
- Persistir os dados em MySQL usando migrations controladas.

Mesmo sendo chamado de backend de tarefas, projetos fazem parte deste planejamento porque as tarefas precisam ser organizadas por projeto.

## Decisoes tecnicas

O backend deve ser desenvolvido na raiz do projeto usando:

- Java com Quarkus.
- Gradle para dependencias e build.
- REST com JSON.
- Hibernate ORM com Panache para persistencia simples.
- MySQL como banco de dados.
- Flyway para migrations.

Nao ha necessidade de criar uma arquitetura em muitas camadas para este modulo. Como o produto e pequeno, a implementacao pode ficar simples:

- Entidades no pacote de dominio.
- Um resource REST para projetos e tarefas.
- Records internos ou classes simples para requests e responses.
- Regras pequenas dentro do proprio resource quando forem especificas daquele endpoint.

## Modelo de dados

### Projeto

Tabela: `todo_project`

Campos planejados:

- `id`: identificador interno numerico, chave primaria.
- `uuid`: identificador publico usado pela API.
- `date_created`: data de criacao.
- `last_updated`: data da ultima alteracao.
- `name`: nome do projeto.

Regras:

- `name` e obrigatorio.
- `uuid` deve ser unico.
- Ao excluir um projeto, suas tarefas tambem devem ser excluidas.

### Tarefa

Tabela: `todo_task`

Campos planejados:

- `id`: identificador interno numerico, chave primaria.
- `uuid`: identificador publico usado pela API.
- `date_created`: data de criacao.
- `last_updated`: data da ultima alteracao.
- `todo_project_id`: projeto ao qual a tarefa pertence.
- `title`: titulo da tarefa.
- `status`: status atual da tarefa.
- `sort_order`: posicao usada para ordenar a tarefa na lista e no kanban.

Regras:

- `title` e obrigatorio.
- `uuid` deve ser unico.
- Toda tarefa deve pertencer a um projeto.
- Ao criar uma tarefa sem status, o status padrao deve ser `TODO`.
- Ao criar uma tarefa sem ordenacao, a ordenacao deve ser calculada no final da lista do projeto.

### Status da tarefa

Enum planejado: `TaskStatus`

Valores:

- `TODO`: tarefa a fazer.
- `DOING`: tarefa em andamento.
- `DONE`: tarefa concluida.

Esses tres status sao suficientes para a aba de kanban prevista no escopo.

## Migration inicial

A primeira migration do modulo de tarefas deve criar as tabelas `todo_project` e `todo_task`.

Estrutura esperada:

```sql
CREATE TABLE todo_project (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uuid VARCHAR(36) NOT NULL,
    date_created DATETIME NOT NULL,
    last_updated DATETIME NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_todo_project_uuid UNIQUE (uuid)
);

CREATE TABLE todo_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uuid VARCHAR(36) NOT NULL,
    date_created DATETIME NOT NULL,
    last_updated DATETIME NOT NULL,
    todo_project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    sort_order DECIMAL(20,10) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_todo_task_uuid UNIQUE (uuid),
    CONSTRAINT fk_todo_task_project FOREIGN KEY (todo_project_id)
        REFERENCES todo_project(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_todo_task_project ON todo_task(todo_project_id);
CREATE INDEX idx_todo_task_project_status_order ON todo_task(todo_project_id, status, sort_order);
```

## API planejada

Base path: `/api`

### Projetos

`GET /api/projects`

Lista todos os projetos.

Resposta esperada:

```json
[
  {
    "uuid": "project-uuid",
    "name": "Pessoal",
    "dateCreated": "2026-06-08T10:00:00",
    "lastUpdated": "2026-06-08T10:00:00"
  }
]
```

`POST /api/projects`

Cria um projeto.

Request:

```json
{
  "name": "Pessoal"
}
```

Resposta: `201 Created` com o projeto criado.

`PUT /api/projects/{projectUuid}`

Atualiza o nome de um projeto.

Request:

```json
{
  "name": "Trabalho"
}
```

Resposta: `200 OK` com o projeto atualizado.

`DELETE /api/projects/{projectUuid}`

Remove o projeto e suas tarefas.

Resposta: `204 No Content`.

### Tarefas

`GET /api/projects/{projectUuid}/tasks`

Lista as tarefas de um projeto ordenadas por `sortOrder` e depois por data de criacao.

Resposta esperada:

```json
[
  {
    "uuid": "task-uuid",
    "projectUuid": "project-uuid",
    "title": "Comprar cafe",
    "status": "TODO",
    "sortOrder": 1,
    "dateCreated": "2026-06-08T10:00:00",
    "lastUpdated": "2026-06-08T10:00:00"
  }
]
```

`POST /api/projects/{projectUuid}/tasks`

Cria uma tarefa dentro de um projeto.

Request minimo:

```json
{
  "title": "Comprar cafe"
}
```

Request completo permitido:

```json
{
  "title": "Comprar cafe",
  "status": "TODO",
  "sortOrder": 1
}
```

Resposta: `201 Created` com a tarefa criada.

`PUT /api/tasks/{taskUuid}`

Atualiza uma tarefa.

Request para alterar titulo:

```json
{
  "title": "Comprar cafe e leite"
}
```

Request para mover no kanban:

```json
{
  "status": "DOING",
  "sortOrder": 2
}
```

Resposta: `200 OK` com a tarefa atualizada.

`DELETE /api/tasks/{taskUuid}`

Remove uma tarefa.

Resposta: `204 No Content`.

## Validacoes e erros

Validacoes minimas planejadas:

- Projeto com nome vazio deve retornar `400 Bad Request`.
- Tarefa com titulo vazio deve retornar `400 Bad Request`.
- Projeto inexistente deve retornar `404 Not Found`.
- Tarefa inexistente deve retornar `404 Not Found`.
- Status fora dos valores do enum deve retornar erro de desserializacao ou `400 Bad Request`.

Nao e necessario criar validacoes complexas neste momento, como tamanho customizado de titulo, limite de projetos ou limite de tarefas.

## Regras de ordenacao

A ordenacao deve ser simples para evitar complexidade desnecessaria.

Regras planejadas:

- Ao criar uma tarefa sem `sortOrder`, usar a proxima posicao do projeto.
- Ao listar tarefas, ordenar por `sortOrder ASC` e depois por `dateCreated ASC`.
- Ao mover uma tarefa no kanban, aceitar `status` e `sortOrder` enviados pelo frontend.
- Nao recalcular automaticamente todas as tarefas a cada movimento, a menos que isso se torne necessario para corrigir um problema real.

Essa abordagem e suficiente para uma primeira versao simples.

## Sequencia de desenvolvimento

1. Configurar dependencias do backend no Gradle: REST, Jackson, Hibernate ORM Panache, JDBC MySQL e Flyway.
2. Criar entidade base com `id`, `uuid`, `dateCreated` e `lastUpdated`, caso ainda nao exista.
3. Criar enum `TaskStatus` com `TODO`, `DOING` e `DONE`.
4. Criar entidade `TodoProject`.
5. Criar entidade `TodoTask`.
6. Criar migration inicial das tabelas de projeto e tarefa.
7. Criar endpoints REST de projetos.
8. Criar endpoints REST de tarefas.
9. Implementar validacoes minimas nos endpoints.
10. Criar testes de fluxo para projeto e tarefa.
11. Rodar o build do backend para validar compilacao e testes.

## Testes planejados

Testes principais:

- Criar projeto com sucesso.
- Listar projetos.
- Rejeitar projeto sem nome.
- Criar tarefa em um projeto existente.
- Criar tarefa sem status e confirmar status `TODO`.
- Listar tarefas de um projeto.
- Atualizar titulo da tarefa.
- Atualizar status da tarefa para `DOING`.
- Atualizar status da tarefa para `DONE`.
- Excluir tarefa.
- Excluir projeto e garantir que suas tarefas foram removidas.
- Retornar `404` ao tentar criar tarefa em projeto inexistente.

Nao e necessario testar login neste plano.

## Criterios de aceite

O backend de tarefas esta pronto quando:

- As migrations criam corretamente as tabelas no MySQL.
- Projetos podem ser criados, listados, editados e excluidos pela API.
- Tarefas podem ser criadas, listadas, editadas e excluidas pela API.
- Cada tarefa pertence a exatamente um projeto.
- A listagem de tarefas retorna dados suficientes para a lista e para o kanban.
- O kanban consegue mover tarefas apenas alterando `status` e `sortOrder`.
- Erros basicos retornam status HTTP adequados.
- Os testes automatizados principais passam.

## Observacao critica

O ponto mais importante deste planejamento e nao transformar uma To Do List simples em um sistema grande. Para o escopo definido, nao faz sentido criar modulos extras, servicos genericos, permissoes, eventos, filas ou regras antecipadas. O backend deve entregar somente projetos, tarefas, status, ordenacao e persistencia controlada por migration.
