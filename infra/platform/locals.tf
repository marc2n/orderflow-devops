locals {
  location = "francecentral"

  tags = {
    project     = "orderflow"
    environment = "dev"
    managed_by  = "terraform"
    purpose     = "application-platform"
  }

  network = {
    vnet_address_space  = ["10.40.0.0/16"]
    aks_subnet_prefixes = ["10.40.0.0/22"]
  }
}