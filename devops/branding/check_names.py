"""Executa a triagem automatica de nomes de empresa."""

from __future__ import annotations

from dataclasses import dataclass
import logging
from pathlib import Path
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed

import yaml
from dotenv import load_dotenv

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.branding.branding_client import BrandingClient
from devops.branding.report import BrandAssessment, render_console_report, write_csv, write_markdown

logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class ScoreWeights:
    github: int = 25
    com: int = 30
    com_br: int = 20
    io: int = 10
    low_risk: int = 15
    medium_risk: int = 7
    high_risk: int = 0


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
    load_dotenv()

    base_dir = Path(__file__).resolve().parent
    candidates = load_candidates(base_dir / "candidates.yaml")
    client = BrandingClient()
    weights = ScoreWeights()

    assessments: list[BrandAssessment] = []
    with ThreadPoolExecutor(max_workers=min(12, max(1, len(candidates)))) as executor:
        futures = {executor.submit(build_assessment, client, candidate, weights): candidate for candidate in candidates}
        for future in as_completed(futures):
            assessments.append(future.result())

    ordered = sorted(assessments, key=lambda item: (-item.score, item.name.lower()))

    write_csv(base_dir / "results.csv", ordered)
    write_markdown(base_dir / "results.md", ordered)
    render_console_report(ordered)


def load_candidates(path: Path) -> list[str]:
    if not path.exists():
        raise FileNotFoundError(f"Arquivo nao encontrado: {path}")

    data = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    raw_candidates = data.get("candidates")
    if raw_candidates is None:
        raw_candidates = data.get("company_names", [])
    if not isinstance(raw_candidates, list):
        raise ValueError("O arquivo candidates.yaml deve conter uma lista em 'candidates' ou 'company_names'.")

    candidates = [str(item).strip() for item in raw_candidates if str(item).strip()]
    if not candidates:
        logger.warning("Nenhum candidato informado em candidates.yaml.")
    return candidates


def build_assessment(client: BrandingClient, candidate: str, weights: ScoreWeights) -> BrandAssessment:
    github = client.check_github(candidate)
    domain_com = client.check_domain(candidate, "com")
    domain_com_br = client.check_domain(candidate, "com.br")
    domain_io = client.check_domain(candidate, "io")
    risk = client.assess_web_risk(candidate)

    score = 0
    score += weights.github if github.status == "AVAILABLE" else 0
    score += weights.com if domain_com.status == "AVAILABLE" else 0
    score += weights.com_br if domain_com_br.status == "AVAILABLE" else 0
    score += weights.io if domain_io.status == "AVAILABLE" else 0

    if risk.risk == "LOW":
        score += weights.low_risk
    elif risk.risk == "MEDIUM":
        score += weights.medium_risk
    else:
        score += weights.high_risk

    return BrandAssessment(
        name=candidate,
        github=github.status,
        domain_com=domain_com.status,
        domain_com_br=domain_com_br.status,
        domain_io=domain_io.status,
        risk=risk.risk,
        score=min(score, 100),
    )


if __name__ == "__main__":
    main()
