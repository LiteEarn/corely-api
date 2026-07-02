"""Geracao dos relatatorios de triagem de branding."""

from __future__ import annotations

from dataclasses import dataclass
import csv
from pathlib import Path
from typing import Iterable

from rich.console import Console
from rich.table import Table

LAB_NOTICE = "A utilização comercial exige consulta oficial ao INPI."


@dataclass(frozen=True, slots=True)
class BrandAssessment:
    name: str
    github: str
    domain_com: str
    domain_com_br: str
    domain_io: str
    risk: str
    score: int


@dataclass(frozen=True, slots=True)
class BrandLabResult:
    name: str
    style: str
    pattern: str
    score: int
    pronunciation: int
    memorization: int
    length: int
    sonority: int
    originality: int
    style_score: int
    github: str
    domain_com: str
    domain_com_br: str
    domain_io: str
    domain_dev: str
    domain_app: str
    npm: str
    dockerhub: str


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


def write_lab_csv(path: Path, items: Iterable[BrandLabResult]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(
            [
                "Nome",
                "Estilo",
                "Padrao",
                "Score",
                "Pronuncia",
                "Memorizacao",
                "Comprimento",
                "Sonoridade",
                "Originalidade",
                "EstiloScore",
                "GitHub",
                ".com",
                ".com.br",
                ".io",
                ".dev",
                ".app",
                "npm",
                "DockerHub",
            ]
        )
        for item in items:
            writer.writerow(
                [
                    item.name,
                    item.style,
                    item.pattern,
                    item.score,
                    item.pronunciation,
                    item.memorization,
                    item.length,
                    item.sonority,
                    item.originality,
                    item.style_score,
                    item.github,
                    item.domain_com,
                    item.domain_com_br,
                    item.domain_io,
                    item.domain_dev,
                    item.domain_app,
                    item.npm,
                    item.dockerhub,
                ]
            )


def write_results_md(path: Path, items: Iterable[BrandLabResult]) -> None:
    ordered = sorted(items, key=lambda item: (-item.score, item.name.lower()))
    style_count = len({item.style for item in ordered})
    lines = [
        "# Resultados",
        "",
        "Não foram encontrados conflitos aparentes durante a triagem automática.",
        "",
        LAB_NOTICE,
        "",
        f"Total de candidatos: {len(ordered)}",
        f"Quantidade de estilos: {style_count}",
        f"Melhor score: {ordered[0].score if ordered else 0}",
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_ranking_md(path: Path, items: Iterable[BrandLabResult], limit: int | None = None) -> None:
    ordered = sorted(items, key=lambda item: (-item.score, item.name.lower()))
    if limit is not None:
        ordered = ordered[:limit]

    lines = [
        "# Ranking",
        "",
        "Não foram encontrados conflitos aparentes durante a triagem automática.",
        "",
        LAB_NOTICE,
        "",
        "| Nome | Estilo | Score | GitHub | .com | .com.br | .io | .dev | .app | npm | DockerHub |",
        "| --- | --- | ---: | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for item in ordered:
        lines.append(
            f"| {item.name} | {item.style} | {item.score} | {item.github} | {item.domain_com} | {item.domain_com_br} | {item.domain_io} | {item.domain_dev} | {item.domain_app} | {item.npm} | {item.dockerhub} |"
        )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_top100_md(path: Path, items: Iterable[BrandLabResult]) -> None:
    write_ranking_md(path, items, limit=100)


def render_lab_console(items: Iterable[BrandLabResult]) -> None:
    console = Console()
    ordered = sorted(items, key=lambda item: (-item.score, item.name.lower()))
    table = Table(title="Branding Lab")
    table.add_column("Nome")
    table.add_column("Estilo")
    table.add_column("Score", justify="right")
    table.add_column("GitHub")
    table.add_column(".com")
    table.add_column(".io")

    for item in ordered[:20]:
        table.add_row(item.name, item.style, str(item.score), item.github, item.domain_com, item.domain_io)

    console.print(table)
    console.print("Não foram encontrados conflitos aparentes durante a triagem automática.")
    console.print(LAB_NOTICE)


def write_lab_reports(base_dir: Path, items: Iterable[BrandLabResult]) -> None:
    ordered = sorted(items, key=lambda item: (-item.score, item.name.lower()))
    write_lab_csv(base_dir / "results.csv", ordered)
    write_results_md(base_dir / "results.md", ordered)
    write_ranking_md(base_dir / "ranking.md", ordered)
    write_top100_md(base_dir / "Top100.md", ordered)
