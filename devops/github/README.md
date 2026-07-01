# GitHub Tools

Ferramenta em Python para automatizar a configuração do GitHub do Corely.

## Instalação

```bash
python -m venv .venv
pip install -r requirements.txt
```

## Configuração

Defina as variáveis de ambiente:

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

Se o Project já existir, a ferramenta exibirá `Project já existe.` e encerrará sem duplicar.
