"""Atualiza o Status de cada EPIC no GitHub Project V2 com base na análise do código."""

from __future__ import annotations

import logging
import sys
from pathlib import Path

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.config import load_config
from devops.github.github_client import GitHubApiError, GitHubClient
from devops.github.runtime import find_existing_project, resolve_repository_context

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)

EPIC_STATUSES: dict[str, str] = {
    "EPIC - Fundação": "Em Desenvolvimento",
    "EPIC - Cadastros": "Homologado",
    "EPIC - Agenda Operacional": "Homologado",
    "EPIC - Reposições": "Homologado",
    "EPIC - Dashboard": "Em Desenvolvimento",
    "EPIC - Financeiro": "Backlog",
    "EPIC - Relatórios": "Backlog",
    "EPIC - Mobile": "Backlog",
    "EPIC - IA": "Backlog",
    "EPIC - Infraestrutura": "Homologado",
}


def list_epic_items(client, project_id: str) -> dict[str, dict]:
    query = """
    query($projectId: ID!, $after: String) {
      node(id: $projectId) {
        ... on ProjectV2 {
          items(first: 100, after: $after) {
            nodes {
              id
              type
              content {
                __typename
                ... on Issue {
                  id
                  title
                  number
                }
              }
              fieldValues(first: 20) {
                nodes {
                  __typename
                  ... on ProjectV2ItemFieldSingleSelectValue {
                    field {
                      ... on ProjectV2SingleSelectField {
                        name
                        id
                      }
                    }
                    name
                    id
                    optionId
                  }
                }
              }
            }
            pageInfo {
              hasNextPage
              endCursor
            }
          }
        }
      }
    }
    """
    items: dict[str, dict] = {}
    after: str | None = None
    while True:
        data = client.execute_graphql(query, {"projectId": project_id, "after": after})
        connection = data.get("data", {}).get("node", {}).get("items", {})
        nodes = connection.get("nodes", []) or []
        for node in nodes:
            content = node.get("content") or {}
            if content.get("__typename") == "Issue":
                items[content["title"]] = node
        page_info = connection.get("pageInfo", {})
        if not page_info.get("hasNextPage"):
            break
        after = page_info.get("endCursor")
    return items


def get_status_field_and_option(client, project_id: str, target_status: str) -> tuple[str, str] | None:
    query = """
    query($projectId: ID!) {
      node(id: $projectId) {
        ... on ProjectV2 {
          fields(first: 50) {
            nodes {
              __typename
              ... on ProjectV2SingleSelectField {
                id
                name
                options { id name }
              }
            }
          }
        }
      }
    }
    """
    data = client.execute_graphql(query, {"projectId": project_id})
    nodes = data.get("data", {}).get("node", {}).get("fields", {}).get("nodes", []) or []
    for field in nodes:
        if field.get("name") == "Status":
            for option in field.get("options", []) or []:
                if option["name"] == target_status:
                    return field["id"], option["id"]
    return None


def set_item_status(client, project_id: str, item_id: str, field_id: str, option_id: str) -> None:
    mutation = """
    mutation($input: UpdateProjectV2ItemFieldValueInput!) {
      updateProjectV2ItemFieldValue(input: $input) {
        projectV2Item { id }
      }
    }
    """
    client.execute_graphql(
        mutation,
        {
            "input": {
                "projectId": project_id,
                "itemId": item_id,
                "fieldId": field_id,
                "value": {"singleSelectOptionId": option_id},
            }
        },
    )


def main() -> int:
    try:
        config = load_config()
        client = GitHubClient(config.token)

        project = find_existing_project(client, config.org, config.project_name)
        if not project:
            logger.error("Projeto '%s' não encontrado na org '%s'. Execute create_project.py primeiro.", config.project_name, config.org)
            return 1

        logger.info("Projeto encontrado: %s (ID: %s)", project.name, project.id)

        items = list_epic_items(client, project.id)
        updated = 0
        not_found = 0

        for epic_title, target_status in EPIC_STATUSES.items():
            item = items.get(epic_title)
            if not item:
                logger.warning("EPIC não encontrado no board: %s", epic_title)
                not_found += 1
                continue

            status_info = get_status_field_and_option(client, project.id, target_status)
            if not status_info:
                logger.error("Status '%s' não encontrado nos campos do projeto.", target_status)
                return 1

            field_id, option_id = status_info
            set_item_status(client, project.id, item["id"], field_id, option_id)
            logger.info("✓ %s -> %s", epic_title, target_status)
            updated += 1

        logger.info("Resumo: %s EPICs atualizados, %s não encontrados", updated, not_found)
        return 0

    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
