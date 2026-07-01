"""Adiciona os épicos ao Project V2 e os posiciona em Backlog."""

from __future__ import annotations

import logging
import sys
from pathlib import Path
from typing import Any

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.create_epics import EPICS
from devops.github.create_project_fields import ensure_project, get_project_fields
from devops.github.github_client import GitHubApiError
from devops.github.runtime import build_runtime_context


logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


def list_repository_issues(client, owner: str, repo: str) -> dict[str, dict[str, Any]]:
    issues: dict[str, dict[str, Any]] = {}
    page = 1
    while True:
        data = client.execute_rest("GET", f"/repos/{owner}/{repo}/issues?state=all&per_page=100&page={page}")
        if not data:
            break
        for item in data:
            if "pull_request" in item:
                continue
            issues[item["title"]] = item
        if len(data) < 100:
            break
        page += 1
    return issues


def list_project_items(client, project_id: str) -> dict[str, dict[str, Any]]:
    query = """
    query($projectId: ID!) {
      node(id: $projectId) {
        ... on ProjectV2 {
          items(first: 100) {
            nodes {
              id
              content {
                __typename
                ... on Issue {
                  id
                  title
                }
              }
            }
          }
        }
      }
    }
    """
    data = client.execute_graphql(query, {"projectId": project_id})
    nodes = data.get("data", {}).get("node", {}).get("items", {}).get("nodes", []) or []
    items: dict[str, dict[str, Any]] = {}
    for node in nodes:
        content = node.get("content") or {}
        if content.get("__typename") == "Issue" and content.get("title"):
            items[content["title"]] = node
    return items


def add_issue_to_project(client, project_id: str, issue_id: str) -> str:
    mutation = """
    mutation($input: AddProjectV2ItemByIdInput!) {
      addProjectV2ItemById(input: $input) {
        item {
          id
        }
      }
    }
    """
    data = client.execute_graphql(mutation, {"input": {"projectId": project_id, "contentId": issue_id}})
    payload = data.get("data", {}).get("addProjectV2ItemById", {})
    item = payload.get("item") or payload.get("projectItem")
    if not item:
        raise GitHubApiError("Não foi possível adicionar o item ao Project.")
    return item["id"]


def get_backlog_option(client, project_id: str) -> tuple[str, str]:
    fields = get_project_fields(client, project_id)
    status_field = fields.get("Status")
    if not status_field:
        raise GitHubApiError("Campo Status não encontrado no Project.")

    for option in status_field.get("options", []) or []:
        if option["name"] == "Backlog":
            return status_field["id"], option["id"]

    raise GitHubApiError("Opção Backlog não encontrada no campo Status.")


def set_item_status(client, project_id: str, item_id: str, field_id: str, option_id: str) -> None:
    mutation = """
    mutation($input: UpdateProjectV2ItemFieldValueInput!) {
      updateProjectV2ItemFieldValue(input: $input) {
        projectV2Item {
          id
        }
      }
    }
    """
    client.execute_graphql(
        mutation,
        {"input": {"projectId": project_id, "itemId": item_id, "fieldId": field_id, "value": {"singleSelectOptionId": option_id}}},
    )


def main() -> int:
    try:
        runtime = build_runtime_context()
        project_id = ensure_project(runtime)
        repository_issues = list_repository_issues(runtime.client, runtime.repository.owner, runtime.repository.name)
        project_items = list_project_items(runtime.client, project_id)
        status_field_id, backlog_option_id = get_backlog_option(runtime.client, project_id)

        processed = 0
        skipped = 0
        for epic in EPICS:
            issue = repository_issues.get(epic.title)
            if issue is None:
                logger.info("Épico não encontrado no repositório: %s", epic.title)
                skipped += 1
                continue

            item_id = project_items.get(epic.title, {}).get("id")
            if not item_id:
                item_id = add_issue_to_project(runtime.client, project_id, issue["id"])
                project_items[epic.title] = {"id": item_id}

            set_item_status(runtime.client, project_id, item_id, status_field_id, backlog_option_id)
            processed += 1
            logger.info("Épico movido para Backlog: %s", epic.title)

        logger.info("Resumo project items: processados=%s ignorados=%s", processed, skipped)
        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
