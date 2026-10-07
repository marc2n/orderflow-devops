terraform {
  backend "azurerm" {
    storage_account_name = "storderflowdevopstfdev1"
    container_name       = "tfstate"
    key                  = "dev/platform/terraform.tfstate"

    use_azuread_auth = true
    use_cli          = true
  }
}