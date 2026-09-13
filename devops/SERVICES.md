# DAAM DevOps — backend + frontend

Same pattern for every component: CI then CD.

## Credentials (once)
| ID | Use |
|----|-----|
| `github-daam` | GitHub PAT |
| `dockerhub-daam` | Docker Hub user `lfray` |
| SonarQube server name `SonarQube` | URL `http://host.docker.internal:9000` |

## Jenkins jobs

| Job name | Script Path | Image | Port (host) |
|----------|-------------|-------|-------------|
| `ci-eureka` | `devops/pipelines/ci-eureka.Jenkinsfile` | `lfray/daam-eureka` | — |
| `cd-eureka` | `devops/pipelines/cd-eureka.Jenkinsfile` | `lfray/daam-eureka` | 8761 |
| `ci-gateway` | `devops/pipelines/ci-gateway.Jenkinsfile` | `lfray/daam-gateway` | — |
| `cd-gateway` | `devops/pipelines/cd-gateway.Jenkinsfile` | `lfray/daam-gateway` | **8088** |
| `ci-user` | `devops/pipelines/ci-user.Jenkinsfile` | `lfray/daam-user` | — |
| `cd-user` | `devops/pipelines/cd-user.Jenkinsfile` | `lfray/daam-user` | 8089 |
| `ci-recruitment` | `devops/pipelines/ci-recruitment.Jenkinsfile` | `lfray/daam-recruitment` | — |
| `cd-recruitment` | `devops/pipelines/cd-recruitment.Jenkinsfile` | `lfray/daam-recruitment` | 8090 |
| `ci-reclamation` | `devops/pipelines/ci-reclamation.Jenkinsfile` | `lfray/daam-reclamation` | — |
| `cd-reclamation` | `devops/pipelines/cd-reclamation.Jenkinsfile` | `lfray/daam-reclamation` | 8091 |
| `ci-frontend` | `devops/pipelines/ci-frontend.Jenkinsfile` | `lfray/daam-frontend` | — |
| `cd-frontend` | `devops/pipelines/cd-frontend.Jenkinsfile` | `lfray/daam-frontend` | **4200** |

## Suggested order
1. Eureka → Gateway + microservices → **Frontend** last (API on `:8088`)
2. Open UI: http://localhost:4200

## Docker Desktop tip
```powershell
docker exec -u root jenkins chmod 666 /var/run/docker.sock
```
