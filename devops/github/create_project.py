"""Cria um GitHub Project V2 na organização configurada."""

from __future__ import annotations

import logging
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.config import load_config
from devops.github.github_client import GitHubApiError, GitHubClient


logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class GitHubProject:
    name: str
    id: str
    number: int
    url: str


def get_organization(client: GitHubClient, org: str) -> dict[str, Any]:
    return client.execute_rest("GET", f"/orgs/{org}")


def get_owner_id(client: GitHubClient, org: str) -> str:
    query = """
    query($login: String!) {
      organization(login: $login) {
        id
      }
    }
    """
    data = client.execute_graphql(query, {"login": org})
    organization = data.get("data", {}).get("organization")
    if not organization or not organization.get("id"):
        raise GitHubApiError("Não foi possível obter o ownerId da organização.")
    return organization["id"]


def find_existing_project(client: GitHubClient, org: str, project_name: str) -> GitHubProject | None:
    query = """
    query($login: String!, $after: String) {
      organization(login: $login) {
        projectsV2(first: 100, after: $after) {
          nodes {
            id
            number
            title
            url
          }
          pageInfo {
            hasNextPage
            endCursor
          }
        }
      }
    }
    """

    after: str | None = None
    while True:
        data = client.execute_graphql(query, {"login": org, "after": after})
        projects = data.get("data", {}).get("organization", {}).get("projectsV2", {})
        for node in projects.get("nodes", []) or []:
            if node.get("title") == project_name:
                return GitHubProject(
                    name=node["title"],
                    id=node["id"],
                    number=int(node["number"]),
                    url=node["url"],
                )

        page_info = projects.get("pageInfo", {})
        if not page_info.get("hasNextPage"):
            return None
        after = page_info.get("endCursor")


def create_project(client: GitHubClient, owner_id: str, project_name: str) -> GitHubProject:
    mutation = """
    mutation($input: CreateProjectV2Input!) {
      createProjectV2(input: $input) {
        projectV2 {
          id
          number
          title
          url
        }
      }
    }
    """
    variables = {
        "input": {
            "ownerId": owner_id,
            "title": project_name,
        }
    }

    data = client.execute_graphql(mutation, variables)
    project = data.get("data", {}).get("createProjectV2", {}).get("projectV2")
    if not project:
        raise GitHubApiError("Não foi possível criar o Project V2.")

    return GitHubProject(
        name=project["title"],
        id=project["id"],
        number=int(project["number"]),
        url=project["url"],
    )


def main() -> int:
    try:
        config = load_config()
        client = GitHubClient(config.token)

        get_organization(client, config.org)
        owner_id = get_owner_id(client, config.org)

        existing_project = find_existing_project(client, config.org, config.project_name)
        if existing_project is not None:
            logger.info("Project já existe.")
            return 0

        project = create_project(client, owner_id, config.project_name)
        logger.info("Nome: %s", project.name)
        logger.info("ID: %s", project.id)
        logger.info("Número: %s", project.number)
        logger.info("URL: %s", project.url)
        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
