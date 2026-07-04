"""Adiciona as tasks criadas do EPIC - Fundacao ao Project V2 com Tipo=Task e Status=Refinado."""

from __future__ import annotations

import logging
import sys
from pathlib import Path

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.config import load_config
from devops.github.create_project_fields import get_project_fields
from devops.github.github_client import GitHubApiError, GitHubClient
from devops.github.runtime import find_existing_project, resolve_repository_context

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)

ISSUE_NUMBERS = [35, 36, 37, 38, 39, 40, 41, 42, 43]


def get_issue_node_id(client, owner: str, repo: str, number: int) -> str:
    query = """
    query($owner: String!, $repo: String!, $number: Int!) {
      repository(owner: $owner, name: $repo) {
        issue(number: $number) { id }
      }
    }
    """
    data = client.execute_graphql(query, {"owner": owner, "repo": repo, "number": number})
    issue = data.get("data", {}).get("repository", {}).get("issue")
    if not issue or not issue.get("id"):
        raise GitHubApiError(f"Nao foi possivel obter o node ID da issue #{number}.")
    return issue["id"]


def list_project_items_by_content_id(client, project_id: str) -> set[str]:
    query = """
    query($projectId: ID!, $after: String) {
      node(id: $projectId) {
        ... on ProjectV2 {
          items(first: 100, after: $after) {
            nodes {
              id
              content { __typename ... on Issue { id } }
            }
            pageInfo { hasNextPage endCursor }
          }
        }
      }
    }
    """
    items: set[str] = set()
    after: str | None = None
    while True:
        data = client.execute_graphql(query, {"projectId": project_id, "after": after})
        connection = data.get("data", {}).get("node", {}).get("items", {})
        for node in connection.get("nodes", []) or []:
            content = node.get("content") or {}
            if content.get("id"):
                items.add(content["id"])
        page_info = connection.get("pageInfo", {})
        if not page_info.get("hasNextPage"):
            break
        after = page_info.get("endCursor")
    return items


def add_issue_to_project(client, project_id: str, issue_id: str) -> str:
    mutation = """
    mutation($input: AddProjectV2ItemByIdInput!) {
      addProjectV2ItemById(input: $input) {
        item { id }
      }
    }
    """
    data = client.execute_graphql(mutation, {"input": {"projectId": project_id, "contentId": issue_id}})
    payload = data.get("data", {}).get("addProjectV2ItemById", {})
    item = payload.get("item") or payload.get("projectItem")
    if not item:
        raise GitHubApiError(f"Nao foi possivel adicionar o item ao Project.")
    return item["id"]


def set_item_field(client, project_id: str, item_id: str, field_id: str, option_id: str) -> None:
    mutation = """
    mutation($input: UpdateProjectV2ItemFieldValueInput!) {
      updateProjectV2ItemFieldValue(input: $input) {
        projectV2Item { id }
      }
    }
    """
    client.execute_graphql(
        mutation,
        {"input": {"projectId": project_id, "itemId": item_id, "fieldId": field_id, "value": {"singleSelectOptionId": option_id}}},
    )


def main() -> int:
    try:
        config = load_config()
        client = GitHubClient(config.token)
        repo = resolve_repository_context()

        project = find_existing_project(client, config.org, config.project_name)
        if not project:
            logger.error("Projeto nao encontrado.")
            return 1

        fields = get_project_fields(client, project.id)

        def opt(field_name: str, option_name: str) -> str:
            f = fields.get(field_name)
            if not f:
                raise GitHubApiError(f"Campo {field_name} nao encontrado.")
            for o in f.get("options", []) or []:
                if o["name"] == option_name:
                    return o["id"]
            raise GitHubApiError(f"Opcao {option_name} nao encontrada em {field_name}.")

        status_id = fields["Status"]["id"]
        existing_content_ids = list_project_items_by_content_id(client, project.id)
        added = 0

        for num in ISSUE_NUMBERS:
            node_id = get_issue_node_id(client, repo.owner, repo.name, num)

            if node_id in existing_content_ids:
                logger.info("Issue #%d ja esta no board, pulando.", num)
                continue

            item_id = add_issue_to_project(client, project.id, node_id)
            existing_content_ids.add(node_id)

            set_item_field(client, project.id, item_id, fields["Tipo"]["id"], opt("Tipo", "Task"))
            set_item_field(client, project.id, item_id, fields["\u00c1rea"]["id"], opt("\u00c1rea", "Arquitetura"))
            set_item_field(client, project.id, item_id, fields["Prioridade"]["id"], opt("Prioridade", "High"))
            set_item_field(client, project.id, item_id, fields["Sprint"]["id"], opt("Sprint", "Backlog"))
            set_item_field(client, project.id, item_id, fields["Status Produto"]["id"], opt("Status Produto", "Em andamento"))
            set_item_field(client, project.id, item_id, status_id, opt("Status", "Refinado"))

            logger.info("Issue #%d adicionada ao board como Task/Refinado.", num)
            added += 1

        logger.info("Resumo: %s issues adicionadas ao board.", added)
        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
