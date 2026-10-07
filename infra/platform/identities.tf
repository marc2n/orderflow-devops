resource "azurerm_user_assigned_identity" "aks_control_plane" {
  name                = "id-orderflow-aks-control-dev-frc"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name

  tags = local.tags
}

resource "azurerm_user_assigned_identity" "aks_kubelet" {
  name                = "id-orderflow-aks-kubelet-dev-frc"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name

  tags = local.tags
}