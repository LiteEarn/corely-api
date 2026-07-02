"""Cria milestones padrão para o Corely no repositório GitHub."""

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
class MilestoneSpec:
    title: str
    description: str


MILESTONES: tuple[MilestoneSpec, ...] = (
    MilestoneSpec("MVP", "Marco mínimo viável do Corely."),
    MilestoneSpec("V1", "Primeira versão pública do Corely."),
    MilestoneSpec("V2", "Evolução da primeira versão do Corely."),
    MilestoneSpec("Mobile", "Entrega voltada ao aplicativo mobile."),
    MilestoneSpec("IA", "Entrega relacionada a inteligência artificial."),
    MilestoneSpec("Financeiro", "Entrega relacionada ao módulo financeiro."),
    MilestoneSpec("Marketplace", "Entrega relacionada ao marketplace."),
)


def ensure_milestones(client, owner: str, repo: str) -> tuple[int, int]:
    existing_milestones = list_milestones(client, owner, repo)
    created = 0
    ignored = 0

    for milestone in MILESTONES:
        if milestone.title in existing_milestones:
            ignored += 1
            continue
        create_milestone(client, owner, repo, milestone)
        created += 1
        logger.info("Milestone criado: %s", milestone.title)

    return created, ignored


def list_milestones(client, owner: str, repo: str) -> dict[str, int]:
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


def create_milestone(client, owner: str, repo: str, spec: MilestoneSpec) -> None:
    client.execute_rest(
        "POST",
        f"/repos/{owner}/{repo}/milestones",
        {"title": spec.title, "description": spec.description, "state": "open"},
    )


def main() -> int:
    try:
        runtime = build_runtime_context()
        created, ignored = ensure_milestones(runtime.client, runtime.repository.owner, runtime.repository.name)

        logger.info("Resumo milestones: criados=%s ignorados=%s", created, ignored)
        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
