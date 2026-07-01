"""Cliente HTTP para a API REST e GraphQL do GitHub."""

from __future__ import annotations

from typing import Any

import requests


class GitHubApiError(RuntimeError):
    pass


class GitHubClient:
    def __init__(self, token: str, api_base_url: str = "https://api.github.com") -> None:
        self._api_base_url = api_base_url.rstrip("/")
        self._session = requests.Session()
        self._session.headers.update(
            {
                "Authorization": f"Bearer {token}",
                "Accept": "application/vnd.github+json",
                "X-GitHub-Api-Version": "2022-11-28",
            }
        )

    def execute_rest(self, method: str, endpoint: str, body: dict[str, Any] | None = None) -> Any:
        url = endpoint if endpoint.startswith("http") else f"{self._api_base_url}/{endpoint.lstrip('/')}"

        try:
            response = self._session.request(method=method.upper(), url=url, json=body, timeout=30)
            response.raise_for_status()
            if response.content:
                return response.json()
            return None
        except requests.RequestException as exc:
            raise GitHubApiError(f"Erro REST no GitHub: {exc}") from exc

    def execute_graphql(self, query: str, variables: dict[str, Any] | None = None) -> dict[str, Any]:
        payload = {"query": query, "variables": variables or {}}
        data = self.execute_rest("POST", "/graphql", payload)

        if isinstance(data, dict) and data.get("errors"):
            raise GitHubApiError(f"Erro GraphQL no GitHub: {data['errors']}")

        if not isinstance(data, dict):
            raise GitHubApiError("Resposta GraphQL inválida do GitHub.")

        return data
