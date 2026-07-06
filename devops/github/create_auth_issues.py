"""Cria as Issues de Autenticação (AUTH-001 a AUTH-008) no repositório e as adiciona ao Project Corely."""

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
class AuthIssueSpec:
    id: str
    title: str
    body: str
    labels: tuple[str, ...]


AUTH_ISSUES: tuple[AuthIssueSpec, ...] = (
    AuthIssueSpec(
        id="AUTH-001",
        title="Implementar autenticação JWT",
        body="""**ID:** AUTH-001

## Objetivo

Criar toda infraestrutura de autenticação baseada em JWT.

## Critérios de Aceitação

- [ ] emissão de token
- [ ] validação
- [ ] filtro Spring Security
- [ ] claims
- [ ] expiração
- [ ] testes

## Área
Autenticação

## Milestone
MVP""",
        labels=("backend", "frontend", "security", "priority:high"),
    ),
    AuthIssueSpec(
        id="AUTH-002",
        title="Tela de Login",
        body="""**ID:** AUTH-002

## Objetivo

Criar a tela moderna de login utilizando o Design System.

## Critérios de Aceitação

- [ ] responsiva
- [ ] validações
- [ ] loading
- [ ] tratamento de erro
- [ ] integração com AUTH-001

## Área
Autenticação

## Milestone
MVP""",
        labels=("backend", "frontend", "security", "priority:high"),
    ),
    AuthIssueSpec(
        id="AUTH-003",
        title="Controle de Perfis (RBAC)",
        body="""**ID:** AUTH-003

## Objetivo

Implementar perfis:

- OWNER
- ADMIN
- RECEPTIONIST
- INSTRUCTOR
- FINANCIAL
- STUDENT

## Critérios de Aceitação

- [ ] Backend
- [ ] Frontend
- [ ] Spring Security
- [ ] Testes

## Área
Autenticação

## Milestone
MVP""",
        labels=("backend", "frontend", "security", "priority:high"),
    ),
    AuthIssueSpec(
        id="AUTH-004",
        title="Recuperação de Senha",
        body="""**ID:** AUTH-004

## Objetivo

Implementar fluxo de recuperação de senha.

## Área
Autenticação

## Milestone
MVP""",
        labels=("backend", "frontend", "security", "priority:high"),
    ),
    AuthIssueSpec(
        id="AUTH-005",
        title="Troca de Senha",
        body="""**ID:** AUTH-005

## Objetivo

Implementar fluxo de troca de senha.

## Área
Autenticação

## Milestone
MVP""",
        labels=("backend", "frontend", "security", "priority:high"),
    ),
    AuthIssueSpec(
        id="AUTH-006",
        title="Refresh Token",
        body="""**ID:** AUTH-006

## Objetivo

Implementar refresh token para renovação de sessão.

## Área
Autenticação

## Milestone
MVP""",
        labels=("backend", "security", "priority:high"),
    ),
    AuthIssueSpec(
        id="AUTH-007",
        title="Remember Me",
        body="""**ID:** AUTH-007

## Objetivo

Implementar funcionalidade Remember Me (manter conectado).

## Área
Autenticação

## Milestone
MVP""",
        labels=("backend", "frontend", "security", "priority:high"),
    ),
    AuthIssueSpec(
        id="AUTH-008",
        title="Auditoria de Login",
        body="""**ID:** AUTH-008

## Objetivo

Registrar eventos de auditoria:

- login
- logout
- tentativas inválidas
- IP
- User Agent

## Área
Autenticação

## Milestone
MVP""",
        labels=("backend", "security", "priority:high"),
    ),
)

EXTRA_LABELS: tuple[tuple[str, str, str], ...] = (
    ("security", "E91E63", "Segurança e autenticação"),
    ("priority:high", "FF6F00", "Prioridade alta"),
)


def ensure_label(client, owner: str, repo: str, name: str, color: str, description: str) -> dict[str, Any]:
    try:
        existing = client.execute_rest("GET", f"/repos/{owner}/{repo}/labels/{name}")
        if existing and existing.get("name") == name:
            return existing
    except GitHubApiError:
        pass
    return client.execute_rest(
        "POST",
        f"/repos/{owner}/{repo}/labels",
        {"name": name, "color": color, "description": description},
    )


def ensure_milestone_number(client, owner: str, repo: str, title: str) -> int:
    milestones = client.execute_rest("GET", f"/repos/{owner}/{repo}/milestones?state=all&per_page=100")
    for ms in milestones:
        if ms.get("title") == title:
            return int(ms["number"])
    data = client.execute_rest(
        "POST",
        f"/repos/{owner}/{repo}/milestones",
        {"title": title, "description": f"Milestone {title} do Corely.", "state": "open"},
    )
    return int(data["number"])


def list_existing_issue_titles(client, owner: str, repo: str) -> dict[str, int]:
    titles: dict[str, int] = {}
    page = 1
    while True:
        data = client.execute_rest("GET", f"/repos/{owner}/{repo}/issues?state=all&per_page=100&page={page}")
        if not data:
            break
        for item in data:
            if "pull_request" in item:
                continue
            titles[item["title"]] = int(item["number"])
        if len(data) < 100:
            break
        page += 1
    return titles


def create_issue(client, owner: str, repo: str, spec: AuthIssueSpec, milestone_number: int) -> dict[str, Any]:
    return client.execute_rest(
        "POST",
        f"/repos/{owner}/{repo}/issues",
        {
            "title": spec.title,
            "body": spec.body,
            "labels": list(spec.labels),
            "milestone": milestone_number,
        },
    )


def _fetch_owner_projects(client, login: str, after: str | None) -> dict[str, Any] | None:
    for owner_type in ("user", "organization"):
        try:
            data = client.execute_graphql(
                """query($login: String!, $after: String) {
                  %s(login: $login) {
                    projectsV2(first: 100, after: $after) {
                      nodes { id number title url }
                      pageInfo { hasNextPage endCursor }
                    }
                  }
                }""" % owner_type,
                {"login": login, "after": after},
            )
            owner = data.get("data", {}).get(owner_type)
            if owner:
                return owner
        except GitHubApiError:
            continue
    return None


def find_project(client, login: str, project_name: str) -> dict[str, Any] | None:
    after: str | None = None
    while True:
        owner = _fetch_owner_projects(client, login, after)
        if not owner:
            return None
        projects = owner.get("projectsV2", {})
        for node in projects.get("nodes", []) or []:
            if node.get("title") == project_name:
                return node
        page_info = projects.get("pageInfo", {})
        if not page_info.get("hasNextPage"):
            return None
        after = page_info.get("endCursor")
    return None


def get_issue_node_id(client, owner: str, repo: str, number: int) -> str:
    data = client.execute_graphql(
        """query($owner: String!, $repo: String!, $number: Int!) {
          repository(owner: $owner, name: $repo) {
            issue(number: $number) { id }
          }
        }""",
        {"owner": owner, "repo": repo, "number": number},
    )
    return data["data"]["repository"]["issue"]["id"]


def list_project_item_content_ids(client, project_id: str) -> set[str]:
    data = client.execute_graphql(
        """query($projectId: ID!) {
          node(id: $projectId) {
            ... on ProjectV2 {
              items(first: 100) {
                nodes {
                  content {
                    __typename
                    ... on Issue { id }
                  }
                }
              }
            }
          }
        }""",
        {"projectId": project_id},
    )
    nodes = data.get("data", {}).get("node", {}).get("items", {}).get("nodes", []) or []
    return {
        node["content"]["id"]
        for node in nodes
        if node.get("content") and node["content"].get("__typename") == "Issue" and node["content"].get("id")
    }


def add_issue_to_project(client, project_id: str, issue_id: str) -> str:
    data = client.execute_graphql(
        """mutation($input: AddProjectV2ItemByIdInput!) {
          addProjectV2ItemById(input: $input) {
            item { id }
          }
        }""",
        {"input": {"projectId": project_id, "contentId": issue_id}},
    )
    payload = data.get("data", {}).get("addProjectV2ItemById", {})
    item = payload.get("item") or payload.get("projectItem")
    if not item:
        raise GitHubApiError("Não foi possível adicionar o item ao Project.")
    return item["id"]


def get_project_fields(client, project_id: str) -> dict[str, dict[str, Any]]:
    data = client.execute_graphql(
        """query($projectId: ID!) {
          node(id: $projectId) {
            ... on ProjectV2 {
              fields(first: 100) {
                nodes {
                  __typename
                  ... on ProjectV2FieldCommon { id name dataType }
                  ... on ProjectV2SingleSelectField { id name dataType options { id name } }
                }
              }
            }
          }
        }""",
        {"projectId": project_id},
    )
    nodes = data.get("data", {}).get("node", {}).get("fields", {}).get("nodes", []) or []
    return {node["name"]: node for node in nodes if node.get("name")}


def get_backlog_option(fields: dict[str, dict[str, Any]]) -> tuple[str, str]:
    status_field = fields.get("Status")
    if not status_field:
        raise GitHubApiError("Campo Status não encontrado no Project.")
    for option in status_field.get("options", []) or []:
        if option["name"] == "Backlog":
            return status_field["id"], option["id"]
    raise GitHubApiError("Opção Backlog não encontrada no campo Status.")


def set_item_field_value(client, project_id: str, item_id: str, field_id: str, option_id: str) -> None:
    client.execute_graphql(
        """mutation($input: UpdateProjectV2ItemFieldValueInput!) {
          updateProjectV2ItemFieldValue(input: $input) {
            projectV2Item { id }
          }
        }""",
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
        runtime = build_runtime_context()
        client = runtime.client
        config = runtime.config
        repo = runtime.repository

        logger.info("=== Criando Issues de Autenticação ===")
        logger.info("Repositório: %s/%s", repo.owner, repo.name)
        logger.info("Organização: %s", config.org)
        logger.info("Projeto: %s", config.project_name)

        # 1. Ensure extra labels
        logger.info("")
        logger.info("--- Garantindo labels ---")
        for name, color, desc in EXTRA_LABELS:
            ensure_label(client, repo.owner, repo.name, name, color, desc)
            logger.info("Label garantida: %s", name)

        # 2. Ensure MVP milestone
        logger.info("")
        logger.info("--- Garantindo milestone MVP ---")
        mvp_number = ensure_milestone_number(client, repo.owner, repo.name, "MVP")
        logger.info("Milestone MVP: #%s", mvp_number)

        # 3. Find project
        logger.info("")
        logger.info("--- Localizando Project Corely ---")
        project = find_project(client, config.org, config.project_name)
        if not project:
            raise GitHubApiError(f"Project '{config.project_name}' não encontrado.")
        logger.info("Project: %s (ID: %s)", config.project_name, project["id"])

        # 4. Get project fields and existing project items
        logger.info("")
        logger.info("--- Obtendo campos do Project ---")
        fields = get_project_fields(client, project["id"])
        status_field_id, backlog_option_id = get_backlog_option(fields)
        logger.info("Campo Status encontrado. Backlog option OK.")

        existing_project_content_ids = list_project_item_content_ids(client, project["id"])
        logger.info("Items existentes no Project: %d", len(existing_project_content_ids))

        # 5. Check existing issues
        logger.info("")
        logger.info("--- Verificando issues existentes ---")
        existing_issues = list_existing_issue_titles(client, repo.owner, repo.name)
        logger.info("Issues existentes no repositório: %d", len(existing_issues))

        # 6. Create issues and add to project
        logger.info("")
        logger.info("--- Criando issues e adicionando ao Project ---")

        results: list[tuple[str, str, int | None]] = []

        for spec in AUTH_ISSUES:
            issue_number: int | None = None
            status = "criada"

            if spec.title in existing_issues:
                issue_number = existing_issues[spec.title]
                status = "ja_existia"
                logger.info("[%s] já existe (#%d): %s", spec.id, issue_number, spec.title)
            else:
                issue = create_issue(client, repo.owner, repo.name, spec, mvp_number)
                issue_number = int(issue["number"])
                logger.info("[%s] criada (#%d): %s", spec.id, issue_number, spec.title)

            # Add to project if not already there
            issue_node_id = get_issue_node_id(client, repo.owner, repo.name, issue_number)
            if issue_node_id not in existing_project_content_ids:
                item_id = add_issue_to_project(client, project["id"], issue_node_id)
                set_item_field_value(client, project["id"], item_id, status_field_id, backlog_option_id)
                existing_project_content_ids.add(issue_node_id)
                logger.info("  -> Adicionada ao Project + Status=Backlog")
            else:
                logger.info("  -> Já está no Project")

            results.append((spec.id, spec.title, issue_number))

        # 7. Summary
        logger.info("")
        logger.info("========================================")
        logger.info("          RESUMO FINAL                 ")
        logger.info("========================================")
        for spec_id, title, number in results:
            logger.info("%s → #%d → https://github.com/%s/%s/issues/%d", spec_id, number, repo.owner, repo.name, number)

        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
