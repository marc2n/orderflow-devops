resource "azurerm_log_analytics_workspace" "platform" {
  name                = "law-orderflow-dev-frc"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name

  sku               = "PerGB2018"
  retention_in_days = 30
  daily_quota_gb    = var.log_analytics_daily_quota_gb

  local_authentication_enabled = false

  internet_ingestion_enabled = true
  internet_query_enabled     = true

  tags = local.tags
}

resource "azurerm_monitor_workspace" "platform" {
  name                = "amw-orderflow-dev-frc"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name

  public_network_access_enabled = true

  tags = local.tags
}

resource "azurerm_monitor_action_group" "platform" {
  name                = "ag-orderflow-dev"
  resource_group_name = azurerm_resource_group.platform.name
  short_name          = "orderflowdev"
  enabled             = true

  email_receiver {
    name                    = "development-admin"
    email_address           = var.monitoring_alert_email
    use_common_alert_schema = true
  }

  tags = local.tags
}