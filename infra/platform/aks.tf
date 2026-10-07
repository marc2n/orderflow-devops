resource "azurerm_kubernetes_cluster" "platform" {
  name                = "aks-orderflow-dev-frc"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name
  dns_prefix          = "aks-orderflow-dev-frc"
  node_resource_group = "rg-orderflow-aks-nodes-dev-frc"
  run_command_enabled = false

  sku_tier           = "Free"
  kubernetes_version = var.aks_kubernetes_version

  role_based_access_control_enabled = true
  local_account_disabled            = true
  private_cluster_enabled           = false

  oidc_issuer_enabled       = true
  workload_identity_enabled = true

  automatic_upgrade_channel = null
  node_os_upgrade_channel   = "None"

  azure_active_directory_role_based_access_control {
    tenant_id          = data.azurerm_client_config.current.tenant_id
    azure_rbac_enabled = true
  }

  api_server_access_profile {
    authorized_ip_ranges = var.aks_admin_ipv4_cidrs
  }

  identity {
    type = "UserAssigned"

    identity_ids = [
      azurerm_user_assigned_identity.aks_control_plane.id
    ]
  }

  kubelet_identity {
    client_id = azurerm_user_assigned_identity.aks_kubelet.client_id
    object_id = azurerm_user_assigned_identity.aks_kubelet.principal_id

    user_assigned_identity_id = azurerm_user_assigned_identity.aks_kubelet.id
  }

  default_node_pool {
    name           = "system"
    vm_size        = "Standard_D4s_v6"
    node_count     = 2
    vnet_subnet_id = azurerm_subnet.aks.id

    auto_scaling_enabled = false

    os_sku          = "AzureLinux"
    os_disk_type    = "Managed"
    os_disk_size_gb = 64

    max_pods = 30

    zones = ["1", "2"]

    tags = local.tags

    upgrade_settings {
      max_surge                     = "1"
      drain_timeout_in_minutes      = 30
      node_soak_duration_in_minutes = 0
    }
  }

  network_profile {
    network_plugin      = "azure"
    network_plugin_mode = "overlay"
    network_data_plane  = "cilium"

    pod_cidr       = "10.244.0.0/16"
    service_cidr   = "10.245.0.0/16"
    dns_service_ip = "10.245.0.10"

    load_balancer_sku = "standard"
    outbound_type     = "loadBalancer"

    load_balancer_profile {
      managed_outbound_ip_count = 1
    }
  }

  key_vault_secrets_provider {
    secret_rotation_enabled  = true
    secret_rotation_interval = "5m"
  }

  tags = local.tags

  monitor_metrics {}

  oms_agent {
    log_analytics_workspace_id      = azurerm_log_analytics_workspace.platform.id
    msi_auth_for_monitoring_enabled = true
  }

  depends_on = [
    azurerm_subnet_network_security_group_association.aks,
    azurerm_role_assignment.aks_subnet_network,
    azurerm_role_assignment.aks_nsg_network,
    azurerm_role_assignment.aks_kubelet_operator,
    azurerm_role_assignment.aks_acr_pull
  ]

}