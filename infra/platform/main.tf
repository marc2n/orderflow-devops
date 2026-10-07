resource "azurerm_resource_group" "platform" {
  name     = "rg-orderflow-dev-frc"
  location = local.location

  tags = local.tags
}