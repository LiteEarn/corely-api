"""Cria os épicos do Corely como issues no GitHub."""

from __future__ import annotations

import logging
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.github_client import GitHubApiError
from devops.github.runtime import build_runtime_context


logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class EpicSpec:
    title: str
    objective: str
    description: str
    acceptance_criteria: tuple[str, ...]
    labels: tuple[str, ...]


EPICS: tuple[EpicSpec, ...] = (
    EpicSpec(
        title="EPIC - Fundação",
        objective="Estabelecer a base técnica e organizacional do Corely.",
        description="Estrutura inicial para suportar o restante da entrega.",
        acceptance_criteria=("Estrutura base validada.", "Dependências principais organizadas."),
        labels=("epic", "tech-debt"),
    ),
    EpicSpec(
        title="EPIC - Cadastros",
        objective="Centralizar os cadastros principais do sistema.",
        description="Fluxos de cadastro de entidades essenciais do domínio.",
        acceptance_criteria=("Cadastros principais disponíveis.", "Validações essenciais aplicadas."),
        labels=("epic", "backend", "frontend"),
    ),
    EpicSpec(
        title="EPIC - Agenda Operacional",
        objective="Organizar a agenda operacional dos estúdios.",
        description="Fluxos de agenda, recorrência e acompanhamento operacional.",
        acceptance_criteria=("Agenda operacional visível.", "Regras de agenda aplicadas."),
        labels=("epic", "dashboard", "backend", "frontend"),
    ),
    EpicSpec(
        title="EPIC - Reposições",
        objective="Gerenciar reposições e compensações de aulas.",
        description="Fluxos de reposição para alunos e instrutores.",
        acceptance_criteria=("Solicitações de reposição suportadas.", "Fluxo de aprovação definido."),
        labels=("epic", "backend", "frontend"),
    ),
    EpicSpec(
        title="EPIC - Dashboard",
        objective="Consolidar indicadores operacionais e financeiros.",
        description="Visões gerenciais para acompanhamento da operação.",
        acceptance_criteria=("Métricas principais exibidas.", "Indicadores atualizados."),
        labels=("epic", "dashboard", "frontend"),
    ),
    EpicSpec(
        title="EPIC - Financeiro",
        objective="Cobrir os fluxos financeiros do Corely.",
        description="Rotinas de cobrança, recebimento e acompanhamento financeiro.",
        acceptance_criteria=("Fluxos financeiros cobertos.", "Visões financeiras consistentes."),
        labels=("epic", "finance", "backend", "frontend"),
    ),
    EpicSpec(
        title="EPIC - Relatórios",
        objective="Fornecer relatórios operacionais e financeiros.",
        description="Exportações e consultas consolidadas para gestão.",
        acceptance_criteria=("Relatórios principais disponíveis.", "Dados consolidados corretamente."),
        labels=("epic", "dashboard", "backend", "frontend"),
    ),
    EpicSpec(
        title="EPIC - Mobile",
        objective="Cobrir as necessidades do uso mobile.",
        description="Funcionalidades priorizadas para o aplicativo mobile.",
        acceptance_criteria=("Fluxos mobile priorizados.", "Experiência mobile validada."),
        labels=("epic", "frontend", "ux"),
    ),
    EpicSpec(
        title="EPIC - IA",
        objective="Adicionar recursos com inteligência artificial.",
        description="Casos de uso orientados por IA para ganho operacional.",
        acceptance_criteria=("Casos de uso de IA definidos.", "Integração planejada."),
        labels=("epic", "ai", "backend"),
    ),
    EpicSpec(
        title="EPIC - Infraestrutura",
        objective="Fortalecer a infraestrutura do Corely.",
        description="Base de segurança, confiabilidade e manutenção técnica.",
        acceptance_criteria=("Infraestrutura mapeada.", "Pontos críticos priorizados."),
        labels=("epic", "tech-debt", "security", "backend"),
    ),
)


def _list_milestones(client, owner: str, repo: str) -> dict[str, int]:
    milestones: dict[str, int] = {}
    page = 1
    while True:
        data = client.execute_rest(
            "GET",
            f"/repos/{owner}/{repo}/milestones?state=all&per_page=100&page={page}",
        )
        if not data:
            break
        for item in data:
            milestones[item["title"]] = int(item["number"])
        if len(data) < 100:
            break
        page += 1
    return milestones


def ensure_milestone_number(client, owner: str, repo: str, title: str) -> int:
    milestones = _list_milestones(client, owner, repo)
    if title in milestones:
        return milestones[title]

    data = client.execute_rest(
        "POST",
        f"/repos/{owner}/{repo}/milestones",
        {"title": title, "description": f"Milestone {title} do Corely.", "state": "open"},
    )
    return int(data["number"])


def list_existing_issues(client, owner: str, repo: str) -> set[str]:
    issues: set[str] = set()
    page = 1
    while True:
        data = client.execute_rest("GET", f"/repos/{owner}/{repo}/issues?state=all&per_page=100&page={page}")
        if not data:
            break
        for item in data:
            if "pull_request" in item:
                continue
            issues.add(item["title"])
        if len(data) < 100:
            break
        page += 1
    return issues


def build_body(spec: EpicSpec) -> str:
    lines = [
        "## Objetivo",
        spec.objective,
        "",
        "## Descrição",
        spec.description,
        "",
        "## Critérios de aceite",
    ]
    lines.extend(f"- {criterion}" for criterion in spec.acceptance_criteria)
    lines.extend(["", "## Labels", ", ".join(spec.labels), "", "## Milestone", "MVP"])
    return "\n".join(lines)


def create_epic_issue(client, owner: str, repo: str, spec: EpicSpec, milestone_number: int) -> dict[str, Any]:
    return client.execute_rest(
        "POST",
        f"/repos/{owner}/{repo}/issues",
        {
            "title": spec.title,
            "body": build_body(spec),
            "labels": list(spec.labels),
            "milestone": milestone_number,
        },
    )


def main() -> int:
    try:
        runtime = build_runtime_context()
        existing_issues = list_existing_issues(runtime.client, runtime.repository.owner, runtime.repository.name)
        mvp_number = ensure_milestone_number(runtime.client, runtime.repository.owner, runtime.repository.name, "MVP")
        created = 0
        ignored = 0

        for epic in EPICS:
            if epic.title in existing_issues:
                ignored += 1
                continue
            create_epic_issue(runtime.client, runtime.repository.owner, runtime.repository.name, epic, mvp_number)
            created += 1
            logger.info("Épico criado: %s", epic.title)

        logger.info("Resumo épicos: criados=%s ignorados=%s", created, ignored)
        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
