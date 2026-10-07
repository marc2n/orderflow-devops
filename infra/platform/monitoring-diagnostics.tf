resource "azurerm_monitor_diagnostic_setting" "aks_control_plane" {
  name               = "diag-orderflow-aks-dev"
  target_resource_id = azurerm_kubernetes_cluster.platform.id

  log_analytics_workspace_id     = azurerm_log_analytics_workspace.platform.id
  log_analytics_destination_type = "Dedicated"

  enabled_log {
    category = "kube-apiserver"
  }

  enabled_log {
    category = "kube-controller-manager"
  }

  enabled_log {
    category = "kube-scheduler"
  }
}