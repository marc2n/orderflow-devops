locals {
  aks_monitoring_alerts = {
    OrderflowNodeNotReady = {
      expression = "max by (cluster, node) (kube_node_status_condition{cluster=\"aks-orderflow-dev-frc\",condition=\"Ready\",status=~\"false|unknown\"}) == 1"
      duration   = "PT10M"
      severity   = 2
      summary    = "An AKS node has not been ready for 10 minutes."
      response   = "Check kubectl get nodes, describe the affected node, and inspect node events."
    }

    OrderflowKubeletScrapeFailed = {
      expression = "max by (cluster, instance) (up{cluster=\"aks-orderflow-dev-frc\",job=\"kubelet\"}) == 0"
      duration   = "PT10M"
      severity   = 2
      summary    = "A kubelet metrics target has failed scraping for 10 minutes."
      response   = "Check node readiness and the ama-metrics agent logs."
    }

    OrderflowKubeletTelemetryMissing = {
      expression = "absent_over_time(up{cluster=\"aks-orderflow-dev-frc\",job=\"kubelet\"}[15m])"
      duration   = "PT5M"
      severity   = 2
      summary    = "Kubelet telemetry is missing from managed Prometheus."
      response   = "Check whether the cluster was intentionally stopped, then inspect metrics agents and collection-rule associations."
    }

    OrderflowContainerCrashLoop = {
      expression = "max by (cluster, namespace, pod, container) (max_over_time(kube_pod_container_status_waiting_reason{cluster=\"aks-orderflow-dev-frc\",reason=\"CrashLoopBackOff\"}[5m])) == 1"
      duration   = "PT10M"
      severity   = 2
      summary    = "A container has remained in CrashLoopBackOff for 10 minutes."
      response   = "Describe the pod and inspect current and previous container logs."
    }

    OrderflowPodPending = {
      expression = "max by (cluster, namespace, pod) (kube_pod_status_phase{cluster=\"aks-orderflow-dev-frc\",phase=\"Pending\"}) == 1"
      duration   = "PT15M"
      severity   = 3
      summary    = "A pod has remained pending for 15 minutes."
      response   = "Check scheduling events, resource requests, volume binding and image-pull errors."
    }
  }
}

resource "azurerm_monitor_alert_prometheus_rule_group" "platform" {
  name                = "prom-alerts-orderflow-dev"
  location            = azurerm_monitor_workspace.platform.location
  resource_group_name = azurerm_resource_group.platform.name

  cluster_name       = azurerm_kubernetes_cluster.platform.name
  rule_group_enabled = true
  interval           = "PT1M"

  scopes = [
    azurerm_monitor_workspace.platform.id,
    azurerm_kubernetes_cluster.platform.id,
  ]

  description = "Development alerts for node health, telemetry and pod failures."

  dynamic "rule" {
    for_each = local.aks_monitoring_alerts

    content {
      alert      = rule.key
      enabled    = true
      expression = rule.value.expression
      for        = rule.value.duration
      severity   = rule.value.severity

      labels = {
        environment = "dev"
        project     = "orderflow"
      }

      annotations = {
        summary     = rule.value.summary
        description = rule.value.response
      }

      action {
        action_group_id = azurerm_monitor_action_group.platform.id
      }

      alert_resolution {
        auto_resolved   = true
        time_to_resolve = "PT5M"
      }
    }
  }

  tags = local.tags

  depends_on = [
    azurerm_monitor_data_collection_rule_association.prometheus
  ]
}