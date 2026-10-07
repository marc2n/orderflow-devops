resource "azurerm_container_registry" "platform" {
  name                = var.acr_name
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name

  sku                           = "Basic"
  admin_enabled                 = false
  public_network_access_enabled = true
  role_assignment_mode          = "LegacyRegistryPermissions"

  tags = local.tags
}