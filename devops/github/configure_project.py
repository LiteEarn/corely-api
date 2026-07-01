"""Configura views e status do Project V2 do Corely."""

from __future__ import annotations

import logging
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.create_project_fields import ensure_project, get_project_fields
from devops.github.github_client import GitHubApiError
from devops.github.runtime import build_runtime_context


logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class ViewSpec:
    name: str
    layout: str


VIEW_SPECS: tuple[ViewSpec, ...] = (
    ViewSpec("View Board", "BOARD"),
    ViewSpec("View Table", "TABLE"),
    ViewSpec("View Roadmap", "ROADMAP"),
)

STATUS_OPTIONS: tuple[tuple[str, str], ...] = (
    ("Backlog", "GRAY"),
    ("Refinado", "BLUE"),
    ("AI Ready", "PURPLE"),
    ("Em Desenvolvimento", "YELLOW"),
    ("Code Review", "ORANGE"),
    ("Testes", "PINK"),
    ("Concluído", "GREEN"),
)
def get_views(client, project_id: str) -> dict[str, dict[str, Any]]:
    query = """
    query($projectId: ID!) {
      node(id: $projectId) {
        ... on ProjectV2 {
          views(first: 100) {
            nodes {
              id
              name
              layout
            }
          }
        }
      }
    }
    """
    data = client.execute_graphql(query, {"projectId": project_id})
    nodes = data.get("data", {}).get("node", {}).get("views", {}).get("nodes", []) or []
    return {node["name"]: node for node in nodes if node.get("name")}


def create_view(client, project_id: str, view: ViewSpec) -> None:
    mutation = """
    mutation($input: CreateProjectV2ViewInput!) {
      createProjectV2View(input: $input) {
        projectV2View {
          id
          name
          layout
        }
      }
    }
    """
    client.execute_graphql(mutation, {"input": {"projectId": project_id, "name": view.name, "layout": view.layout}})


def update_view(client, project_id: str, view_id: str, view: ViewSpec) -> None:
    mutation = """
    mutation($input: UpdateProjectV2ViewInput!) {
      updateProjectV2View(input: $input) {
        projectV2View {
          id
          name
          layout
        }
      }
    }
    """
    client.execute_graphql(
        mutation,
        {"input": {"projectId": project_id, "viewId": view_id, "name": view.name, "layout": view.layout}},
    )


def ensure_status_field(client, project_id: str) -> None:
    fields = get_project_fields(client, project_id)
    status_field = fields.get("Status")
    if not status_field:
        raise GitHubApiError("Campo Status não encontrado no Project.")

    existing = {option["name"] for option in status_field.get("options", []) or []}
    for option_name, color in STATUS_OPTIONS:
        if option_name in existing:
            continue
        mutation = """
        mutation($input: CreateProjectV2SingleSelectFieldOptionInput!) {
          createProjectV2SingleSelectFieldOption(input: $input) {
            projectV2SingleSelectFieldOption {
              id
              name
            }
          }
        }
        """
        client.execute_graphql(
            mutation,
            {"input": {"projectId": project_id, "fieldId": status_field["id"], "name": option_name, "color": color}},
        )


def main() -> int:
    try:
        runtime = build_runtime_context()
        project_id = ensure_project(runtime)
        views = get_views(runtime.client, project_id)

        processed = 0
        for view in VIEW_SPECS:
            if view.name in views:
                update_view(runtime.client, project_id, views[view.name]["id"], view)
            else:
                create_view(runtime.client, project_id, view)
            processed += 1
            logger.info("View configurada: %s", view.name)

        ensure_status_field(runtime.client, project_id)
        logger.info("Status configurado com opções padrão.")
        logger.info("Resumo projeto: views=%s status=ok", processed)
        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
