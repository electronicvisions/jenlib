node {
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
			} catch (MissingPropertyException ignored) {
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
				sish "exit 1"
			}
			assert (currentBuild.currentResult == "SUCCESS")
		}

		stage('sishTest') {
			// Basic functionality
			assert (sish(script: "hostname", returnStdout: true) == sh(script: "hostname", returnStdout: true))

			// Must not be inside singularity
			String shellEnv = sish(script: "env", returnStdout: true)
			assert (!shellEnv.contains("SINGULARITY_CONTAINER"))

			// Mandatory arguments have to be there
			assertBuildResult("FAILURE") {
				sish(returnStdout: true)
			}

			// For Singularity tests, see stage 'inSingularityTest'
		}

		stage('inSingularityTest') {
			// sish-shell steps are executed in containers
			inSingularity {
				String containerEnv = sish(script: "env", returnStdout: true)
				assert (containerEnv.contains("SINGULARITY_CONTAINER"))
			}

			// Clearing the environment works
			String shellEnv = sish(script: "env", returnStdout: true)
			assert (!shellEnv.contains("SINGULARITY_CONTAINER"))

			// Other commands still work
			inSingularity {
				String currentDirectory = pwd()
				assert (currentDirectory)   // must not be empty
			}

			// Nested containers are forbidden
			assertBuildResult("FAILURE") {
				inSingularity {
					inSingularity {
						sish("hostname")
					}
				}
			}

			// Escaping of sish scripts
			// Escaping of " is not tested since it does not work in plain "sh" steps
			for (command in ['echo $USER', 'echo \'echo hello\'', 'echo "hello\\nworld"']) {
				shOutput = sh(script: command, returnStdout: true)
				inSingularity {
					sishOutput = sish(script: command, returnStdout: true)
				}
				assert (shOutput == sishOutput): "sh: $shOutput != sish: $sishOutput"
			}
		}

		stage("checkPythonPackageTest") {
			inSingularity {
				sish "mkdir lib"

				// Good package
				sish "mkdir lib/good_package"
				sish "echo 'class NiceClass(object):\n    pass' > lib/good_package/__init__.py"
				checkPythonPackage(pkg: "lib/good_package")
				assert (currentBuild.currentResult == "SUCCESS")

				// Bad package
				sish "mkdir lib/bad_package"
				sish "echo 'class uglyclass(): pass' > lib/bad_package/__init__.py"
				assertBuildResult("UNSTABLE") {
					checkPythonPackage(pkg: "lib/bad_package")
				}
			}
		}

		stage("getGerritUsernameTest") {
			// We expect this to be hudson in the general case
			assert (getGerritUsername().equals("hudson"))

			// Local repos have priority
			String repoDir = UUID.randomUUID().toString()
			sish("mkdir -p ${repoDir}")
			sish("cd ${repoDir} && git init")
			sish("cd ${repoDir} && git config gitreview.username foobar")

			dir(repoDir) {
				assert (getGerritUsername().equals("foobar"))
			}

			// Global config not testable without interfering with other builds
		}

		stage("notifyFailureTest") {
			notifyFailure(mattermostChannel: "jenkins-trashbin", nonGerritOnly: true)
			notifyFailure(mattermostChannel: "jenkins-trashbin", nonGerritOnly: false)

			// mattermostChannel is mandatory
			assertBuildResult("FAILURE") {
				notifyFailure()
			}
		}

		stage("onSlurmResourceTest") {
			onSlurmResource(partition: "jenkins") {
				assert (env.NODE_LABELS.contains("swarm"))
			}

			// PWD stays the same
			runOnSlave(label: "frontend") {
				frontendPwd = pwd()
				onSlurmResource(partition: "jenkins") {
					slavePwd = pwd()
					assert (slavePwd == frontendPwd): "slavePwd: $slavePwd, frontendPwd: $frontendPwd"
				}
			}

			// Workspace stays the same
			runOnSlave(label: "frontend") {
				frontendWs = WORKSPACE
				onSlurmResource(partition: "jenkins") {
					slaveWs = WORKSPACE
					assert (slaveWs == frontendWs): "slaveWs: $slaveWs, frontendWs: $frontendWs"
				}
			}

			assertBuildResult("FAILURE") {
				onSlurmResource(partition: "jenkins", nodes: 2) {
					sish "hostname"
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
				stdout = sish(returnStdout: true, script: "waf --help")
				assert (stdout.contains("waf [commands] [options]"))

				inSingularity {
					stdout_singularity = sish(returnStdout: true, script: "waf --help")
				}
				assert (stdout_singularity.contains("waf [commands] [options]"))
			}

			withWaf(gerrit_changes: "3981") {
				stdout = sish(returnStdout: true, script: "waf --help")
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
	notifyFailure(mattermostChannel: "#softies")
}
