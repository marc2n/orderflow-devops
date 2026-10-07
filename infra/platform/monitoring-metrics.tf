resource "azurerm_monitor_data_collection_endpoint" "prometheus" {
  name                = "dce-orderflow-prom-dev-frc"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name

  kind                          = "Linux"
  public_network_access_enabled = true

  tags = local.tags
}

resource "azurerm_monitor_data_collection_rule" "prometheus" {
  name                = "dcr-orderflow-prom-dev-frc"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name

  kind                        = "Linux"
  data_collection_endpoint_id = azurerm_monitor_data_collection_endpoint.prometheus.id
  description                 = "Route Orderflow AKS metrics to managed Prometheus."

  destinations {
    monitor_account {
      name               = "prometheus-workspace"
      monitor_account_id = azurerm_monitor_workspace.platform.id
    }
  }

  data_sources {
    prometheus_forwarder {
      name    = "prometheus-source"
      streams = ["Microsoft-PrometheusMetrics"]
    }
  }

  data_flow {
    streams      = ["Microsoft-PrometheusMetrics"]
    destinations = ["prometheus-workspace"]
  }

  tags = local.tags
}

resource "azurerm_monitor_data_collection_rule_association" "prometheus" {
  name                    = "orderflow-prometheus"
  target_resource_id      = azurerm_kubernetes_cluster.platform.id
  data_collection_rule_id = azurerm_monitor_data_collection_rule.prometheus.id

  description = "Connect Orderflow AKS to its Prometheus collection rule."
}