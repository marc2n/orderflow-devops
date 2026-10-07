locals {
  container_insights_streams = [
    "Microsoft-ContainerLogV2",
    "Microsoft-KubeEvents",
    "Microsoft-KubePodInventory",
  ]
}

resource "azurerm_monitor_data_collection_rule" "container_insights" {
  name                = "MSCI-${local.location}-${azurerm_kubernetes_cluster.platform.name}"
  location            = azurerm_log_analytics_workspace.platform.location
  resource_group_name = azurerm_resource_group.platform.name

  description = "Collect Orderflow container logs, Kubernetes events and pod inventory."

  destinations {
    log_analytics {
      name                  = "logs-workspace"
      workspace_resource_id = azurerm_log_analytics_workspace.platform.id
    }
  }

  data_sources {
    extension {
      name           = "ContainerInsightsExtension"
      extension_name = "ContainerInsights"
      streams        = local.container_insights_streams

      extension_json = jsonencode({
        dataCollectionSettings = {
          interval               = "5m"
          namespaceFilteringMode = "Off"
          namespaces             = []
          enableContainerLogV2   = true
        }
      })
    }
  }

  data_flow {
    streams      = local.container_insights_streams
    destinations = ["logs-workspace"]
  }

  tags = local.tags
}

resource "azurerm_monitor_data_collection_rule_association" "container_insights" {
  name                    = "ContainerInsightsExtension"
  target_resource_id      = azurerm_kubernetes_cluster.platform.id
  data_collection_rule_id = azurerm_monitor_data_collection_rule.container_insights.id

  description = "Connect Orderflow AKS to its container log collection rule."
}