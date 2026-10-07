resource "azurerm_role_assignment" "aks_subnet_network" {
  scope                = azurerm_subnet.aks.id
  role_definition_name = "Network Contributor"
  principal_id         = azurerm_user_assigned_identity.aks_control_plane.principal_id
  principal_type       = "ServicePrincipal"
}

resource "azurerm_role_assignment" "aks_nsg_network" {
  scope                = azurerm_network_security_group.aks.id
  role_definition_name = "Network Contributor"
  principal_id         = azurerm_user_assigned_identity.aks_control_plane.principal_id
  principal_type       = "ServicePrincipal"
}

resource "azurerm_role_assignment" "aks_kubelet_operator" {
  scope                = azurerm_user_assigned_identity.aks_kubelet.id
  role_definition_name = "Managed Identity Operator"
  principal_id         = azurerm_user_assigned_identity.aks_control_plane.principal_id
  principal_type       = "ServicePrincipal"
}

resource "azurerm_role_assignment" "aks_acr_pull" {
  scope                = azurerm_container_registry.platform.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_user_assigned_identity.aks_kubelet.principal_id
  principal_type       = "ServicePrincipal"
}

resource "azurerm_role_assignment" "key_vault_admin" {
  scope                = azurerm_key_vault.platform.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = var.key_vault_admin_object_id
  principal_type       = "User"
}

resource "azurerm_role_assignment" "aks_admin_credentials" {
  scope                = azurerm_kubernetes_cluster.platform.id
  role_definition_name = "Azure Kubernetes Service Cluster User Role"
  principal_id         = var.aks_admin_object_id
  principal_type       = "User"
}

resource "azurerm_role_assignment" "aks_admin" {
  scope                = azurerm_kubernetes_cluster.platform.id
  role_definition_name = "Azure Kubernetes Service RBAC Cluster Admin"
  principal_id         = var.aks_admin_object_id
  principal_type       = "User"
}

resource "azurerm_role_assignment" "github_acr_publish" {
  scope                = azurerm_container_registry.platform.id
  role_definition_name = "AcrPush"

  principal_id   = azurerm_user_assigned_identity.github_acr_publish.principal_id
  principal_type = "ServicePrincipal"
}