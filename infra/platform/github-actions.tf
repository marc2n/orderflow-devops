resource "azurerm_user_assigned_identity" "github_acr_publish" {
  name                = "id-orderflow-gh-acr-dev-frc"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name

  tags = local.tags
}

resource "azurerm_federated_identity_credential" "github_acr_publish_main" {
  name = "github-orderflow-main"

  user_assigned_identity_id = azurerm_user_assigned_identity.github_acr_publish.id

  issuer   = "https://token.actions.githubusercontent.com"
  audience = ["api://AzureADTokenExchange"]
  subject  = "repo:marc2n@14019628/orderflow-devops@1339648589:ref:refs/heads/main"
}