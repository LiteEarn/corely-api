"""Regras foneticas para filtragem e composicao de nomes."""

from __future__ import annotations

from functools import lru_cache
import re
import unicodedata
from typing import Iterable

VOWELS = frozenset("aeiouy")
BLACKLIST_MARKERS = frozenset()
DANGEROUS_CLUSTERS = (
    "brr",
    "cct",
    "dth",
    "gth",
    "jkh",
    "ksh",
    "mpt",
    "npt",
    "ptl",
    "qv",
    "rth",
    "tch",
    "tsk",
    "xq",
    "zq",
)


def normalize_fragment(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value)
    ascii_text = normalized.encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z0-9]", "", ascii_text.lower())


def blend_fragments(parts: Iterable[str]) -> str:
    raw = "".join(normalize_fragment(part) for part in parts if part)
    raw = re.sub(r"(.)\1+", r"\1", raw)
    raw = re.sub(r"[^a-z0-9]", "", raw)
    return raw[:10]


def title_case_name(value: str) -> str:
    clean = normalize_fragment(value)
    return clean[:1].upper() + clean[1:]


@lru_cache(maxsize=8192)
def analyze(value: str) -> dict[str, int | bool | str]:
    name = normalize_fragment(value)
    vowels = sum(1 for char in name if char in VOWELS)
    consonants = sum(1 for char in name if char.isalpha() and char not in VOWELS)
    unique_letters = len(set(name))
    letters = sum(1 for char in name if char.isalpha())

    return {
        "name": name,
        "length": len(name),
        "vowels": vowels,
        "consonants": consonants,
        "letters": letters,
        "unique_letters": unique_letters,
        "vowel_ratio": round(vowels / letters, 4) if letters else 0,
        "has_triple_consonants": _has_triple_consonants(name),
        "has_repetition": _has_repetition(name),
        "has_dangerous_cluster": any(cluster in name for cluster in DANGEROUS_CLUSTERS),
        "ends_with_vowel": bool(name and name[-1] in VOWELS),
        "syllable_count": _estimate_syllables(name),
    }


def is_acceptable(value: str, blacklist: Iterable[str]) -> bool:
    name = normalize_fragment(value)
    if len(name) < 5 or len(name) > 10:
        return False
    if not name:
        return False

    lowered = name.lower()
    for entry in blacklist:
        marker = normalize_fragment(entry)
        if marker and marker in lowered:
            return False

    analysis = analyze(name)
    if analysis["has_triple_consonants"]:
        return False
    if analysis["has_repetition"]:
        return False
    if analysis["has_dangerous_cluster"]:
        return False
    if analysis["syllable_count"] < 2:
        return False
    return True


def _has_triple_consonants(value: str) -> bool:
    return bool(re.search(r"[^aeiouy]{3,}", value))


def _has_repetition(value: str) -> bool:
    if re.search(r"(.)\1{2,}", value):
        return True
    return bool(re.search(r"(.{2,})\1", value))


def _estimate_syllables(value: str) -> int:
    groups = re.findall(r"[aeiouy]+", value)
    return max(1, len(groups))
