# Jenlib – Shared Library for Visionary Jenkins Pipelines
This project collects shared code between Jenkins Pipelines used within the Electronic Vision(s) group at Heidelberg University.
Have a look at [JenkinsIO/shared-libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) for a comprehensive introduction.

## How to use
Include the library in your script via `@Library("jenlib")`.
All implemented features (should) implement a short example in `test/resources/JenlibTest.Jenkinsfile`.

## How to contribute
Read and understand [JenkinsIO/shared-libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) for the basic concepts.
We support global variables implemented in `var/` as well as arbitrary Groovy code within the `org.electronicvisions.jenlib` package under `src/`.
Make sure to provide `javadoc`-style documentation for all your code and add a unit test in `test/src/` for whatever you add in `src/`.
Add a unit/integration test stage in `test/resources/JenlibGlobalTests.Jenkinsfile` for whatever you add in `var/`.
Provide a short example for new features as a stage in `test/resources/JenlibTest.Jenkinsfile`.
If you introduce new dependencies, make sure to add them to the `pom.xml`: Although Jenkins does not use `maven`, the test infrastructure does.
To test your changes, you may push to your sandbox branch (`sandbox/${GERRIT_USERNAME}/master`) and load the library as `@Library("jenlib@sandbox/${GERRIT_USERNAME}/master"`) in an arbitrary job.

## Testing and CI
Core groovy classes in `src/` should be tested by implementing unit tests in `test/src`.
Since mocking extended pipeline definitions from `var/` in unit tests is painful, there is an additional Jenkinsfile for testing those prior to commit (triggered by gerrit): `test/resources/JenlibGlobalTests.Jenkinsfile`.
This file dynamically loads `jenlib` and can therefore include pending changesets.
`JenlibExamples.Jenkinsfile` cannot be used for this purpose since classes from `src/` are not (trivially) usable from dynamic imports (they have to be available at compile-time).

In total, there are four CI jobs verifying this library:
* `bld_gerrit-jenlib` executes `ci/Jenkinsfile` with every submitted changeset
* `bld_gerrit-jenlib-globals` executes `test/resources/JenlibGlobalTests.Jenkinsfile` with every submitted changeset
* `bld_nightly-jenlib` executes `ci/Jenkinsfile` every night
* `bld_nightly-jenlib-examples` executes `test/resources/JenlibExamples.Jenkinsfile` and ensures that all examples run with `HEAD`
