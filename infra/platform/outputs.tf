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