data "azurerm_client_config" "current" {}

resource "azurerm_key_vault" "platform" {
  name                = var.key_vault_name
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name
  tenant_id           = data.azurerm_client_config.current.tenant_id

  sku_name                      = "standard"
  rbac_authorization_enabled    = true
  public_network_access_enabled = true

  soft_delete_retention_days = 30
  purge_protection_enabled   = true

  network_acls {
    default_action = "Deny"
    bypass         = "None"

    ip_rules                   = var.key_vault_admin_ipv4_cidrs
    virtual_network_subnet_ids = [azurerm_subnet.aks.id]
  }

  tags = local.tags
}