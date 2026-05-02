locals {
  env_suffix      = var.environment
  frontend_origin = var.frontend_domain != "" ? "https://${var.frontend_domain}" : "https://panol-frontend-${local.env_suffix}-${data.google_project.current.number}.${var.region}.run.app"

  common_labels = {
    environment = var.environment
    managed_by  = "terraform"
    project     = "panol"
  }

  backend_env = merge({
    APP_DB_ENV           = "supabase"
    APP_PORT             = "8080"
    APP_SECURITY_ENABLED = "false"
    FRONTEND_ORIGIN      = local.frontend_origin
    CORS_ALLOWED_ORIGINS = local.frontend_origin
    DB_SUPABASE_HOST     = var.supabase_db_host
    DB_SUPABASE_PORT     = tostring(var.supabase_db_port)
    DB_SUPABASE_NAME     = var.supabase_db_name
    DB_SUPABASE_USER     = var.supabase_db_user
    DB_SUPABASE_SSL_MODE = var.supabase_db_ssl_mode
  })

  frontend_env = {
    VITE_API_BASE_URL = var.backend_domain != "" ? "https://${var.backend_domain}" : module.backend_service.service_uri
  }
}

data "google_project" "current" {
  project_id = var.gcp_project_id
}

module "artifact_registry" {
  source        = "../../modules/artifact_registry"
  project_id    = var.gcp_project_id
  location      = var.artifact_registry_location
  repository_id = "panol-apps-${local.env_suffix}"
  labels        = local.common_labels
}

module "secret_manager" {
  source     = "../../modules/secret_manager"
  project_id = var.gcp_project_id
  secrets = [
    "DB_SUPABASE_PASSWORD",
    "MONGODB_URI",
    "JWT_ISSUER_URI",
    "VITE_SUPABASE_PUBLISHABLE_KEY"
  ]
}

module "runtime_iam" {
  source                       = "../../modules/iam"
  project_id                   = var.gcp_project_id
  environment                  = var.environment
  service_account_name         = "panol-runtime"
  service_account_display_name = "Panol Runtime ${upper(var.environment)}"
  service_account_user_members = var.runtime_sa_user_members
  roles = [
    "roles/secretmanager.secretAccessor",
    "roles/logging.logWriter",
    "roles/monitoring.metricWriter"
  ]
}

module "backend_service" {
  source                           = "../../modules/cloud_run_service"
  project_id                       = var.gcp_project_id
  region                           = var.region
  service_name                     = "panol-backend-${local.env_suffix}"
  image                            = var.backend_image
  container_port                   = 8080
  labels                           = local.common_labels
  ingress                          = "INGRESS_TRAFFIC_ALL"
  allow_unauthenticated            = true
  service_account_email            = module.runtime_iam.service_account_email
  min_instance_count               = var.backend_min_instances
  max_instance_count               = var.backend_max_instances
  timeout_seconds                  = var.backend_timeout_seconds
  max_instance_request_concurrency = var.backend_concurrency
  env_vars                         = local.backend_env
  secret_env_vars = {
    DB_SUPABASE_PASSWORD = {
      secret  = module.secret_manager.secret_ids["DB_SUPABASE_PASSWORD"]
      version = "latest"
    }
    MONGODB_URI = {
      secret  = module.secret_manager.secret_ids["MONGODB_URI"]
      version = "latest"
    }
    JWT_ISSUER_URI = {
      secret  = module.secret_manager.secret_ids["JWT_ISSUER_URI"]
      version = "latest"
    }
  }
  custom_domain = var.backend_domain
  providers = {
    google      = google
    google-beta = google-beta
  }
}

module "frontend_service" {
  source                           = "../../modules/cloud_run_service"
  project_id                       = var.gcp_project_id
  region                           = var.region
  service_name                     = "panol-frontend-${local.env_suffix}"
  image                            = var.frontend_image
  container_port                   = 80
  labels                           = local.common_labels
  ingress                          = "INGRESS_TRAFFIC_ALL"
  allow_unauthenticated            = true
  service_account_email            = module.runtime_iam.service_account_email
  min_instance_count               = var.frontend_min_instances
  max_instance_count               = var.frontend_max_instances
  timeout_seconds                  = var.frontend_timeout_seconds
  max_instance_request_concurrency = var.frontend_concurrency
  env_vars                         = local.frontend_env
  secret_env_vars = {
    VITE_SUPABASE_PUBLISHABLE_KEY = {
      secret  = module.secret_manager.secret_ids["VITE_SUPABASE_PUBLISHABLE_KEY"]
      version = "latest"
    }
  }
  custom_domain = var.frontend_domain
  providers = {
    google      = google
    google-beta = google-beta
  }
}
