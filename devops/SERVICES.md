# DAAM DevOps — all backend services

Same pattern as Eureka for every microservice.

## Credentials (once)
| ID | Use |
|----|-----|
| `github-daam` | GitHub PAT |
| `dockerhub-daam` | Docker Hub user `lfray` |
| SonarQube server name `SonarQube` | URL `http://host.docker.internal:9000` |

## Jenkins jobs to create

For each row: **New Item** → **Pipeline** → Pipeline script from SCM → same Git repo/`*/master` → set **Script Path**.

| Job name | Script Path | Image | Port (host) |
|----------|-------------|-------|-------------|
| `ci-eureka` | `devops/pipelines/ci-eureka.Jenkinsfile` | `lfray/daam-eureka` | — |
| `cd-eureka` | `devops/pipelines/cd-eureka.Jenkinsfile` | `lfray/daam-eureka` | 8761 |
| `ci-gateway` | `devops/pipelines/ci-gateway.Jenkinsfile` | `lfray/daam-gateway` | — |
| `cd-gateway` | `devops/pipelines/cd-gateway.Jenkinsfile` | `lfray/daam-gateway` | **8088** (maps to 8080) |
| `ci-user` | `devops/pipelines/ci-user.Jenkinsfile` | `lfray/daam-user` | — |
| `cd-user` | `devops/pipelines/cd-user.Jenkinsfile` | `lfray/daam-user` | 8089 |
| `ci-recruitment` | `devops/pipelines/ci-recruitment.Jenkinsfile` | `lfray/daam-recruitment` | — |
| `cd-recruitment` | `devops/pipelines/cd-recruitment.Jenkinsfile` | `lfray/daam-recruitment` | 8090 |
| `ci-reclamation` | `devops/pipelines/ci-reclamation.Jenkinsfile` | `lfray/daam-reclamation` | — |
| `cd-reclamation` | `devops/pipelines/cd-reclamation.Jenkinsfile` | `lfray/daam-reclamation` | 8091 |

Gateway uses host port **8088** so it does not clash with Jenkins on **8080**.

## Suggested order
1. CI then CD for **eureka**
2. CI then CD for **gateway**, **user**, **recruitment**, **reclamation** (CI can be parallel; CD after Eureka is up)

## CD runtime needs
- **MySQL** on the host (`localhost:3306`) with DBs: `us`, `RhhjETC`, `reclamations` (or let Hibernate create them)
- DB user/password as in `application.properties` (`root` / `000000`) unless you override env vars
- Eureka must be running before other services register

## Docker Desktop tip
After reboot, if Docker fails in Jenkins:
```powershell
docker exec -u root jenkins chmod 666 /var/run/docker.sock
```
