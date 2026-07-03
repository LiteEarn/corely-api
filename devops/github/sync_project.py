"""Sincroniza conclusão de story com o GitHub Project."""

from __future__ import annotations

import argparse
import logging
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.config import load_config
from devops.github.github_client import GitHubApiError, GitHubClient
from devops.github.runtime import build_runtime_context, resolve_repository_context

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)

PROJECT_ID = "PVT_kwHOAt6L7M4BcUH4"
STATUS_FIELD_ID = "PVTSSF_lAHOAt6L7M4BcUH4zhW93hQ"
STATUS_OPTIONS: dict[str, str] = {
    "Backlog": "b3ea9839",
    "Refinado": "8eb68da1",
    "Ready": "92fbd9fc",
    "Em Desenvolvimento": "47699629",
    "Code Review": "4ce68118",
    "Testes": "c5e47935",
    "Homologado": "2161d221",
    "Done": "98236657",
}


@dataclass(frozen=True, slots=True)
class IssueInfo:
    number: int
    title: str
    node_id: str


def get_current_branch() -> str:
    result = subprocess.run(["git", "branch", "--show-current"], capture_output=True, text=True, check=True)
    return result.stdout.strip()


def extract_issue_key(branch: str) -> str | None:
    patterns = [
        r"(?P<key>[A-Z]+-\d+)",
    ]
    for pattern in patterns:
        match = re.search(pattern, branch)
        if match:
            return match.group("key")
    return None


def find_issue_by_key(client: GitHubClient, owner: str, repo: str, key: str) -> IssueInfo | None:
    page = 1
    while True:
        data = client.execute_rest("GET", f"https://api.github.com/repos/{owner}/{repo}/issues?state=all&per_page=100&page={page}")
        if not data:
            break
        for item in data:
            if item.get("pull_request"):
                continue
            title: str = item.get("title", "")
            if key in title:
                node_id = get_issue_node_id(client, owner, repo, int(item["number"]))
                return IssueInfo(number=int(item["number"]), title=title, node_id=node_id)
        if len(data) < 100:
            break
        page += 1
    return None


def get_issue_node_id(client: GitHubClient, owner: str, repo: str, number: int) -> str:
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


def _run_git(*args: str) -> str:
    result = subprocess.run(list(args), capture_output=True, text=True)
    return result.stdout.strip() if result.returncode == 0 else ""


def get_git_diff_summary() -> str:
    commits = _run_git("git", "log", "origin/main..HEAD", "--oneline", "--no-decorate")
    stat = _run_git("git", "diff", "origin/main...HEAD", "--stat")

    parts: list[str] = []
    if commits:
        parts.append("## Commits incluídos\n")
        parts.append("```\n" + commits + "\n```")
    if stat:
        parts.append("\n## Arquivos alterados\n")
        parts.append("```\n" + stat + "\n```")

    return "\n".join(parts) if parts else "N/A"


def add_issue_comment(client: GitHubClient, owner: str, repo: str, issue_number: int, body: str) -> None:
    client.execute_rest("POST", f"https://api.github.com/repos/{owner}/{repo}/issues/{issue_number}/comments", {"body": body})


def find_project_item_id(client: GitHubClient, project_id: str, issue_node_id: str) -> str | None:
    query = """
    query($projectId: ID!, $after: String) {
      node(id: $projectId) {
        ... on ProjectV2 {
          items(first: 100, after: $after) {
            nodes {
              id
              content {
                ... on Issue {
                  id
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
    after: str | None = None
    while True:
        data = client.execute_graphql(query, {"projectId": project_id, "after": after})
        connection = data.get("data", {}).get("node", {}).get("items", {})
        nodes = connection.get("nodes", []) or []
        for node in nodes:
            content = node.get("content") or {}
            if content.get("id") == issue_node_id:
                return node["id"]
        page_info = connection.get("pageInfo", {})
        if not page_info.get("hasNextPage"):
            break
        after = page_info.get("endCursor")
    return None


def set_item_status(client: GitHubClient, project_id: str, item_id: str, status: str) -> None:
    option_id = STATUS_OPTIONS.get(status)
    if not option_id:
        raise ValueError(f"Status inválido: {status}. Opções: {', '.join(STATUS_OPTIONS.keys())}")
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
                "fieldId": STATUS_FIELD_ID,
                "value": {"singleSelectOptionId": option_id},
            }
        },
    )


def add_issue_to_project(client: GitHubClient, project_id: str, issue_node_id: str) -> str:
    mutation = """
    mutation($input: AddProjectV2ItemByIdInput!) {
      addProjectV2ItemById(input: $input) {
        item { id }
      }
    }
    """
    data = client.execute_graphql(mutation, {"input": {"projectId": project_id, "contentId": issue_node_id}})
    payload = data.get("data", {}).get("addProjectV2ItemById", {})
    item = payload.get("item") or payload.get("projectItem")
    if not item:
        raise GitHubApiError("Não foi possível adicionar o item ao Project.")
    return item["id"]


def git_commit_and_push(message: str) -> None:
    subprocess.run(["git", "add", "-A"], check=True)
    result = subprocess.run(["git", "diff", "--cached", "--quiet"], capture_output=True)
    if result.returncode != 0:
        logger.info("Fazendo commit...")
        subprocess.run(["git", "commit", "-m", message], check=True)
    logger.info("Enviando para o GitHub...")
    subprocess.run(["git", "push"], check=True)


def branch_to_commit_message(branch: str) -> str:
    parts = branch.split("/", 1)
    if len(parts) > 1:
        return parts[1].replace("-", " ").title()
    return branch.replace("-", " ").title()


def main() -> int:
    parser = argparse.ArgumentParser(description="Sincroniza story concluída com o GitHub Project")
    parser.add_argument("--issue", "-i", help="Issue key (ex: DASH-002). Detectado da branch se omitido")
    parser.add_argument("--status", "-s", default="Code Review", help="Status para mover (padrão: Code Review)")
    parser.add_argument("--commit", "-c", action="store_true", help="Commit e push automáticos")
    parser.add_argument("--message", "-m", help="Mensagem de commit (usada também no comentário)")
    parser.add_argument("--dry-run", action="store_true", help="Apenas mostra o que seria feito")
    args = parser.parse_args()

    try:
        config = load_config()
        client = GitHubClient(config.token)
        runtime = build_runtime_context()
        repo = runtime.repository

        branch = get_current_branch()
        logger.info("Branch atual: %s", branch)

        issue_key = args.issue or extract_issue_key(branch)
        if not issue_key:
            logger.error("Não foi possível detectar o código da issue da branch '%s'. Use --issue.", branch)
            return 1
        logger.info("Issue key: %s", issue_key)

        issue = find_issue_by_key(client, repo.owner, repo.name, issue_key)
        if not issue:
            logger.error("Nenhuma issue encontrada com a key '%s'.", issue_key)
            return 1
        logger.info("Issue encontrada: #%d - %s", issue.number, issue.title)

        commit_msg = args.message or branch_to_commit_message(branch)

        if args.commit:
            if args.dry_run:
                logger.info("[DRY-RUN] git commit -m \"%s\" && git push", commit_msg)
            else:
                git_commit_and_push(commit_msg)

        diff_summary = get_git_diff_summary()
        comment_body = (
            f"## Implementação concluída\n\n"
            f"{diff_summary}\n\n"
            f"_Sincronizado automaticamente pelo OpenCode._"
        )

        if args.dry_run:
            logger.info("[DRY-RUN] Adicionaria comentário na issue #%d", issue.number)
            logger.info("[DRY-RUN] Comentário:\n%s", comment_body)
            logger.info("[DRY-RUN] Atualizaria Status para '%s'", args.status)
        else:
            logger.info("Adicionando comentário na issue #%d...", issue.number)
            add_issue_comment(client, repo.owner, repo.name, issue.number, comment_body)

            logger.info("Buscando item no Project...")
            item_id = find_project_item_id(client, PROJECT_ID, issue.node_id)
            if not item_id:
                logger.info("Issue #%d não está no Project. Adicionando...", issue.number)
                item_id = add_issue_to_project(client, PROJECT_ID, issue.node_id)
            logger.info("Atualizando Status para '%s'...", args.status)
            set_item_status(client, PROJECT_ID, item_id, args.status)
            logger.info("Status atualizado para '%s'!", args.status)

        logger.info("Resumo:")
        logger.info("  Branch:     %s", branch)
        logger.info("  Issue:      #%d - %s", issue.number, issue.title)
        logger.info("  Status:     %s", args.status)
        logger.info("  Commit:     %s", commit_msg)

        return 0
    except (ValueError, GitHubApiError, subprocess.CalledProcessError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
