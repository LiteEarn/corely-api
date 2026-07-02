"""Pontuacao automatica para nomes de marca."""

from __future__ import annotations

from dataclasses import dataclass
from functools import lru_cache
import re

from devops.branding.phonetics import analyze, normalize_fragment


@dataclass(frozen=True, slots=True)
class StyleProfile:
    name: str
    slug: str
    inspiration: tuple[str, ...]
    patterns: tuple[str, ...]
    selector: str
    preferred_letters: tuple[str, ...] = ()
    preferred_endings: tuple[str, ...] = ()
    min_length: int = 5
    max_length: int = 10


@dataclass(frozen=True, slots=True)
class ScoreBreakdown:
    pronunciation: int
    memorization: int
    length: int
    sonority: int
    originality: int
    style: int

    @property
    def total(self) -> int:
        raw_total = self.pronunciation + self.memorization + self.length + self.sonority + self.originality + self.style
        return min(100, round(raw_total * 0.84))


def score_name(value: str, style: StyleProfile) -> ScoreBreakdown:
    analysis = analyze(value)
    name = analysis["name"]
    length = int(analysis["length"])
    vowel_ratio = float(analysis["vowel_ratio"])
    unique_ratio = int(analysis["unique_letters"]) / max(1, int(analysis["letters"]))

    pronunciation = _score_pronunciation(analysis)
    memorization = _score_memorization(name, length, unique_ratio)
    length_score = _score_length(length, style)
    sonority = _score_sonority(name, vowel_ratio)
    originality = _score_originality(name, analysis)
    style_score = _score_style(name, style, analysis)

    return ScoreBreakdown(
        pronunciation=pronunciation,
        memorization=memorization,
        length=length_score,
        sonority=sonority,
        originality=originality,
        style=style_score,
    )


@lru_cache(maxsize=8192)
def _score_pronunciation_signature(signature: str, has_triple: bool, has_repetition: bool, dangerous: bool) -> int:
    score = 20
    if has_triple:
        score -= 8
    if has_repetition:
        score -= 5
    if dangerous:
        score -= 4
    if re.search(r"[^aeiouy]{2,3}$", signature):
        score -= 3
    if signature.endswith(tuple("aeiouy")):
        score += 2
    return max(0, min(20, score))


def _score_pronunciation(analysis: dict[str, int | bool | str]) -> int:
    return _score_pronunciation_signature(
        str(analysis["name"]),
        bool(analysis["has_triple_consonants"]),
        bool(analysis["has_repetition"]),
        bool(analysis["has_dangerous_cluster"]),
    )


def _score_memorization(name: str, length: int, unique_ratio: float) -> int:
    score = 20
    if 6 <= length <= 8:
        score += 2
    score -= int(abs(unique_ratio - 0.72) * 18)
    if re.search(r"(.).\1", name):
        score -= 3
    if re.search(r"(.)\1", name):
        score -= 2
    return max(0, min(20, score))


def _score_length(length: int, style: StyleProfile) -> int:
    if style.min_length <= length <= style.max_length:
        if 6 <= length <= 8:
            return 15
        if length in {5, 9}:
            return 13
        return 11
    if length == style.min_length - 1 or length == style.max_length + 1:
        return 7
    return 0


def _score_sonority(name: str, vowel_ratio: float) -> int:
    score = 20
    if 0.38 <= vowel_ratio <= 0.58:
        score += 1
    elif vowel_ratio < 0.28 or vowel_ratio > 0.7:
        score -= 6
    if re.search(r"[aeiouy]{3,}", name):
        score -= 4
    if name.endswith(tuple("aeiouy")):
        score += 1
    return max(0, min(20, score))


def _score_originality(name: str, analysis: dict[str, int | bool | str]) -> int:
    score = 15
    if int(analysis["unique_letters"]) < 4:
        score -= 5
    if re.search(r"(.)\1", name):
        score -= 2
    if re.search(r"(ing|tion|ware|soft|cloud)$", name):
        score -= 4
    if len(set(name)) >= 6:
        score += 1
    score += sum(ord(char) for char in name) % 5 - 2
    return max(0, min(15, score))


def _score_style(name: str, style: StyleProfile, analysis: dict[str, int | bool | str]) -> int:
    score = 10
    lowered = normalize_fragment(name)
    if style.preferred_letters and any(letter in lowered for letter in style.preferred_letters):
        score += 2
    if style.preferred_endings and any(lowered.endswith(ending) for ending in style.preferred_endings):
        score += 2
    if style.selector == "minimal":
        if int(analysis["length"]) <= 8:
            score += 2
        if bool(analysis["ends_with_vowel"]):
            score += 1
    elif style.selector == "scandinavian":
        if any(letter in lowered for letter in ("v", "k", "l", "r", "n")):
            score += 2
    elif style.selector == "premium":
        if lowered.endswith(tuple(("a", "e", "o", "ia", "io"))):
            score += 2
    elif style.selector == "latin_modern":
        if 5 <= int(analysis["length"]) <= 8:
            score += 2
    elif style.selector == "invented":
        score += 2
    return max(0, min(10, score))
