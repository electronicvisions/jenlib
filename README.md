# Jenlib – Shared Library for Visionary Jenkins Pipelines
This project collects shared code between Jenkins Pipelines used within the Electronic Vision(s) group at Heidelberg University.
Have a look at [JenkinsIO/shared-libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) for a comprehensive introduction.

## How to use
Include the library in your script via `@Library("jenlib")`.
All implemented features (should) implement a short example in `test/resources/JenlibGlobalTests.Jenkinsfile`.

## How to contribute
Issues and feature requests are tracked in [OpenProject/jenlib](https://brainscales-r.kip.uni-heidelberg.de/projects/jenlib/work_packages).
Make sure to add an issue for whatever you work on: Things should not get done twice.

Read and understand [JenkinsIO/shared-libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) for the basic concepts.
We support global variables implemented in `var/` as well as arbitrary Groovy code within the `org.electronicvisions.jenlib` package under `src/`.
Make sure to provide `javadoc`-style documentation for all your code and add a unit test in `test/src/` for whatever you add in `src/`.
Add a unit/integration test stage in `test/resources/JenlibGlobalTests.Jenkinsfile` for whatever you add in `var/`.
This test should have explanatory character, users should be able to infer how your step was thought to be used from this test.
If you introduce new dependencies, make sure to add them to the `pom.xml`: Although Jenkins does not use `maven`, the test infrastructure does.
To test your changes, you may push to your sandbox branch (`sandbox/${GERRIT_USERNAME}/master`) and load the library as `@Library("jenlib@sandbox/${GERRIT_USERNAME}/master"`) in an arbitrary job.

## Testing and CI
Core groovy classes in `src/` should be tested by implementing unit tests in `test/src`.
We force 100% line coverage for these.
Since mocking extended pipeline definitions from `var/` in unit tests is painful, there is an additional Jenkinsfile for testing those prior to commit (triggered by gerrit): `test/resources/JenlibGlobalTests.Jenkinsfile`.
This file dynamically loads `jenlib` and can therefore include pending changesets.

In total, there are three CI jobs verifying this library:
* `bld_gerrit-jenlib-unittests` executes `ci/Jenkinsfile` with every submitted changeset
* `bld_gerrit-jenlib-globals` executes `test/resources/JenlibGlobalTests.Jenkinsfile` with every submitted changeset
* `bld_nightly-jenlib-unittests` executes `ci/Jenkinsfile` every night
