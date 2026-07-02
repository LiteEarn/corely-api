# ==========================================
# Corely - Create GitHub Project v2
# ==========================================

$ErrorActionPreference = "Stop"

# ------------------------------------------
# Configuração
# ------------------------------------------

$Org = "LiteEarn"
$ProjectTitle = "Corely"

Write-Host ""
Write-Host "========================================="
Write-Host " CORELY - CREATE PROJECT"
Write-Host "========================================="
Write-Host ""

# ------------------------------------------
# Verifica GitHub CLI
# ------------------------------------------

if (!(Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Host "GitHub CLI não encontrado."
    Write-Host ""
    Write-Host "Instale em:"
    Write-Host "https://cli.github.com/"
    exit
}

# ------------------------------------------
# Obtém token da variável de ambiente
# ------------------------------------------

$Token = $env:GITHUB_TOKEN

if ([string]::IsNullOrWhiteSpace($Token)) {
    Write-Host ""
    Write-Host "Variável GITHUB_TOKEN não encontrada."
    Write-Host ""
    Write-Host "Execute:"
    Write-Host ""
    Write-Host 'set GITHUB_TOKEN=SEU_TOKEN'
    exit
}

# ------------------------------------------
# Login automático (caso ainda não esteja logado)
# ------------------------------------------

gh auth status *> $null

if ($LASTEXITCODE -ne 0) {

    Write-Host "Autenticando..."

    $Token | gh auth login --with-token

    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao autenticar."
    }

}

Write-Host "Autenticado com sucesso."
Write-Host ""

# ------------------------------------------
# Obtém ID da organização
# ------------------------------------------

Write-Host "Obtendo Organization ID..."

$Query = @"
query(\$login:String!){
  organization(login:\$login){
    id
  }
}
"@

$OrgResponse = gh api graphql `
    -f query="$Query" `
    -F login="$Org" | ConvertFrom-Json

$OrgId = $OrgResponse.data.organization.id

if (-not $OrgId) {
    throw "Não foi possível obter o ID da organização."
}

Write-Host "Organization ID: $OrgId"
Write-Host ""

# ------------------------------------------
# Cria Project V2
# ------------------------------------------

Write-Host "Criando Project..."

$Mutation = @"
mutation(\$owner:ID!, \$title:String!){
  createProjectV2(input:{
    ownerId:\$owner,
    title:\$title
  }){
    projectV2{
      id
      number
      title
      url
    }
  }
}
"@

$ProjectResponse = gh api graphql `
    -f query="$Mutation" `
    -F owner="$OrgId" `
    -F title="$ProjectTitle" | ConvertFrom-Json

$Project = $ProjectResponse.data.createProjectV2.projectV2

if (-not $Project) {
    throw "Falha ao criar o Project."
}

Write-Host ""
Write-Host "========================================="
Write-Host "PROJECT CRIADO COM SUCESSO"
Write-Host "========================================="
Write-Host ""

Write-Host "Título : $($Project.title)"
Write-Host "Número : $($Project.number)"
Write-Host "ID     : $($Project.id)"
Write-Host ""
Write-Host "URL:"
Write-Host $Project.url
Write-Host ""
Write-Host "========================================="