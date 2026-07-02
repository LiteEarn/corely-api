"""Branding Lab: geracao, validacao, pontuacao e relatorios."""

from __future__ import annotations

from dataclasses import asdict, dataclass
from itertools import islice, product
import logging
from pathlib import Path
import sys
from typing import Iterable

import yaml
from dotenv import load_dotenv

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.branding.finalists import update_finalists
from devops.branding.branding_client import BrandingClient
from devops.branding.phonetics import blend_fragments, is_acceptable, normalize_fragment, title_case_name
from devops.branding.report import BrandLabResult, render_lab_console, write_lab_reports
from devops.branding.scorer import ScoreBreakdown, StyleProfile, score_name
from devops.branding.validator import BrandValidator, ValidationStatus

logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class FragmentPools:
    prefixes: tuple[str, ...]
    middles: tuple[str, ...]
    suffixes: tuple[str, ...]
    blacklist: tuple[str, ...]


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
    load_dotenv()

    base_dir = Path(__file__).resolve().parent
    data_dir = base_dir / "data"

    pools = FragmentPools(
        prefixes=load_words(data_dir / "prefixes.yaml", "prefixes"),
        middles=load_words(data_dir / "middles.yaml", "middles"),
        suffixes=load_words(data_dir / "suffixes.yaml", "suffixes"),
        blacklist=load_words(data_dir / "blacklist.yaml", "blacklist"),
    )
    styles = load_styles(data_dir / "styles.yaml")

    validator = BrandValidator(client=BrandingClient(offline=True))
    results = generate_lab_results(styles, pools, validator, limit_per_style=500)

    ordered = sorted(results, key=lambda item: (-item.score, item.name.lower(), item.style.lower()))
    write_lab_reports(base_dir, ordered)
    update_finalists(data_dir / "finalists.yaml", [asdict(item) for item in ordered])
    render_lab_console(ordered)


def load_words(path: Path, key: str) -> tuple[str, ...]:
    data = load_yaml(path)
    values = data.get(key, [])
    if not isinstance(values, list):
        raise ValueError(f"O arquivo {path.name} deve conter a chave '{key}' como lista.")
    return tuple(normalize_fragment(str(value)) for value in values if normalize_fragment(str(value)))


def load_styles(path: Path) -> list[StyleProfile]:
    data = load_yaml(path)
    items = data.get("styles", [])
    if not isinstance(items, list):
        raise ValueError("styles.yaml deve conter a chave 'styles' como lista.")

    styles: list[StyleProfile] = []
    for item in items:
        if not isinstance(item, dict):
            continue
        styles.append(
            StyleProfile(
                name=str(item["name"]),
                slug=str(item["slug"]),
                inspiration=tuple(str(value) for value in item.get("inspiration", [])),
                patterns=tuple(str(value) for value in item.get("patterns", [])),
                selector=str(item.get("selector", "invented")),
                preferred_letters=tuple(str(value) for value in item.get("preferred_letters", [])),
                preferred_endings=tuple(str(value) for value in item.get("preferred_endings", [])),
                min_length=int(item.get("min_length", 5)),
                max_length=int(item.get("max_length", 10)),
            )
        )
    return styles


def load_yaml(path: Path) -> dict:
    if not path.exists():
        raise FileNotFoundError(f"Arquivo nao encontrado: {path}")
    return yaml.safe_load(path.read_text(encoding="utf-8")) or {}


def generate_lab_results(
    styles: Iterable[StyleProfile],
    pools: FragmentPools,
    validator: BrandValidator,
    limit_per_style: int,
) -> list[BrandLabResult]:
    results: list[BrandLabResult] = []
    seen_names: set[str] = set()

    for style in styles:
        style_candidates = build_candidates_for_style(style, pools, limit_per_style, seen_names)
        logger.info("Gerando %s candidatos para o estilo %s", len(style_candidates), style.name)

        for candidate, pattern in style_candidates:
            validation = validator.validate(candidate)
            breakdown = score_name(candidate, style)
            total = _combine_scores(breakdown, validation)
            results.append(
                BrandLabResult(
                    name=candidate,
                    style=style.name,
                    pattern=pattern,
                    score=total,
                    pronunciation=breakdown.pronunciation,
                    memorization=breakdown.memorization,
                    length=breakdown.length,
                    sonority=breakdown.sonority,
                    originality=breakdown.originality,
                    style_score=breakdown.style,
                    github=validation.github,
                    domain_com=validation.domain_com,
                    domain_com_br=validation.domain_com_br,
                    domain_io=validation.domain_io,
                    domain_dev=validation.domain_dev,
                    domain_app=validation.domain_app,
                    npm=validation.npm,
                    dockerhub=validation.dockerhub,
                )
            )

    return results


def build_candidates_for_style(
    style: StyleProfile,
    pools: FragmentPools,
    limit: int,
    seen_names: set[str],
) -> list[tuple[str, str]]:
    selected_prefixes = select_fragments(style, pools.prefixes, "prefix")
    selected_middles = select_fragments(style, pools.middles, "middle")
    selected_suffixes = select_fragments(style, pools.suffixes, "suffix")

    candidates: list[tuple[str, str]] = []
    pattern_map = {
        "prefix+suffix": (selected_prefixes, None, selected_suffixes),
        "prefix+middle": (selected_prefixes, selected_middles, None),
        "middle+suffix": (None, selected_middles, selected_suffixes),
        "prefix+middle+suffix": (selected_prefixes, selected_middles, selected_suffixes),
        "blend": (selected_prefixes, selected_middles, selected_suffixes),
    }

    for pattern in style.patterns:
        pools_for_pattern = pattern_map.get(pattern)
        if not pools_for_pattern:
            continue

        iterables: list[Iterable[str]] = [pool for pool in pools_for_pattern if pool is not None]
        for combo in islice(product(*iterables), limit * 20):
            candidate = _compose_candidate(combo, pattern)
            normalized = normalize_fragment(candidate)
            if normalized in seen_names:
                continue
            if not is_acceptable(candidate, pools.blacklist):
                continue

            seen_names.add(normalized)
            candidates.append((title_case_name(candidate), pattern))
            if len(candidates) >= limit:
                return candidates

    if len(candidates) < limit:
        for pattern in style.patterns:
            pools_for_pattern = pattern_map.get(pattern)
            if not pools_for_pattern:
                continue
            iterables = [pool for pool in pools_for_pattern if pool is not None]
            for combo in islice(product(*iterables), limit * 50):
                candidate = _compose_candidate(combo, pattern, invented=True)
                normalized = normalize_fragment(candidate)
                if normalized in seen_names:
                    continue
                if not is_acceptable(candidate, pools.blacklist):
                    continue
                seen_names.add(normalized)
                candidates.append((title_case_name(candidate), pattern))
                if len(candidates) >= limit:
                    return candidates

    return candidates


def select_fragments(style: StyleProfile, fragments: tuple[str, ...], kind: str) -> tuple[str, ...]:
    _ = kind
    if style.selector == "minimal":
        filtered = tuple(fragment for fragment in fragments if len(fragment) <= 3)
    elif style.selector == "scandinavian":
        filtered = tuple(fragment for fragment in fragments if any(letter in fragment for letter in "vklrn") or fragment.endswith(("a", "e", "o")))
    elif style.selector == "premium":
        filtered = tuple(fragment for fragment in fragments if fragment.endswith(("a", "e", "o", "ia", "io")) or len(fragment) >= 3)
    elif style.selector == "latin_modern":
        filtered = tuple(fragment for fragment in fragments if 2 <= len(fragment) <= 4)
    else:
        filtered = fragments

    return filtered if len(filtered) >= 6 else fragments


def _compose_candidate(parts: tuple[str, ...], pattern: str, invented: bool = False) -> str:
    candidate = blend_fragments(parts)
    if pattern == "blend" or invented:
        candidate = _invent(candidate)
    return candidate


def _invent(value: str) -> str:
    if not value:
        return value
    if value[-1] not in "aeiouy":
        value = f"{value}a"
    if len(value) > 10:
        return value[:10]
    return value


def _combine_scores(breakdown: ScoreBreakdown, validation: ValidationStatus) -> int:
    score = breakdown.total
    score += 3 if validation.github == "AVAILABLE" else 0
    score += 2 if validation.domain_com == "AVAILABLE" else 0
    score += 2 if validation.domain_com_br == "AVAILABLE" else 0
    score += 1 if validation.domain_io == "AVAILABLE" else 0
    score += 1 if validation.domain_dev == "AVAILABLE" else 0
    score += 1 if validation.domain_app == "AVAILABLE" else 0
    score += 1 if validation.npm == "AVAILABLE" else 0
    score += 1 if validation.dockerhub == "AVAILABLE" else 0
    return min(100, score)


if __name__ == "__main__":
    main()
