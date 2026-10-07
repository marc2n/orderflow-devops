variable "storage_account_name" {
  description = "Globally unique name for the Terraform state storage account."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9]{3,24}$", var.storage_account_name))
    error_message = "Use 3–24 lowercase letters and digits."
  }
}

variable "state_admin_object_id" {
  description = "Entra object ID of the administrator who manages bootstrap state."
  type        = string

  validation {
    condition = can(regex(
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
      var.state_admin_object_id
    ))
    error_message = "Provide a valid Entra object ID."
  }
}