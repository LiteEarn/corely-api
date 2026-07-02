@echo off
setlocal

REM ===========================================
REM CONFIGURAÇÃO
REM ===========================================

set ORG=LiteEarn
set PROJECT_NAME=Corely

REM O token deve estar na variável de ambiente GITHUB_TOKEN
if "%GITHUB_TOKEN%"=="" (
    echo.
    echo ERRO: Variavel GITHUB_TOKEN nao encontrada.
    echo.
    echo Execute antes:
    echo.
    echo set GITHUB_TOKEN=SEU_TOKEN
    echo.
    pause
    exit /b 1
)

echo Autenticando no GitHub...

echo %GITHUB_TOKEN% | gh auth login --with-token

echo.

echo Criando labels...

gh label create "epic" --color 5319E7 --force
gh label create "story" --color 1D76DB --force
gh label create "bug" --color D73A4A --force
gh label create "enhancement" --color A2EEEF --force
gh label create "tech-debt" --color F9D0C4 --force

gh label create "backend" --color 0E8A16 --force
gh label create "frontend" --color FBCA04 --force
gh label create "database" --color C5DEF5 --force
gh label create "dashboard" --color 0052CC --force
gh label create "finance" --color B60205 --force
gh label create "ux" --color D4C5F9 --force
gh label create "security" --color 000000 --force
gh label create "ai" --color 5319E7 --force

gh label create "priority:critical" --color B60205 --force
gh label create "priority:high" --color D93F0B --force
gh label create "priority:medium" --color FBCA04 --force
gh label create "priority:low" --color 0E8A16 --force

echo.
echo Criando milestones...

gh api repos/%ORG%/corely-api/milestones ^
-f title="MVP"

gh api repos/%ORG%/corely-api/milestones ^
-f title="Beta"

gh api repos/%ORG%/corely-api/milestones ^
-f title="Produção"

gh api repos/%ORG%/corely-api/milestones ^
-f title="Mobile"

gh api repos/%ORG%/corely-api/milestones ^
-f title="IA"

echo.
echo Criando Epicos...

gh issue create ^
--title "EPIC - Fundação" ^
--body "Epic principal da fundação do sistema." ^
--label epic

gh issue create ^
--title "EPIC - Cadastros" ^
--body "Studios, usuários, alunos, instrutores e objetivos." ^
--label epic

gh issue create ^
--title "EPIC - Agenda Operacional" ^
--body "Turmas, sessões, agenda e presença." ^
--label epic

gh issue create ^
--title "EPIC - Reposições" ^
--body "Solicitação, aprovação e utilização de reposições." ^
--label epic

gh issue create ^
--title "EPIC - Dashboard" ^
--body "Dashboard operacional e financeiro." ^
--label epic

gh issue create ^
--title "EPIC - Financeiro" ^
--body "Mensalidades, pagamentos e fluxo de caixa." ^
--label epic

gh issue create ^
--title "EPIC - Relatórios" ^
--body "Relatórios operacionais e financeiros." ^
--label epic

gh issue create ^
--title "EPIC - Mobile" ^
--body "Aplicativo para alunos e instrutores." ^
--label epic

gh issue create ^
--title "EPIC - IA" ^
--body "Funcionalidades com Inteligência Artificial." ^
--label epic

gh issue create ^
--title "EPIC - Infraestrutura" ^
--body "CI/CD, observabilidade e arquitetura." ^
--label epic

echo.
echo Finalizado.
pause