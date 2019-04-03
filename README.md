# Jenlib – Shared Library for Visionary Jenkins Pipelines

[![Unit Test Status](https://jenkins.bioai.eu/buildStatus/icon?job=bld_nightly-jenlib-unittests&subject=Unit%20Tests)](https://jenkins.bioai.eu/view/nightly/job/bld_nightly-jenlib-unittests)
[![Global Test Status](https://jenkins.bioai.eu/buildStatus/icon?job=bld_nightly-jenlib-globals&subject=Global%20Tests)](https://jenkins.bioai.eu/view/nightly/job/bld_nightly-jenlib-globals)

This project collects shared code between Jenkins Pipelines used within the Electronic Vision(s) group at Heidelberg University.
Have a look at [JenkinsIO/shared-libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) for a comprehensive introduction.

## How to use
Include the library in your *Jenkinsfile* via `@Library("jenlib") _`.
All implemented features (should) implement a short example in `test/resources/JenlibGlobalTests.Jenkinsfile`.

References for all implemented global variables can be found [here](https://jenkins.bioai.eu/view/nightly/job/bld_nightly-jenlib-globals/pipeline-syntax/globals) (rendered nightly).

API documentation is nightly deployed on an internal Jenkins server.
If you have access, have a look [here](https://jenkins.bioai.eu/job/bld_nightly-jenlib-unittests/Jenlib_20Documentation).bld_gerrit-jenlib-unittests


## How to contribute
Issues and feature requests are tracked in [OpenProject/jenlib](https://openproject.bioai.eu/projects/jenlib/work_packages).
Make sure to add an issue for whatever you work on: Things should not get done twice.

Read and understand [JenkinsIO/shared-libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) for the basic concepts.
We support global variables implemented in `var/` as well as arbitrary Groovy code within the `org.electronicvisions.jenlib` package under `src/`.
Make sure to provide *javadoc*-style documentation for all your code and add tests (see [Testing and CI](#testing-and-ci) below).

If you introduce new dependencies, make sure to add them to the `pom.xml`: Although Jenkins does not use *maven*, the test infrastructure does.

As soon as you feel confident about your change, submit a change request in gerrit.
Pull requests via GitHub are welcome but outside our internal review process and need manual interaction.
Don't hesitate to drop a message if responses are lacking.


## Testing and CI
Core groovy classes in `src/` should be tested by implementing unit tests in `test/src`.
We force 100% line coverage for these.

Since mocking extended pipeline definitions from `var/` in unit tests is painful, there is an additional Jenkinsfile for testing those prior to commit (triggered by gerrit): `test/resources/JenlibGlobalTests.Jenkinsfile`.
This file dynamically loads *jenlib* and can therefore include pending changesets.
Tests you add for global variables should have explanatory character, users should be able to infer how your step was thought to be used from this test.

To test your changes manually, you may push to your sandbox branch (`sandbox/${GERRIT_USERNAME}/master`) and load the library as `@Library("jenlib@sandbox/${GERRIT_USERNAME}/master") _`) in an arbitrary job.

In total, there are four CI jobs verifying this library:
* `bld_gerrit-jenlib-unittests` executes `ci/Jenkinsfile` with every submitted changeset
* `bld_gerrit-jenlib-globals` executes `test/resources/JenlibGlobalTests.Jenkinsfile` with every submitted changeset
* `bld_nightly-jenlib-unittests` executes `ci/Jenkinsfile` every night
* `bld_nightly-jenlib-globals` executes `test/resources/JenlibGlobalTests.Jenkinsfile` every night


## License
```
Jenlib – Shared Library for Visionary Jenkins Pipelines
Copyright (C) 2018-2019 Electronic Vision(s) Group
                        Kirchhoff-Institute for Physics
                        Ruprecht-Karls-Universität Heidelberg
                        69120 Heidelberg
                        Germany

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
```
