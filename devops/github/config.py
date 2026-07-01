"""Configuração da integração com GitHub."""

from __future__ import annotations

from dataclasses import dataclass
import os

from dotenv import load_dotenv


load_dotenv()

GITHUB_TOKEN: str | None = os.getenv("GITHUB_TOKEN")
GITHUB_ORG: str | None = os.getenv("GITHUB_ORG")
PROJECT_NAME: str | None = os.getenv("PROJECT_NAME")


@dataclass(frozen=True, slots=True)
class GitHubConfig:
    token: str
    org: str
    project_name: str


def load_config() -> GitHubConfig:
    missing = [
        name
        for name, value in (
            ("GITHUB_TOKEN", GITHUB_TOKEN),
            ("GITHUB_ORG", GITHUB_ORG),
            ("PROJECT_NAME", PROJECT_NAME),
        )
        if not value
    ]

    if missing:
        raise ValueError(f"Variáveis de ambiente ausentes: {', '.join(missing)}")

    return GitHubConfig(
        token=GITHUB_TOKEN or "",
        org=GITHUB_ORG or "",
        project_name=PROJECT_NAME or "",
    )
