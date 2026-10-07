variable "acr_name" {
  description = "Globally unique Azure Container Registry name."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9]{5,50}$", var.acr_name))
    error_message = "Use 5–50 lowercase letters and digits."
  }
}

variable "key_vault_name" {
  description = "Globally unique Key Vault name."
  type        = string

  validation {
    condition = (
      can(regex("^[a-z][a-z0-9-]{1,22}[a-z0-9]$", var.key_vault_name)) &&
      !strcontains(var.key_vault_name, "--")
    )
    error_message = "Use 3–24 lowercase letters, digits or hyphens; start with a letter, end with a letter or digit, and avoid consecutive hyphens."
  }
}

variable "key_vault_admin_object_id" {
  description = "Entra object ID of the administrator managing development secrets."
  type        = string

  validation {
    condition = can(regex(
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
      var.key_vault_admin_object_id
    ))
    error_message = "Provide a valid Entra object ID."
  }
}

variable "key_vault_admin_ipv4_cidrs" {
  description = "Public administrative IPv4 addresses allowed through the vault firewall, each as /32."
  type        = set(string)

  validation {
    condition = (
      length(var.key_vault_admin_ipv4_cidrs) > 0 &&
      alltrue([
        for cidr in var.key_vault_admin_ipv4_cidrs :
        can(cidrnetmask(cidr)) && endswith(cidr, "/32")
      ])
    )
    error_message = "Provide at least one public IPv4 address with a /32 prefix."
  }
}