# Branding Tools

Ferramenta em Python para triagem automatica de nomes de empresa.

## Instalacao

```bash
python -m venv .venv
pip install -r devops/branding/requirements.txt
```

## Execucao

```bash
python devops/branding/check_names.py
```

## Como adicionar novos nomes

Edite `devops/branding/candidates.yaml` e inclua os nomes em `candidates` ou `company_names`:

```yaml
company_names:
  - NomeUm
  - NomeDois
```

## Saidas geradas

- `results.csv` com: Nome, GitHub, .com, .com.br, .io, Risco e Score.
- `results.md` ordenado do maior score para o menor.

## Critério de pontuacao

- GitHub disponivel: +25
- .com disponivel: +30
- .com.br disponivel: +20
- .io disponivel: +10
- Risco baixo: +15
- Risco medio: +7
- Risco alto: +0

## Observacoes

Nao foram encontrados conflitos aparentes durante a triagem.

E obrigatoria a consulta oficial ao INPI antes da adocao definitiva da marca.
