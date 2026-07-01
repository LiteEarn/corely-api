"""Clientes e heuristicas para triagem de nomes de marca."""

from __future__ import annotations

from dataclasses import dataclass
from html.parser import HTMLParser
import logging
import re
import unicodedata
from typing import Iterable
from urllib.parse import quote_plus

import requests

logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class AvailabilityCheck:
    value: str
    status: str


@dataclass(frozen=True, slots=True)
class WebRiskCheck:
    value: str
    risk: str


class _DuckDuckGoResultParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self._capture_text = False
        self._current_text: list[str] = []
        self.results: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attr_map = dict(attrs)
        classes = attr_map.get("class", "") or ""
        if tag == "a" and "result__a" in classes.split():
            self._capture_text = True
            self._current_text = []

    def handle_endtag(self, tag: str) -> None:
        if tag == "a" and self._capture_text:
            text = " ".join(part.strip() for part in self._current_text).strip()
            if text:
                self.results.append(text)
            self._capture_text = False
            self._current_text = []

    def handle_data(self, data: str) -> None:
        if self._capture_text:
            self._current_text.append(data)


class BrandingClient:
    def __init__(self, timeout: int = 5) -> None:
        self._timeout = timeout
        self._session = requests.Session()
        self._session.headers.update(
            {
                "User-Agent": "CorelyBrandingBot/1.0",
                "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            }
        )

    def check_github(self, name: str) -> AvailabilityCheck:
        slug = self._slugify_github_name(name)
        url = f"https://github.com/{slug}"
        return AvailabilityCheck(value=url, status=self._check_taken_by_http(url))

    def check_domain(self, name: str, tld: str) -> AvailabilityCheck:
        domain = self._build_domain(name, tld)
        status = self._check_domain_via_dns(domain)
        return AvailabilityCheck(value=domain, status=status)

    def assess_web_risk(self, name: str) -> WebRiskCheck:
        query = quote_plus(f'"{name}" empresa')
        url = f"https://html.duckduckgo.com/html/?q={query}"

        try:
            response = self._session.get(url, timeout=self._timeout)
            response.raise_for_status()
        except requests.RequestException as exc:
            logger.warning("Falha ao consultar busca web para %s: %s", name, exc)
            return WebRiskCheck(value=name, risk="MEDIUM")

        parser = _DuckDuckGoResultParser()
        parser.feed(response.text)
        risk = self._classify_risk(name, parser.results)
        return WebRiskCheck(value=name, risk=risk)

    def _check_taken_by_http(self, url: str) -> str:
        try:
            response = self._session.get(url, timeout=self._timeout, allow_redirects=True)
            if response.status_code == 404:
                return "AVAILABLE"
            return "TAKEN"
        except requests.RequestException as exc:
            logger.warning("Falha ao consultar %s: %s", url, exc)
            return "TAKEN"

    def _check_rdap_domain(self, url: str) -> str:
        try:
            response = self._session.get(url, timeout=self._timeout)
            if response.status_code == 404:
                return "AVAILABLE"
            if response.status_code == 200:
                return "TAKEN"
            return "TAKEN"
        except requests.RequestException as exc:
            logger.warning("Falha ao consultar RDAP %s: %s", url, exc)
            return "TAKEN"

    def _check_domain_via_dns(self, domain: str) -> str:
        url = f"https://dns.google/resolve?name={domain}&type=A"
        try:
            response = self._session.get(url, timeout=self._timeout)
            response.raise_for_status()
            payload = response.json()
        except requests.RequestException as exc:
            logger.warning("Falha ao consultar DNS %s: %s", domain, exc)
            return "TAKEN"
        except ValueError as exc:
            logger.warning("Resposta DNS invalida para %s: %s", domain, exc)
            return "TAKEN"

        if payload.get("Status") == 3:
            return "AVAILABLE"
        return "TAKEN"

    def _classify_risk(self, name: str, results: Iterable[str]) -> str:
        normalized_name = self._normalize_text(name)
        if not normalized_name:
            return "MEDIUM"

        matched_results = 0
        strong_matches = 0

        for result in results:
            normalized_result = self._normalize_text(result)
            if normalized_name not in normalized_result:
                continue
            matched_results += 1
            if normalized_result == normalized_name or self._looks_like_company_name(normalized_result, normalized_name):
                strong_matches += 1

        if strong_matches >= 1:
            return "HIGH"
        if matched_results >= 2:
            return "MEDIUM"
        if matched_results == 1:
            return "MEDIUM"
        return "LOW"

    def _looks_like_company_name(self, result: str, name: str) -> bool:
        common_markers = (" ltda", " sa", " s.a", " inc", " llc", " empresa", " grupo")
        return result.startswith(name) or any(marker in result for marker in common_markers)

    def _build_domain(self, name: str, tld: str) -> str:
        slug = self._slugify_domain_name(name)
        return f"{slug}.{tld.lstrip('.')}"

    def _slugify_github_name(self, name: str) -> str:
        slug = self._slugify_domain_name(name)
        slug = re.sub(r"-+", "-", slug).strip("-")
        return slug or "candidate"

    def _slugify_domain_name(self, name: str) -> str:
        normalized = unicodedata.normalize("NFKD", name)
        ascii_name = normalized.encode("ascii", "ignore").decode("ascii")
        slug = re.sub(r"[^a-zA-Z0-9]+", "-", ascii_name).strip("-")
        return slug.lower()

    def _normalize_text(self, value: str) -> str:
        normalized = unicodedata.normalize("NFKD", value)
        ascii_text = normalized.encode("ascii", "ignore").decode("ascii")
        return re.sub(r"[^a-z0-9]+", " ", ascii_text.lower()).strip()
