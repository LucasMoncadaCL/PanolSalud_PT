# Secrets policy

Store sensitive values in this folder and never commit real values.

Required files:
- db_password.txt: password used by postgres container in docker-compose.
- application-secrets.properties: backend-only secrets loaded by Spring Boot (`spring.config.import`).

Example `application-secrets.properties`:
DB_PASSWORD=replace_me
# Optional if needed:
# JWT_ISSUER_URI=https://your-project.supabase.co/auth/v1