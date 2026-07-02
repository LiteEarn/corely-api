"""Compatibilidade para executar a automação completa do Corely."""

from __future__ import annotations

import sys
from pathlib import Path

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from devops.github.create_project import main as create_project_main


def main() -> int:
    return create_project_main()


if __name__ == "__main__":
    raise SystemExit(main())
