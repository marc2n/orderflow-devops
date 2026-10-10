locals {
  workload_namespace = "orderflow-dev"

  secret_workloads = {
    mysql = {
      identity_name   = "id-orderflow-mysql-dev-frc"
      service_account = "mysql"
    }

    order_service = {
      identity_name   = "id-orderflow-order-service-dev-frc"
      service_account = "order-service"
    }
  }

  workload_secret_access = {
    mysql_root = {
      workload    = "mysql"
      secret_name = "orderflow-mysql-root-password"
    }

    mysql_app = {
      workload    = "mysql"
      secret_name = "orderflow-mysql-app-password"
    }

    order_service_app = {
      workload    = "order_service"
      secret_name = "orderflow-mysql-app-password"
    }
  }
}

resource "azurerm_user_assigned_identity" "workload" {
  for_each = local.secret_workloads

  name                = each.value.identity_name
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name

  tags = local.tags
}

resource "azurerm_federated_identity_credential" "workload" {
  for_each = local.secret_workloads

  name = "aks-${each.value.service_account}"

  user_assigned_identity_id = azurerm_user_assigned_identity.workload[each.key].id

  issuer   = azurerm_kubernetes_cluster.platform.oidc_issuer_url
  audience = ["api://AzureADTokenExchange"]
  subject  = "system:serviceaccount:${local.workload_namespace}:${each.value.service_account}"
}

resource "azurerm_role_assignment" "workload_secret_reader" {
  for_each = local.workload_secret_access

  scope = "${azurerm_key_vault.platform.id}/secrets/${each.value.secret_name}"

  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_user_assigned_identity.workload[each.value.workload].principal_id
  principal_type       = "ServicePrincipal"
}