# Package Diagram

```plantuml
@startuml aims_backend_packages
!pragma layout smetana
title Пакетная диаграмма backend AIMS

skinparam linetype ortho
skinparam shadowing false
skinparam packageStyle rectangle
left to right direction

package "gov.mib.aims.backend" as backend {
  package "bootstrap" as bootstrap {}

  package "config" as config {
    package "dbqueue" as config_dbqueue {}
  }

  package "controller" as controller {}
  package "entity" as entity {}
  package "exception" as exception {}

  package "generated" as generated {
    package "api" as generated_api {}
    package "model" as generated_model {}
  }

  package "model" as model {}
  package "repository" as repository {}
  package "security" as security {}

  package "services" as services {
    package "cleanup.status" as cleanup_status {
      package "postaction" as cleanup_postaction {}
      package "precondition" as cleanup_precondition {}
    }

    package "dbqueue" as services_dbqueue {
      package "processor" as dbqueue_processor {}
    }

    package "incident" as services_incident {
      package "status" as incident_status {
        package "postaction" as incident_postaction {}
        package "precondition" as incident_precondition {}
      }
    }

    package "mapping" as mapping {}
    package "validation" as validation {}
  }
}

generated_api ....> generated_model

controller ....> generated
controller ....> services

services ....> generated_model
services ....> repository
services ....> security
services ....> entity
services ....> model
services ....> exception

repository ....> entity
repository ....> model
entity ....> model
model ....> exception

security ....> repository
security ....> entity

config ....> services
config ....> security

bootstrap ....> repository

cleanup_postaction ....> services_dbqueue
cleanup_postaction ....> incident_postaction
cleanup_precondition ....> incident_precondition

incident_postaction ....> services_dbqueue

incident_precondition ....> validation
services_incident ....> services_dbqueue

@enduml
```

## Генерация диаграммы

```bash
curl -L https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar
mkdir -p /tmp/aims-package-render

# SVG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tsvg -o /tmp/aims-package-render docs/package.md

# PNG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tpng -o /tmp/aims-package-render docs/package.md
```
