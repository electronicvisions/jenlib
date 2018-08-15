node {
	// Default to false to ensure failure messages are sent
	GERRIT_BUILD = false

	try {
		cleanWs()

		stage('Load Library') {

			/**
			 * Temporary sandbox branch for gerrit changesets
			 */
			String tmp_branch_name = "sandbox/hudson/tmp_" + UUID.randomUUID().toString()

			try {
				/**
				 * Workaround for JENKINS-50433
				 * Jenkins' Shared library git checkout plugin always adds '-t -h' to 'git ls-remote'.
				 * It's therefore not possible to get gerrit changes that are referenced as 'refs/changes'.
				 *
				 * We therefore checkout the change in a local repository, push that state to a temporary
				 * branch at 'refs/heads', use that branch for loading the shared library and finally delete it.
				 */
				dir("jenlib_tmp") {
					git url: "ssh://hudson@${GERRIT_HOST}:${GERRIT_PORT}/jenlib.git"
					sh "git fetch ssh://hudson@${GERRIT_HOST}:${GERRIT_PORT}/jenlib ${GERRIT_REFSPEC} && git checkout FETCH_HEAD"
					sh "git checkout -b ${tmp_branch_name}"
					sh "git push --no-thin -u origin ${tmp_branch_name}"
				}

				library "jenlib@${tmp_branch_name}"

				GERRIT_BUILD = true
			} catch (MissingPropertyException ignored) {
				GERRIT_BUILD = false
				library 'jenlib'
			} finally {
				// Cleanup temporary branch
				dir("jenlib_tmp") {
					sh "git push --delete origin ${tmp_branch_name} || exit 0"
				}
			}
		}

		stage('setBuildStateTest') {
			for (result in ["NOT_BUILT", "UNSTABLE", "SUCCESS", "FAILURE", "ABORTED"]) {
				assert (currentBuild.currentResult == "SUCCESS")
				setBuildState(result)
				assert (currentBuild.currentResult == result)
				setBuildState("SUCCESS")
			}
		}

		stage('assertBuildResultTest') {
			assert (currentBuild.currentResult == "SUCCESS")

			// Check if all supported states work
			for (result in ["NOT_BUILT", "UNSTABLE", "SUCCESS", "FAILURE", "ABORTED"]) {
				assertBuildResult(result) {
					setBuildState(result)
				}
			}
			assert (currentBuild.currentResult == "SUCCESS")

			// Expected exceptions should be catched
			assertBuildResult("FAILURE") {
				sh "exit 1"
			}
			assert (currentBuild.currentResult == "SUCCESS")
		}

		stage("checkPythonPackageTest") {
			sh "mkdir lib"

			// Good package
			sh "mkdir lib/good_package"
			sh "echo 'class NiceClass(object):\n    pass' > lib/good_package/__init__.py"
			checkPythonPackage(pkg: "lib/good_package")
			assert (currentBuild.currentResult == "SUCCESS")

			// Bad package
			sh "mkdir lib/bad_package"
			sh "echo 'class uglyclass(): pass' > lib/bad_package/__init__.py"
			assertBuildResult("UNSTABLE") {
				checkPythonPackage(pkg: "lib/bad_package")
			}
		}

		stage("onSlurmResourceTest") {
			onSlurmResource(partition: "jenkins") {
				assert (env.NODE_LABELS.contains("swarm"))
			}

			assertBuildResult("FAILURE") {
				onSlurmResource(partition: "jenkins", nodes: 2) {
					sh "hostname"
				}
			}
		}

		stage("runOnSlaveTest") {
			// Raise for bad user options
			bad_inputs = [[:], [naame: "hel"], [laabel: "frontend"],
			              [name: "hel", label: "frontend"],
			              [naame: "hel", label: "frontend"],
			              [name: "hel", laabel: "frontend"],
			              [name: "hel", label: "frontend", foo: "bar"]]

			for (input in bad_inputs) {
				assertBuildResult("FAILURE") {
					runOnSlave(input) {}
				}
			}

			// Pipeline runs on a node => switching to master should be possible
			runOnSlave(name: "master") {
				assert (env.NODE_NAME == "master")
			}
			runOnSlave(label: "master") {
				assert (env.NODE_NAME == "master")
			}

			// Make sure we stay on the same executor
			pipeline_executor = env.EXECUTOR_NUMBER
			runOnSlave(name: env.NODE_NAME) {
				assert (env.EXECUTOR_NUMBER == pipeline_executor)
			}
		}

		stage('withWafTest') {
			withWaf() {
				stdout = sh(returnStdout: true, script: "waf --help")
				assert (stdout.contains("waf [commands] [options]"))
			}

			withWaf(gerrit_changes: "3981") {
				stdout = sh(returnStdout: true, script: "waf --help")
				assert (stdout.contains("waf [commands] [options]"))
			}
		}
	} catch (Exception e) {
		post_error_build_action()
		throw e
	} finally {
		post_all_build_action()
	}

	// Some Jenkins steps fail a build without raising (e.g. archiveArtifacts)
	if (currentBuild.currentResult != "SUCCESS") {
		post_error_build_action()
	}
}

/*
/* HELPER FUNCTIONS
*/

void post_all_build_action() {
	// Always clean the workspace
	cleanWs()
}

void post_error_build_action() {
	if (!GERRIT_BUILD) {
		mattermostSend(channel: "#softies",
		               text: "@channel Jenkins build `${env.JOB_NAME}` has failed!",
		               message: "${env.BUILD_URL}",
		               endpoint: "https://brainscales-r.kip.uni-heidelberg.de:6443/hooks/qrn4j3tx8jfe3dio6esut65tpr")
	}
}
