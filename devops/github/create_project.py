"""Orquestra a automação do GitHub Project V2 do Corely."""

from __future__ import annotations

import logging
import re
import sys
import unicodedata
from dataclasses import dataclass
from pathlib import Path
from typing import Any

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.configure_project import ensure_views
from devops.github.create_epics import EPICS, create_epic_issue, ensure_milestone_number
from devops.github.create_labels import ensure_labels
from devops.github.create_milestones import ensure_milestones
from devops.github.create_project_fields import ensure_project_fields
from devops.github.github_client import GitHubApiError
from devops.github.runtime import GitHubProject, build_runtime_context, ensure_project


logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


PROJECT_TEMPLATE_DIR = Path(__file__).resolve().parents[2] / ".github" / "ISSUE_TEMPLATE"
PROJECT_TEMPLATE_PATH = PROJECT_TEMPLATE_DIR / "corely.md"
PROJECT_TEMPLATE_CONFIG_PATH = PROJECT_TEMPLATE_DIR / "config.yml"

EPIC_TITLES = {spec.title for spec in EPICS}


@dataclass(frozen=True, slots=True)
class RepositoryIssue:
    id: str
    number: int
    title: str
    labels: tuple[str, ...]


def normalize_text(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value)
    return "".join(char for char in normalized if not unicodedata.combining(char)).lower()


def list_repository_issues(client, owner: str, repo: str) -> list[RepositoryIssue]:
    issues: list[RepositoryIssue] = []
    page = 1
    while True:
        data = client.execute_rest("GET", f"/repos/{owner}/{repo}/issues?state=all&per_page=100&page={page}")
        if not data:
            break

        for item in data:
            if item.get("pull_request"):
                continue

            labels = tuple(label["name"] for label in item.get("labels", []) or [] if label.get("name"))
            issues.append(
                RepositoryIssue(
                    id=item["id"],
                    number=int(item["number"]),
                    title=item["title"],
                    labels=labels,
                )
            )

        if len(data) < 100:
            break
        page += 1

    return issues


def list_project_items(client, project_id: str) -> dict[int, str]:
    query = """
    query($projectId: ID!, $after: String) {
      node(id: $projectId) {
        ... on ProjectV2 {
          items(first: 100, after: $after) {
            nodes {
              id
              content {
                __typename
                ... on Issue {
                  id
                  number
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
    items: dict[int, str] = {}
    after: str | None = None

    while True:
        data = client.execute_graphql(query, {"projectId": project_id, "after": after})
        connection = data.get("data", {}).get("node", {}).get("items", {})
        nodes = connection.get("nodes", []) or []
        for node in nodes:
            content = node.get("content") or {}
            if content.get("__typename") == "Issue" and content.get("number"):
                items[int(content["number"])] = node["id"]

        page_info = connection.get("pageInfo", {})
        if not page_info.get("hasNextPage"):
            break
        after = page_info.get("endCursor")

    return items


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
        raise GitHubApiError(f"Não foi possível obter o node ID da issue #{number}.")
    return issue["id"]


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


def set_item_field_value(client, project_id: str, item_id: str, field_id: str, option_id: str) -> None:
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
        {
            "input": {
                "projectId": project_id,
                "itemId": item_id,
                "fieldId": field_id,
                "value": {"singleSelectOptionId": option_id},
            }
        },
    )


def field_option_id(fields: dict[str, dict[str, Any]], field_name: str, option_name: str) -> str:
    field = fields.get(field_name)
    if not field:
        raise GitHubApiError(f"Campo {field_name} não encontrado no Project.")

    for option in field.get("options", []) or []:
        if option.get("name") == option_name:
            return option["id"]

    raise GitHubApiError(f"Opção {option_name} não encontrada no campo {field_name}.")


def field_id(fields: dict[str, dict[str, Any]], field_name: str) -> str:
    field = fields.get(field_name)
    if not field:
        raise GitHubApiError(f"Campo {field_name} não encontrado no Project.")
    return field["id"]


def map_area_from_title(title: str) -> str:
    normalized = normalize_text(title)
    patterns: tuple[tuple[str, str], ...] = (
        ("agenda", "Agenda"),
        ("repos", "Reposições"),
        ("repor", "Reposições"),
        ("finance", "Financeiro"),
        ("pagamento", "Financeiro"),
        ("cobranca", "Financeiro"),
        ("dashboard", "Dashboard"),
        ("relat", "Relatórios"),
        ("mobile", "Mobile"),
        ("intelig", "IA"),
        ("cadastro", "Cadastros"),
        ("aluno", "Cadastros"),
        ("instrutor", "Cadastros"),
        ("turma", "Cadastros"),
        ("matric", "Cadastros"),
        ("arquitet", "Arquitetura"),
        ("infra", "Infraestrutura"),
        ("devops", "Infraestrutura"),
    )
    for pattern, area in patterns:
        if pattern in normalized:
            return area
    if re.search(r"\bia\b", normalized):
        return "IA"
    return "Arquitetura"


def map_layer_from_labels(labels: tuple[str, ...]) -> str | None:
    normalized_labels = {normalize_text(label) for label in labels}
    for label, layer in (
        ("backend", "Backend"),
        ("frontend", "Frontend"),
        ("api", "API"),
        ("database", "Banco"),
        ("ux", "UX"),
        ("infra", "Infra"),
        ("devops", "DevOps"),
    ):
        if label in normalized_labels:
            return layer
    return None


def map_priority_from_labels(labels: tuple[str, ...]) -> str | None:
    normalized_labels = {normalize_text(label) for label in labels}
    for label in ("critical", "high", "medium", "low"):
        if label in normalized_labels:
            return label.capitalize()
    return None


def ensure_template_files(project: GitHubProject, org: str) -> None:
    PROJECT_TEMPLATE_DIR.mkdir(parents=True, exist_ok=True)

    template_body = f"""---
name: Demanda Corely
about: Estrutura padrão para novas demandas do Corely.
title: ""
projects:
  - {org}/{project.number}
---

## 📋 Objetivo

## 🧠 Regras de Negócio

## ✅ Critérios de Aceite

## 📦 Backend

## 🎨 Frontend

## 🧪 Cenários de Teste

## 📚 Documentação

## ⚠️ Fora do Escopo
"""

    config_body = """blank_issues_enabled: false
contact_links: []
"""

    if not PROJECT_TEMPLATE_PATH.exists() or PROJECT_TEMPLATE_PATH.read_text(encoding="utf-8") != template_body:
        PROJECT_TEMPLATE_PATH.write_text(template_body, encoding="utf-8")

    if not PROJECT_TEMPLATE_CONFIG_PATH.exists() or PROJECT_TEMPLATE_CONFIG_PATH.read_text(encoding="utf-8") != config_body:
        PROJECT_TEMPLATE_CONFIG_PATH.write_text(config_body, encoding="utf-8")


def epic_issue_map(issues: list[RepositoryIssue]) -> dict[str, RepositoryIssue]:
    return {issue.title: issue for issue in issues}


def sync_epics(
    client,
    repository_owner: str,
    repository_name: str,
    project_id: str,
    fields: dict[str, dict[str, Any]],
    issues: list[RepositoryIssue],
) -> tuple[int, int]:
    existing = epic_issue_map(issues)
    mvp_number = ensure_milestone_number(client, repository_owner, repository_name, "MVP")
    project_items = list_project_items(client, project_id)
    processed = 0
    created = 0

    for spec in EPICS:
        issue = existing.get(spec.title)
        if issue is None:
            created_issue = create_epic_issue(client, repository_owner, repository_name, spec, mvp_number)
            created += 1
            logger.info("Épico criado: %s", spec.title)
            issue = RepositoryIssue(
                id=created_issue["id"],
                number=int(created_issue["number"]),
                title=created_issue["title"],
                labels=tuple(label["name"] for label in created_issue.get("labels", []) or [] if label.get("name")),
            )

        if issue is None:
            continue

        item_id = project_items.get(issue.number)
        if not item_id:
            node_id = get_issue_node_id(client, repository_owner, repository_name, issue.number)
            item_id = add_issue_to_project(client, project_id, node_id)
            project_items[issue.number] = item_id

        set_item_field_value(client, project_id, item_id, field_id(fields, "Tipo"), field_option_id(fields, "Tipo", "Epic"))
        set_item_field_value(client, project_id, item_id, field_id(fields, "Área"), field_option_id(fields, "Área", spec.area))
        set_item_field_value(client, project_id, item_id, field_id(fields, "Prioridade"), field_option_id(fields, "Prioridade", "High"))
        set_item_field_value(client, project_id, item_id, field_id(fields, "Sprint"), field_option_id(fields, "Sprint", "Backlog"))
        set_item_field_value(client, project_id, item_id, field_id(fields, "Status Produto"), field_option_id(fields, "Status Produto", "Planejado"))
        set_item_field_value(client, project_id, item_id, field_id(fields, "Status"), field_option_id(fields, "Status", "Backlog"))
        processed += 1
        logger.info("Épico sincronizado: %s", spec.title)

    return processed, created


def sync_stories(
    client,
    repository_owner: str,
    repository_name: str,
    project_id: str,
    fields: dict[str, dict[str, Any]],
    issues: list[RepositoryIssue],
) -> int:
    project_items = list_project_items(client, project_id)
    processed = 0

    for issue in issues:
        labels = {normalize_text(label) for label in issue.labels}
        if "story" not in labels:
            continue
        if issue.title in EPIC_TITLES:
            continue

        item_id = project_items.get(issue.number)
        if not item_id:
            node_id = get_issue_node_id(client, repository_owner, repository_name, issue.number)
            item_id = add_issue_to_project(client, project_id, node_id)
            project_items[issue.number] = item_id

        area = map_area_from_title(issue.title)
        layer = map_layer_from_labels(issue.labels)
        priority = map_priority_from_labels(issue.labels)

        set_item_field_value(client, project_id, item_id, field_id(fields, "Tipo"), field_option_id(fields, "Tipo", "Story"))
        set_item_field_value(client, project_id, item_id, field_id(fields, "Área"), field_option_id(fields, "Área", area))
        if layer:
            set_item_field_value(client, project_id, item_id, field_id(fields, "Camada"), field_option_id(fields, "Camada", layer))
        if priority:
            set_item_field_value(client, project_id, item_id, field_id(fields, "Prioridade"), field_option_id(fields, "Prioridade", priority))
        set_item_field_value(client, project_id, item_id, field_id(fields, "Sprint"), field_option_id(fields, "Sprint", "Backlog"))
        set_item_field_value(client, project_id, item_id, field_id(fields, "Status Produto"), field_option_id(fields, "Status Produto", "Planejado"))
        set_item_field_value(client, project_id, item_id, field_id(fields, "Status"), field_option_id(fields, "Status", "Backlog"))
        processed += 1
        logger.info("Story sincronizada: %s", issue.title)

    return processed


def main() -> int:
    try:
        runtime = build_runtime_context()
        project = ensure_project(runtime.client, runtime.config.org, runtime.config.project_name)

        created_labels, ignored_labels = ensure_labels(runtime.client, runtime.repository.owner, runtime.repository.name)
        created_milestones, ignored_milestones = ensure_milestones(runtime.client, runtime.repository.owner, runtime.repository.name)
        fields = ensure_project_fields(runtime.client, project.id)
        processed_views = ensure_views(runtime.client, project.id)

        ensure_template_files(project, runtime.config.org)

        issues = list_repository_issues(runtime.client, runtime.repository.owner, runtime.repository.name)
        epic_count, epic_created = sync_epics(
            runtime.client,
            runtime.repository.owner,
            runtime.repository.name,
            project.id,
            fields,
            issues,
        )
        story_count = sync_stories(runtime.client, runtime.repository.owner, runtime.repository.name, project.id, fields, issues)

        logger.info(
            "Resumo final: project=%s labels=%s/%s milestones=%s/%s views=%s epics=%s created=%s stories=%s",
            project.number,
            created_labels,
            ignored_labels,
            created_milestones,
            ignored_milestones,
            processed_views,
            epic_count,
            epic_created,
            story_count,
        )
        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
