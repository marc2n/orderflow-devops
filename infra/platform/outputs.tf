output "resource_group_name" {
  description = "Resource group containing the development platform."
  value       = azurerm_resource_group.platform.name
}

output "location" {
  description = "Azure region of the development platform."
  value       = azurerm_resource_group.platform.location
}

output "virtual_network_id" {
  description = "Resource ID of the development virtual network."
  value       = azurerm_virtual_network.platform.id
}

output "aks_subnet_id" {
  description = "Resource ID of the AKS node subnet."
  value       = azurerm_subnet.aks.id
}

output "acr_name" {
  description = "Development container registry name."
  value       = azurerm_container_registry.platform.name
}

output "acr_login_server" {
  description = "Registry hostname used in container image references."
  value       = azurerm_container_registry.platform.login_server
}

output "key_vault_name" {
  description = "Development Key Vault name."
  value       = azurerm_key_vault.platform.name
}

output "key_vault_uri" {
  description = "Development Key Vault data-plane endpoint."
  value       = azurerm_key_vault.platform.vault_uri
}

output "aks_name" {
  description = "Development AKS cluster name."
  value       = azurerm_kubernetes_cluster.platform.name
}

output "aks_id" {
  description = "Development AKS resource ID."
  value       = azurerm_kubernetes_cluster.platform.id
}

output "aks_oidc_issuer_url" {
  description = "OIDC issuer used for workload identity federation."
  value       = azurerm_kubernetes_cluster.platform.oidc_issuer_url
}

output "aks_node_resource_group_name" {
  description = "Resource group managed by AKS for node infrastructure."
  value       = azurerm_kubernetes_cluster.platform.node_resource_group
}

output "log_analytics_workspace_id" {
  description = "Resource ID of the development logs workspace."
  value       = azurerm_log_analytics_workspace.platform.id
}

output "azure_monitor_workspace_id" {
  description = "Resource ID of the development Prometheus workspace."
  value       = azurerm_monitor_workspace.platform.id
}

output "monitoring_action_group_id" {
  description = "Resource ID of the development alert action group."
  value       = azurerm_monitor_action_group.platform.id
}