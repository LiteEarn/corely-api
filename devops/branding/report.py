"""Geracao dos relatatorios de triagem de branding."""

from __future__ import annotations

from dataclasses import dataclass
import csv
from pathlib import Path
from typing import Iterable

from rich.console import Console
from rich.table import Table


@dataclass(frozen=True, slots=True)
class BrandAssessment:
    name: str
    github: str
    domain_com: str
    domain_com_br: str
    domain_io: str
    risk: str
    score: int


def sort_assessments(items: Iterable[BrandAssessment]) -> list[BrandAssessment]:
    return sorted(items, key=lambda item: (-item.score, item.name.lower()))


def write_csv(path: Path, items: Iterable[BrandAssessment]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(["Nome", "GitHub", ".com", ".com.br", ".io", "Risco", "Score"])
        for item in items:
            writer.writerow(
                [item.name, item.github, item.domain_com, item.domain_com_br, item.domain_io, item.risk, item.score]
            )


def write_markdown(path: Path, items: Iterable[BrandAssessment]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    ordered = sort_assessments(items)
    lines = [
        "# Resultados",
        "",
        "Nao foram encontrados conflitos aparentes durante a triagem.",
        "",
        "E obrigatoria a consulta oficial ao INPI antes da adocao definitiva da marca.",
        "",
        "| Nome | GitHub | .com | .com.br | .io | Risco | Score |",
        "| --- | --- | --- | --- | --- | --- | ---: |",
    ]
    for item in ordered:
        lines.append(
            f"| {item.name} | {item.github} | {item.domain_com} | {item.domain_com_br} | {item.domain_io} | {item.risk} | {item.score} |"
        )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def render_console_report(items: Iterable[BrandAssessment]) -> None:
    console = Console()
    table = Table(title="Branding triage")
    table.add_column("Nome")
    table.add_column("GitHub")
    table.add_column(".com")
    table.add_column(".com.br")
    table.add_column(".io")
    table.add_column("Risco")
    table.add_column("Score", justify="right")

    for item in sort_assessments(items):
        table.add_row(item.name, item.github, item.domain_com, item.domain_com_br, item.domain_io, item.risk, str(item.score))

    console.print(table)
    console.print("Nao foram encontrados conflitos aparentes durante a triagem.")
    console.print("E obrigatoria a consulta oficial ao INPI antes da adocao definitiva da marca.")
