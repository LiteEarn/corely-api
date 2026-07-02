"""Contexto compartilhado para as automações do GitHub."""

from __future__ import annotations

from dataclasses import dataclass
import re
import subprocess
from typing import Any

from devops.github.config import GitHubConfig, load_config
from devops.github.github_client import GitHubClient


@dataclass(frozen=True, slots=True)
class RepositoryContext:
    owner: str
    name: str


@dataclass(frozen=True, slots=True)
class RuntimeContext:
    config: GitHubConfig
    client: GitHubClient
    repository: RepositoryContext


@dataclass(frozen=True, slots=True)
class GitHubProject:
    name: str
    id: str
    number: int
    url: str


def resolve_repository_context() -> RepositoryContext:
    completed = subprocess.run(
        ["git", "remote", "get-url", "origin"],
        check=True,
        capture_output=True,
        text=True,
    )
    remote_url = completed.stdout.strip()
    match = re.search(
        r"(?:git@github\.com:|https://github\.com/)(?P<owner>[^/]+)/(?P<repo>[^/.]+)(?:\.git)?$",
        remote_url,
    )
    if not match:
        raise ValueError(f"Não foi possível identificar o repositório a partir de: {remote_url}")

    return RepositoryContext(owner=match.group("owner"), name=match.group("repo"))


def get_owner_id(client: GitHubClient, login: str) -> str:
    for owner_type in ("user", "organization"):
        try:
            data = client.execute_graphql(
                """
                query($login: String!) {
                  %s(login: $login) { id }
                }
                """ % owner_type,
                {"login": login},
            )
            owner = data.get("data", {}).get(owner_type)
            if owner and owner.get("id"):
                return owner["id"]
        except GitHubApiError:
            continue
    raise ValueError(f"Não foi possível obter o ownerId de '{login}'.")


def _fetch_owner_projects(client: GitHubClient, login: str, after: str | None) -> dict[str, Any] | None:
    for owner_type in ("user", "organization"):
        try:
            data = client.execute_graphql(
                """
                query($login: String!, $after: String) {
                  %s(login: $login) {
                    projectsV2(first: 100, after: $after) {
                      nodes { id number title url }
                      pageInfo { hasNextPage endCursor }
                    }
                  }
                }
                """ % owner_type,
                {"login": login, "after": after},
            )
            owner = data.get("data", {}).get(owner_type)
            if owner:
                return owner
        except GitHubApiError:
            continue
    return None


def find_existing_project(client: GitHubClient, login: str, project_name: str) -> GitHubProject | None:
    after: str | None = None
    while True:
        owner = _fetch_owner_projects(client, login, after)
        if not owner:
            return None
        projects = owner.get("projectsV2", {})
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
    variables = {"input": {"ownerId": owner_id, "title": project_name}}
    data = client.execute_graphql(mutation, variables)
    project = data.get("data", {}).get("createProjectV2", {}).get("projectV2")
    if not project:
        raise ValueError("Não foi possível criar o Project V2.")

    return GitHubProject(
        name=project["title"],
        id=project["id"],
        number=int(project["number"]),
        url=project["url"],
    )


def ensure_project(client: GitHubClient, org: str, project_name: str) -> GitHubProject:
    existing_project = find_existing_project(client, org, project_name)
    if existing_project is not None:
        return existing_project

    owner_id = get_owner_id(client, org)
    return create_project(client, owner_id, project_name)


def build_runtime_context() -> RuntimeContext:
    config = load_config()
    return RuntimeContext(
        config=config,
        client=GitHubClient(config.token),
        repository=resolve_repository_context(),
    )
