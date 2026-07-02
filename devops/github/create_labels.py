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
    LabelSpec("backend", "0E8A16", "Itens relacionados ao backend."),
    LabelSpec("frontend", "1D76DB", "Itens relacionados ao frontend."),
    LabelSpec("api", "0052CC", "Itens relacionados à API."),
    LabelSpec("database", "5319E7", "Itens relacionados ao banco de dados."),
    LabelSpec("ux", "FBCA04", "Itens relacionados à experiência do usuário."),
    LabelSpec("infra", "8250DF", "Itens relacionados à infraestrutura."),
    LabelSpec("devops", "6F42C1", "Itens relacionados a DevOps."),
    LabelSpec("dashboard", "1D76DB", "Itens relacionados ao dashboard."),
    LabelSpec("financeiro", "0E8A16", "Itens relacionados ao financeiro."),
    LabelSpec("agenda", "5319E7", "Itens relacionados à agenda."),
    LabelSpec("attendance", "FBCA04", "Itens relacionados à presença."),
    LabelSpec("makeup", "D4C5F9", "Itens relacionados a reposições."),
    LabelSpec("report", "C2E0C6", "Itens relacionados a relatórios."),
    LabelSpec("bug", "D73A4A", "Correção de defeito."),
    LabelSpec("epic", "5319E7", "Épico do backlog."),
    LabelSpec("story", "FBCA04", "História de usuário."),
    LabelSpec("task", "C2E0C6", "Tarefa técnica ou funcional."),
    LabelSpec("spike", "D4C5F9", "Investigação ou descoberta."),
    LabelSpec("critical", "B60205", "Prioridade crítica."),
    LabelSpec("high", "D93F0B", "Prioridade alta."),
    LabelSpec("medium", "FBCA04", "Prioridade média."),
    LabelSpec("low", "0E8A16", "Prioridade baixa."),
    LabelSpec("blocked", "D73A4A", "Item bloqueado."),
)


def ensure_labels(client, owner: str, repo: str) -> tuple[int, int]:
    existing_labels = list_labels(client, owner, repo)
    created = 0
    ignored = 0

    for label in LABELS:
        if label.name in existing_labels:
            ignored += 1
            continue
        create_label(client, owner, repo, label)
        created += 1
        logger.info("Label criada: %s", label.name)

    return created, ignored


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
        created, ignored = ensure_labels(runtime.client, runtime.repository.owner, runtime.repository.name)

        logger.info("Resumo labels: criadas=%s ignoradas=%s", created, ignored)
        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
