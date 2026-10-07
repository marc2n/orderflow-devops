resource "azurerm_virtual_network" "platform" {
  name                = "vnet-orderflow-dev-frc"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name
  address_space       = local.network.vnet_address_space

  tags = local.tags
}

resource "azurerm_subnet" "aks" {
  name                            = "snet-aks"
  resource_group_name             = azurerm_resource_group.platform.name
  virtual_network_name            = azurerm_virtual_network.platform.name
  address_prefixes                = local.network.aks_subnet_prefixes
  default_outbound_access_enabled = false

  service_endpoints = ["Microsoft.KeyVault"]
}

resource "azurerm_network_security_group" "aks" {
  name                = "nsg-orderflow-aks-dev-frc"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name

  tags = local.tags
}

resource "azurerm_subnet_network_security_group_association" "aks" {
  subnet_id                 = azurerm_subnet.aks.id
  network_security_group_id = azurerm_network_security_group.aks.id
}