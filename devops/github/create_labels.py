"""Cria labels padrão para o Corely no repositório GitHub."""

from __future__ import annotations

import logging
import sys
from dataclasses import dataclass
from pathlib import Path

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.github_client import GitHubApiError
from devops.github.runtime import build_runtime_context


logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class LabelSpec:
    name: str
    color: str
    description: str


LABELS: tuple[LabelSpec, ...] = (
    LabelSpec("backend", "0E8A16", "Tarefas relacionadas ao backend."),
    LabelSpec("frontend", "1D76DB", "Tarefas relacionadas ao frontend."),
    LabelSpec("database", "5319E7", "Tarefas relacionadas ao banco de dados."),
    LabelSpec("dashboard", "5319E7", "Tarefas relacionadas a dashboards."),
    LabelSpec("finance", "0E8A16", "Tarefas relacionadas ao financeiro."),
    LabelSpec("ux", "5319E7", "Tarefas relacionadas à experiência do usuário."),
    LabelSpec("security", "B60205", "Tarefas relacionadas à segurança."),
    LabelSpec("ai", "8250DF", "Tarefas relacionadas à inteligência artificial."),
    LabelSpec("bug", "D73A4A", "Correção de defeito."),
    LabelSpec("enhancement", "A2EEEF", "Melhoria incremental."),
    LabelSpec("tech-debt", "7057FF", "Dívida técnica."),
    LabelSpec("epic", "5319E7", "Épico do backlog."),
    LabelSpec("story", "FBCA04", "História de usuário."),
    LabelSpec("priority: critical", "B60205", "Prioridade crítica."),
    LabelSpec("priority: high", "D93F0B", "Prioridade alta."),
    LabelSpec("priority: medium", "FBCA04", "Prioridade média."),
    LabelSpec("priority: low", "0E8A16", "Prioridade baixa."),
    LabelSpec("blocked", "D73A4A", "Item bloqueado."),
    LabelSpec("needs-discussion", "C2E0C6", "Item que precisa de discussão."),
)


def list_labels(client, owner: str, repo: str) -> set[str]:
    labels: set[str] = set()
    page = 1
    while True:
        data = client.execute_rest("GET", f"/repos/{owner}/{repo}/labels?per_page=100&page={page}")
        if not data:
            break
        for item in data:
            labels.add(item["name"])
        if len(data) < 100:
            break
        page += 1
    return labels


def create_label(client, owner: str, repo: str, spec: LabelSpec) -> None:
    client.execute_rest(
        "POST",
        f"/repos/{owner}/{repo}/labels",
        {"name": spec.name, "color": spec.color, "description": spec.description},
    )


def main() -> int:
    try:
        runtime = build_runtime_context()
        existing_labels = list_labels(runtime.client, runtime.repository.owner, runtime.repository.name)
        created = 0
        ignored = 0

        for label in LABELS:
            if label.name in existing_labels:
                ignored += 1
                continue
            create_label(runtime.client, runtime.repository.owner, runtime.repository.name, label)
            created += 1
            logger.info("Label criada: %s", label.name)

        logger.info("Resumo labels: criadas=%s ignoradas=%s", created, ignored)
        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
