"""Persistencia dos nomes finalistas."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

import yaml


@dataclass(frozen=True, slots=True)
class FinalistEntry:
    name: str
    style: str
    score: int


def load_finalists(path: Path) -> list[FinalistEntry]:
    if not path.exists():
        return []
    data = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    items = data.get("finalists", [])
    if not isinstance(items, list):
        return []

    finalists: list[FinalistEntry] = []
    for item in items:
        if isinstance(item, dict) and item.get("name"):
            finalists.append(
                FinalistEntry(
                    name=str(item["name"]),
                    style=str(item.get("style", "")),
                    score=int(item.get("score", 0)),
                )
            )
    return finalists


def update_finalists(path: Path, results: Iterable[dict[str, object]]) -> list[FinalistEntry]:
    current = load_finalists(path)
    current_names = {item.name.lower() for item in current}

    additions: list[FinalistEntry] = []
    for result in results:
        name = str(result.get("name", "")).strip()
        if not name:
            continue
        if int(result.get("score", 0)) < 90:
            continue
        if str(result.get("github", "")) != "AVAILABLE":
            continue
        lowered = name.lower()
        if lowered in current_names:
            continue

        entry = FinalistEntry(name=name, style=str(result.get("style", "")), score=int(result.get("score", 0)))
        additions.append(entry)
        current_names.add(lowered)

    if not additions:
        return current

    combined = current + additions
    payload = {
        "finalists": [
            {"name": item.name, "style": item.style, "score": item.score}
            for item in sorted(combined, key=lambda item: (-item.score, item.name.lower()))
        ]
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(yaml.safe_dump(payload, sort_keys=False, allow_unicode=True), encoding="utf-8")
    return combined
