"""Validador automatico reutilizando o cliente existente."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable

from devops.branding.branding_client import BrandingClient


@dataclass(frozen=True, slots=True)
class ValidationStatus:
    github: str
    domain_com: str
    domain_com_br: str
    domain_io: str
    domain_dev: str
    domain_app: str
    npm: str
    dockerhub: str


@dataclass(frozen=True, slots=True)
class ValidationResult:
    name: str
    status: ValidationStatus


class BrandValidator:
    def __init__(self, client: BrandingClient | None = None) -> None:
        self._client = client or BrandingClient()

    def validate(self, name: str) -> ValidationStatus:
        github = self._client.check_github(name).status
        domain_com = self._client.check_domain(name, "com").status
        domain_com_br = self._client.check_domain(name, "com.br").status
        domain_io = self._client.check_domain(name, "io").status
        domain_dev = self._client.check_domain(name, "dev").status
        domain_app = self._client.check_domain(name, "app").status
        npm = self._client.check_npm_package(name).status
        dockerhub = self._client.check_dockerhub(name).status

        return ValidationStatus(
            github=github,
            domain_com=domain_com,
            domain_com_br=domain_com_br,
            domain_io=domain_io,
            domain_dev=domain_dev,
            domain_app=domain_app,
            npm=npm,
            dockerhub=dockerhub,
        )

    def validate_many(self, names: Iterable[str]) -> list[ValidationResult]:
        return [ValidationResult(name=name, status=self.validate(name)) for name in names]
