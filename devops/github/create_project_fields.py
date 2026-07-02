"""Cria e sincroniza os campos do Project V2 do Corely."""

from __future__ import annotations

import logging
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.github_client import GitHubApiError
from devops.github.runtime import build_runtime_context, ensure_project


logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class FieldOptionSpec:
    name: str
    color: str
    description: str = ""


@dataclass(frozen=True, slots=True)
class FieldSpec:
    name: str
    data_type: str
    options: tuple[FieldOptionSpec, ...] = ()
    aliases: tuple[str, ...] = ()


FIELD_SPECS: tuple[FieldSpec, ...] = (
    FieldSpec(
        name="Status",
        data_type="SINGLE_SELECT",
        options=(
            FieldOptionSpec("Backlog", "GRAY"),
            FieldOptionSpec("Refinado", "BLUE"),
            FieldOptionSpec("Ready", "PURPLE"),
            FieldOptionSpec("Em Desenvolvimento", "YELLOW"),
            FieldOptionSpec("Code Review", "ORANGE"),
            FieldOptionSpec("Testes", "PINK"),
            FieldOptionSpec("Homologado", "GREEN"),
            FieldOptionSpec("Done", "GREEN"),
        ),
    ),
    FieldSpec(
        name="Tipo",
        data_type="SINGLE_SELECT",
        options=(
            FieldOptionSpec("Epic", "RED"),
            FieldOptionSpec("Story", "BLUE"),
            FieldOptionSpec("Task", "GREEN"),
            FieldOptionSpec("Bug", "ORANGE"),
            FieldOptionSpec("Spike", "PURPLE"),
        ),
    ),
    FieldSpec(
        name="Área",
        data_type="SINGLE_SELECT",
        options=(
            FieldOptionSpec("Arquitetura", "GRAY"),
            FieldOptionSpec("Cadastros", "BLUE"),
            FieldOptionSpec("Agenda", "GREEN"),
            FieldOptionSpec("Reposições", "YELLOW"),
            FieldOptionSpec("Dashboard", "ORANGE"),
            FieldOptionSpec("Financeiro", "PURPLE"),
            FieldOptionSpec("Relatórios", "PINK"),
            FieldOptionSpec("IA", "RED"),
            FieldOptionSpec("Mobile", "BLUE"),
            FieldOptionSpec("Infraestrutura", "GREEN"),
        ),
    ),
    FieldSpec(
        name="Camada",
        data_type="SINGLE_SELECT",
        options=(
            FieldOptionSpec("Backend", "RED"),
            FieldOptionSpec("Frontend", "BLUE"),
            FieldOptionSpec("API", "GREEN"),
            FieldOptionSpec("Banco", "YELLOW"),
            FieldOptionSpec("UX", "ORANGE"),
            FieldOptionSpec("Infra", "PURPLE"),
            FieldOptionSpec("DevOps", "PINK"),
        ),
    ),
    FieldSpec(
        name="Prioridade",
        data_type="SINGLE_SELECT",
        options=(
            FieldOptionSpec("Critical", "RED"),
            FieldOptionSpec("High", "ORANGE"),
            FieldOptionSpec("Medium", "YELLOW"),
            FieldOptionSpec("Low", "GREEN"),
        ),
        aliases=("Priority",),
    ),
    FieldSpec(
        name="Sprint",
        data_type="SINGLE_SELECT",
        options=(
            FieldOptionSpec("Backlog", "GRAY"),
            FieldOptionSpec("Sprint 1", "BLUE"),
            FieldOptionSpec("Sprint 2", "GREEN"),
            FieldOptionSpec("Sprint 3", "YELLOW"),
            FieldOptionSpec("Sprint 4", "ORANGE"),
        ),
    ),
    FieldSpec(
        name="Status Produto",
        data_type="SINGLE_SELECT",
        options=(
            FieldOptionSpec("Planejado", "GRAY"),
            FieldOptionSpec("Em andamento", "BLUE"),
            FieldOptionSpec("Concluído", "GREEN"),
        ),
    ),
)

LEGACY_FIELDS: tuple[str, ...] = ("Epic", "Story Points")


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
    input_data: dict[str, Any] = {"projectId": project_id, "name": spec.name, "dataType": spec.data_type}
    if spec.data_type == "SINGLE_SELECT" and spec.options:
        input_data["singleSelectOptions"] = [
            {"name": option.name, "color": option.color, "description": option.description or option.name}
            for option in spec.options
        ]

    data = client.execute_graphql(mutation, {"input": input_data})
    field = data.get("data", {}).get("createProjectV2Field", {}).get("projectV2Field")
    if not field:
        raise GitHubApiError(f"Não foi possível criar o campo {spec.name}.")
    return field


def update_field(client, field_id: str, spec: FieldSpec, existing_options: dict[str, str] | None = None) -> dict[str, Any]:
    mutation = """
    mutation($input: UpdateProjectV2FieldInput!) {
      updateProjectV2Field(input: $input) {
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

    options = []
    if spec.data_type == "SINGLE_SELECT" and spec.options:
        for option in spec.options:
            payload: dict[str, Any] = {
                "name": option.name,
                "color": option.color,
                "description": option.description or option.name,
            }
            if existing_options and option.name in existing_options:
                payload["id"] = existing_options[option.name]
            options.append(payload)

    variables: dict[str, Any] = {"input": {"fieldId": field_id, "name": spec.name}}
    if options:
        variables["input"]["singleSelectOptions"] = options

    data = client.execute_graphql(mutation, variables)
    field = data.get("data", {}).get("updateProjectV2Field", {}).get("projectV2Field")
    if not field:
        raise GitHubApiError(f"Não foi possível atualizar o campo {spec.name}.")
    return field


def delete_field(client, field_id: str, field_name: str) -> None:
    mutation = """
    mutation($input: DeleteProjectV2FieldInput!) {
      deleteProjectV2Field(input: $input) {
        projectV2Field {
          id
        }
      }
    }
    """
    client.execute_graphql(mutation, {"input": {"fieldId": field_id}})
    logger.info("Campo removido: %s", field_name)


def ensure_field(client, project_id: str, spec: FieldSpec, fields: dict[str, dict[str, Any]]) -> dict[str, Any]:
    field = fields.get(spec.name)
    alias_field = next((fields[alias] for alias in spec.aliases if alias in fields), None)

    if field is None and alias_field is not None:
        field = alias_field

    if field is not None and field.get("dataType") != spec.data_type:
        delete_field(client, field["id"], field["name"])
        field = None

    if field is None:
        field = create_field(client, project_id, spec)
    elif field.get("name") != spec.name or spec.data_type == "SINGLE_SELECT":
        existing_options = {option["name"]: option["id"] for option in field.get("options", []) or []}
        field = update_field(client, field["id"], spec, existing_options)

    return field


def ensure_project_fields(client, project_id: str) -> dict[str, dict[str, Any]]:
    fields = get_project_fields(client, project_id)

    for legacy_name in LEGACY_FIELDS:
        legacy_field = fields.get(legacy_name)
        if legacy_field and legacy_name not in {spec.name for spec in FIELD_SPECS} and legacy_name != "Priority":
            delete_field(client, legacy_field["id"], legacy_name)
            fields.pop(legacy_name, None)

    for spec in FIELD_SPECS:
        field = ensure_field(client, project_id, spec, fields)
        fields[field["name"]] = field
        for alias in spec.aliases:
            fields.pop(alias, None)
        logger.info("Campo sincronizado: %s", spec.name)

    # Remove nomes antigos que podem ter sido renomeados.
    if "Priority" in fields and "Prioridade" in fields:
        delete_field(client, fields["Priority"]["id"], "Priority")
        fields.pop("Priority", None)

    return get_project_fields(client, project_id)


def main() -> int:
    try:
        runtime = build_runtime_context()
        project = ensure_project(runtime.client, runtime.config.org, runtime.config.project_name)
        fields = ensure_project_fields(runtime.client, project.id)
        logger.info("Resumo fields: total=%s", len(fields))
        return 0
    except (ValueError, GitHubApiError) as exc:
        logger.error(str(exc))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
