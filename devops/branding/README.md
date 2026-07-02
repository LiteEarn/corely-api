# Branding Lab

Ferramenta em Python para gerar, validar e pontuar nomes de marca.

## Execucao

```bash
python devops/branding/generator.py
```

O fluxo executa:

1. gera 500 nomes por estilo
2. valida automaticamente
3. pontua automaticamente
4. monta o ranking
5. atualiza `finalists.yaml`

## Estrutura de dados

- `devops/branding/data/prefixes.yaml`
- `devops/branding/data/middles.yaml`
- `devops/branding/data/suffixes.yaml`
- `devops/branding/data/blacklist.yaml`
- `devops/branding/data/styles.yaml`
- `devops/branding/data/finalists.yaml`

## Como criar novos estilos

Edite `devops/branding/data/styles.yaml` e adicione um novo item em `styles` com:

- `name`
- `slug`
- `selector`
- `patterns`
- `preferred_letters`
- `preferred_endings`
- `min_length`
- `max_length`

## Como criar novos fonemas

Edite os arquivos:

- `prefixes.yaml`
- `middles.yaml`
- `suffixes.yaml`

Inclua fragmentos curtos e pronunciaveis. A rotina fonetica elimina automaticamente nomes com:

- tres consoantes consecutivas
- repeticoes excessivas
- clusters dificeis
- menos de 5 letras
- mais de 10 letras

## Como criar blacklist

Edite `devops/branding/data/blacklist.yaml` e adicione palavras que nao devem aparecer nos nomes gerados.

## Como interpretar o ranking

- `results.csv`: base completa com score e status de validacao
- `results.md`: resumo geral
- `ranking.md`: ranking completo ordenado por score
- `Top100.md`: top 100 candidatos

O ranking mostra apenas triagem automatica.

## Regras de score

- Pronuncia: 0 a 20
- Memorizacao: 0 a 20
- Comprimento: 0 a 15
- Sonoridade: 0 a 20
- Originalidade: 0 a 15
- Estilo: 0 a 10

O score final e uma normalizacao do total dos componentes para caber na escala de 0 a 100.

Nao foram encontrados conflitos aparentes durante a triagem automatica.

A utilizacao comercial exige consulta oficial ao INPI.

## Finalistas

Quando um nome atingir score maior ou igual a 90 e `GitHub = AVAILABLE`, ele sera salvo automaticamente em `devops/branding/data/finalists.yaml` sem duplicacao.
