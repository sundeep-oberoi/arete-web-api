# Description
API backend component for the multistep web form.

# Architecture
- Deployed as a Docker container, repo must also have a Docker file
- Choose a base image from verified publishers
- API component is built using Java, Spring Boot Web, & Logback frameworks
- Add debug and info logging
- Use "/api/msf/" as the base path
- Use an embedded H2 database with persistence
- Initialize the database on first startup
- Add an environment variable to force re-initialize the database on startup
- Keep database files on a persistent docker volume
- Add a Spring Actuator health endpoint
- Package as a Maven project
- Use com.arete.webapi as the base Java package
- Add build plugins to create a all-in-one Jar

# Steps
- Read the requirements in the file "API-SPEC.md"
- Read additional requirements in the file "SPECIFICATIONS.md"
- Write a "PLAN.md"
- Write the code
- Write tests, code coverage must be at least 60%
- Run tests to check for errors
- Fix any errors