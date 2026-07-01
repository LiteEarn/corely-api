"""Contexto compartilhado para as automações do GitHub."""

from __future__ import annotations

from dataclasses import dataclass
import re
import subprocess

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


def build_runtime_context() -> RuntimeContext:
    config = load_config()
    return RuntimeContext(
        config=config,
        client=GitHubClient(config.token),
        repository=resolve_repository_context(),
    )
