# Eureka DevOps

See also `devops/SERVICES.md` for gateway, user, recruitment, reclamation.

## Files
- `backend/eureka/Dockerfile`
- `devops/pipelines/ci-eureka.Jenkinsfile` — clean, build, Sonar, docker push
- `devops/pipelines/cd-eureka.Jenkinsfile` — docker pull + run

## Jenkins job `ci-eureka`
- SCM Git: `https://github.com/khalil1363/platform_de_recrutement_PFA.git`
- Branch: `*/master`
- Script Path: `devops/pipelines/ci-eureka.Jenkinsfile`
- Lightweight checkout
- Shallow clone depth 1, timeout 60, no tags

## Credentials
- `github-daam` — GitHub PAT
- `dockerhub-daam` — Docker Hub user `lfray`
- SonarQube server name: `SonarQube` — URL `http://host.docker.internal:9000`

## Image
`lfray/daam-eureka:latest`

## Note (Docker Desktop + Jenkins container)
Maven/Sonar stages mount volume `jenkins_home:/var/jenkins_home` (not `-v $PWD:/app`), otherwise sibling containers see an empty workspace.
