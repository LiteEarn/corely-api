"""Configura as views do Project V2 do Corely."""

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
class ViewSpec:
    name: str
    layout: str


VIEW_SPECS: tuple[ViewSpec, ...] = (
    ViewSpec("View Board", "BOARD"),
    ViewSpec("View Table", "TABLE"),
    ViewSpec("View Roadmap", "ROADMAP"),
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


def ensure_views(client, project_id: str) -> int:
    views = get_views(client, project_id)
    processed = 0

    for view in VIEW_SPECS:
        if view.name in views:
            update_view(client, project_id, views[view.name]["id"], view)
            processed += 1
            logger.info("View configurada: %s", view.name)

    if processed == 0:
        logger.info("Nenhuma view existente para atualizar (ignorado).")

    return processed


def main() -> int:
    try:
        runtime = build_runtime_context()
        from devops.github.runtime import ensure_project

        project = ensure_project(runtime.client, runtime.config.org, runtime.config.project_name)
        processed = ensure_views(runtime.client, project.id)
        logger.info("Resumo projeto: views=%s", processed)
        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
