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

## Scripts

- `create_project.py`: cria o Project V2 da organização.
- `create_labels.py`: cria os labels padrão do Corely.
- `create_milestones.py`: cria os milestones padrão.
- `create_epics.py`: cria os épicos como issues no repositório.
- `create_project_fields.py`: cria os campos do Project V2.
- `configure_project.py`: cria as views e configura o Status.
- `add_epics_to_project.py`: adiciona os épicos ao Project e os move para Backlog.
- `bootstrap.py`: executa toda a sequência automaticamente.

## Execução

Executar um script específico:

```bash
python devops/github/create_project.py
```

Executar a automação completa:

```bash
python devops/github/bootstrap.py
```

Se um recurso já existir, o script apenas ignora e continua.
