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