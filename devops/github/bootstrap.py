"""Executa a configuração completa do GitHub do Corely."""

from __future__ import annotations

import logging
import sys
from pathlib import Path
from typing import Callable

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.add_epics_to_project import main as add_epics_to_project_main
from devops.github.configure_project import main as configure_project_main
from devops.github.create_epics import main as create_epics_main
from devops.github.create_labels import main as create_labels_main
from devops.github.create_milestones import main as create_milestones_main
from devops.github.create_project import main as create_project_main
from devops.github.create_project_fields import main as create_project_fields_main


logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


STEP_RUNNERS: tuple[tuple[str, Callable[[], int]], ...] = (
    ("create_project", create_project_main),
    ("create_labels", create_labels_main),
    ("create_milestones", create_milestones_main),
    ("create_epics", create_epics_main),
    ("create_project_fields", create_project_fields_main),
    ("configure_project", configure_project_main),
    ("add_epics_to_project", add_epics_to_project_main),
)


def main() -> int:
    success = 0
    failure = 0

    for name, runner in STEP_RUNNERS:
        try:
            result = runner()
            if result == 0:
                success += 1
                logger.info("Etapa concluída: %s", name)
            else:
                failure += 1
                logger.error("Etapa com erro: %s", name)
        except Exception as exc:  # pragma: no cover - defensivo
            failure += 1
            logger.error("Falha inesperada em %s: %s", name, exc)

    logger.info("Resumo final: sucesso=%s falha=%s", success, failure)
    return 0 if failure == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
