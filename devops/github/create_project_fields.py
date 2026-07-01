"""Cria os campos do Project V2 do Corely."""

from __future__ import annotations

import logging
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.create_project import create_project, find_existing_project, get_owner_id
from devops.github.github_client import GitHubApiError
from devops.github.runtime import build_runtime_context


logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class FieldOptionSpec:
    name: str
    color: str


@dataclass(frozen=True, slots=True)
class FieldSpec:
    name: str
    data_type: str
    options: tuple[FieldOptionSpec, ...] = ()


FIELD_SPECS: tuple[FieldSpec, ...] = (
    FieldSpec(
        name="Status",
        data_type="SINGLE_SELECT",
        options=(
            FieldOptionSpec("Backlog", "GRAY"),
            FieldOptionSpec("Refinado", "BLUE"),
            FieldOptionSpec("AI Ready", "PURPLE"),
            FieldOptionSpec("Em Desenvolvimento", "YELLOW"),
            FieldOptionSpec("Code Review", "ORANGE"),
            FieldOptionSpec("Testes", "PINK"),
            FieldOptionSpec("Concluído", "GREEN"),
        ),
    ),
    FieldSpec(
        name="Priority",
        data_type="SINGLE_SELECT",
        options=(
            FieldOptionSpec("Critical", "RED"),
            FieldOptionSpec("High", "ORANGE"),
            FieldOptionSpec("Medium", "YELLOW"),
            FieldOptionSpec("Low", "GREEN"),
        ),
    ),
    FieldSpec(name="Epic", data_type="TEXT"),
    FieldSpec(name="Sprint", data_type="TEXT"),
    FieldSpec(name="Story Points", data_type="NUMBER"),
)


def ensure_project(runtime) -> str:
    existing = find_existing_project(runtime.client, runtime.config.org, runtime.config.project_name)
    if existing is not None:
        return existing.id
    owner_id = get_owner_id(runtime.client, runtime.config.org)
    return create_project(runtime.client, owner_id, runtime.config.project_name).id


def get_project_fields(client, project_id: str) -> dict[str, dict[str, Any]]:
    query = """
    query($projectId: ID!) {
      node(id: $projectId) {
        ... on ProjectV2 {
          fields(first: 100) {
            nodes {
              __typename
              ... on ProjectV2FieldCommon {
                id
                name
                dataType
              }
              ... on ProjectV2SingleSelectField {
                id
                name
                dataType
                options {
                  id
                  name
                }
              }
            }
          }
        }
      }
    }
    """
    data = client.execute_graphql(query, {"projectId": project_id})
    nodes = data.get("data", {}).get("node", {}).get("fields", {}).get("nodes", []) or []
    return {node["name"]: node for node in nodes if node.get("name")}


def create_field(client, project_id: str, spec: FieldSpec) -> dict[str, Any]:
    mutation = """
    mutation($input: CreateProjectV2FieldInput!) {
      createProjectV2Field(input: $input) {
        projectV2Field {
          __typename
          ... on ProjectV2FieldCommon {
            id
            name
            dataType
          }
          ... on ProjectV2SingleSelectField {
            id
            name
            dataType
            options {
              id
              name
            }
          }
        }
      }
    }
    """
    data = client.execute_graphql(
        mutation,
        {"input": {"projectId": project_id, "name": spec.name, "dataType": spec.data_type}},
    )
    field = data.get("data", {}).get("createProjectV2Field", {}).get("projectV2Field")
    if not field:
        raise GitHubApiError(f"Não foi possível criar o campo {spec.name}.")
    return field


def create_single_select_option(client, project_id: str, field_id: str, option: FieldOptionSpec) -> None:
    mutation = """
    mutation($input: CreateProjectV2SingleSelectFieldOptionInput!) {
      createProjectV2SingleSelectFieldOption(input: $input) {
        projectV2SingleSelectFieldOption {
          id
          name
        }
      }
    }
    """
    client.execute_graphql(
        mutation,
        {
            "input": {
                "projectId": project_id,
                "fieldId": field_id,
                "name": option.name,
                "color": option.color,
            }
        },
    )


def ensure_field(client, project_id: str, spec: FieldSpec) -> None:
    fields = get_project_fields(client, project_id)
    if spec.name not in fields:
        fields[spec.name] = create_field(client, project_id, spec)

    if spec.data_type == "SINGLE_SELECT" and spec.options:
        existing_options = {option["name"] for option in fields[spec.name].get("options", []) or []}
        for option in spec.options:
            if option.name in existing_options:
                continue
            create_single_select_option(client, project_id, fields[spec.name]["id"], option)


def main() -> int:
    try:
        runtime = build_runtime_context()
        project_id = ensure_project(runtime)
        processed = 0

        for spec in FIELD_SPECS:
            ensure_field(runtime.client, project_id, spec)
            processed += 1
            logger.info("Campo garantido: %s", spec.name)

        logger.info("Resumo fields: processados=%s", processed)
        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
