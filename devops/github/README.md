# GitHub Tools

Automação idempotente do GitHub Project V2 do Corely.

## Instalação

```bash
python -m venv .venv
pip install -r requirements.txt
```

## Configuração

Variáveis obrigatórias:

- `GITHUB_TOKEN`
- `GITHUB_ORG`
- `PROJECT_NAME`

Exemplo:

```bash
GITHUB_TOKEN=seu_token
GITHUB_ORG=LiteEarn
PROJECT_NAME=Corely
```

## Execução

```bash
python devops/github/create_project.py
```

O fluxo:

- cria o Project V2 se não existir
- atualiza o Project se já existir
- cria ou atualiza campos sem duplicar
- garante labels e milestones
- atualiza views do board
- cria/atualiza épicos
- sincroniza stories existentes
- atualiza o template de issue

## Como evoluir

### Novos campos

Edite `FIELD_SPECS` em `devops/github/create_project_fields.py`.

### Novos labels

Edite `LABELS` em `devops/github/create_labels.py`.

### Novos épicos

Edite `EPICS` em `devops/github/create_epics.py`.

### Atualizar o board

Edite as views em `devops/github/configure_project.py` e as colunas em `FIELD_SPECS`.

### Reexecutar sem duplicar

Rode novamente `python devops/github/create_project.py`.
O fluxo valida o estado atual e só cria o que estiver faltando.
