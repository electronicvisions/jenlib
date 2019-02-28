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
				jesh "exit 1"
			}
			assert (currentBuild.currentResult == "SUCCESS")
		}

		stage('jeshTest') {
			// Basic functionality
			assert (jesh(script: "hostname", returnStdout: true) == sh(script: "hostname", returnStdout: true))

			// Must not be inside singularity
			String shellEnv = jesh(script: "env", returnStdout: true)
			assert (!shellEnv.contains("SINGULARITY_CONTAINER"))

			// Mandatory arguments have to be there
			assertBuildResult("FAILURE") {
				jesh(returnStdout: true)
			}

			// For Singularity tests, see stage 'inSingularityTest'
		}

		stage('decodeBase64Test') {
			assert (decodeBase64("Zm9vYmFy") == "foobar")
		}

		stage('encodeBase64Test') {
			assert (encodeBase64("barfoo") == "YmFyZm9v")
		}

		stage('inSingularityTest') {
			// jesh-shell steps are executed in containers
			inSingularity {
				String containerEnv = jesh(script: "env", returnStdout: true)
				assert (containerEnv.contains("SINGULARITY_CONTAINER"))
			}

			// Clearing the environment works
			String shellEnv = jesh(script: "env", returnStdout: true)
			assert (!shellEnv.contains("SINGULARITY_CONTAINER"))

			// Other commands still work
			inSingularity {
				String currentDirectory = pwd()
				assert (currentDirectory)   // must not be empty
			}

			// Escaping of jesh scripts
			// Escaping of " is not tested since it does not work in plain "sh" steps
			for (command in ['echo $USER', 'echo \'echo hello\'', 'echo "hello\\nworld"']) {
				shOutput = sh(script: command, returnStdout: true)
				inSingularity {
					jeshOutput = jesh(script: command, returnStdout: true)
				}
				assert (shOutput == jeshOutput): "sh: $shOutput != jesh: $jeshOutput"
			}
		}

		stage("checkPythonPackageTest") {
			inSingularity {
				jesh "mkdir lib"

				// Good package
				jesh "mkdir lib/good_package"
				jesh "echo 'class NiceClass(object):\n    pass' > lib/good_package/__init__.py"
				checkPythonPackage(pkg: "lib/good_package")
				assert (currentBuild.currentResult == "SUCCESS")

				// Bad package
				jesh "mkdir lib/bad_package"
				jesh "echo 'class uglyclass(): pass' > lib/bad_package/__init__.py"
				assertBuildResult("UNSTABLE") {
					checkPythonPackage(pkg: "lib/bad_package")
				}
			}
		}

		stage("getDefaultContainerPathTest") {
			// Default without "In-Container:" in commit message
			withEnv(["GERRIT_CHANGE_COMMIT_MESSAGE=${encodeBase64('')}"]) {
				assert getDefaultContainerPath() == "/containers/stable/latest"
			}

			// "In-Container:" specified in commit message
			String pathCustomImage = "/foo/bar/path"

			// NOTE; additional white-space in the commit-messages is intended
			List<String> commitMessagesSuccess = [
					"""
				Fake commit message subject

				Here be dragons!

				In-Container: ${pathCustomImage}

				Change-Id: 12345678
				""",

					"""
				Fake commit message subject

				Here be dragons!

				In-Container:${pathCustomImage}     

				Change-Id: 12345678
				""",

					"""
				Fake commit message subject

				Change-Id: 12345678
				In-Container:       ${pathCustomImage}     
				"""
			]

			for (String fakeCommitMessage : commitMessagesSuccess) {
				String encodedFakeCommitMessage = encodeBase64(fakeCommitMessage.stripIndent())

				withEnv(["GERRIT_CHANGE_COMMIT_MESSAGE=$encodedFakeCommitMessage"]) {
					String containerPath = getDefaultContainerPath()
					assert containerPath == pathCustomImage:
							"Expected $pathCustomImage, but container path is $containerPath."
				}
			}

			List<String> commitMessagesFail = [
					"""
				Fake commit message subject

				Here be dragons! And multiple In-Container statements!

				In-Container:${pathCustomImage}     
				In-Container: ${pathCustomImage}

				Change-Id: 12345678
				""",
			]

			for (String fakeCommitMessage : commitMessagesFail) {
				String encodedFakeCommitMessage = encodeBase64(fakeCommitMessage.stripIndent())

				assertBuildResult("FAILURE") {
					withEnv(['GERRIT_CHANGE_COMMIT_MESSAGE=' + encodedFakeCommitMessage]) {
						getDefaultContainerPath()
					}
				}
			}
		}

		stage("getContainerAppsTest") {
			assert getContainerApps().contains("visionary-dls")
			assert getContainerApps(getDefaultContainerPath()).contains("visionary-dls")
		}

		stage("wafDefaultPipelineTest") {
			// Test build a seldom altered project with minimal dependencies and a stable CI flow
			wafDefaultPipeline(projects: ["frickel-dls@v3testing"],
			                   container: [app: "visionary-dls"],
			                   notificationChannel: "#jenkins-trashbin")

			// Unsupported command line options
			assertBuildResult("FAILURE") {
				// No pipeline without projects
				wafDefaultPipeline()
			}
			assertBuildResult("FAILURE") {
				// 'projects' has to be of type List<String>
				wafDefaultPipeline(projects: "frickel-dls")
			}
			assertBuildResult("FAILURE") {
				// 'notificationChannel' argument is mandatory
				wafDefaultPipeline(projects: ["frickel-dls"],
				                   container: [app: "visionary-dls"])
			}
			assertBuildResult("FAILURE") {
				// container app has to be specified
				wafDefaultPipeline(projects: ["frickel-dls"],
				                   notificationChannel: "#jenkins-trashbin")
			}
			assertBuildResult("FAILURE") {
				// No pipeline without projects
				wafDefaultPipeline(projects: [])
			}
			assertBuildResult("FAILURE") {
				// Target may not be modified, the pipeline runs for default and '*' internally
				wafDefaultPipeline(projects: ["frickel-dls@v3testing"],
				                   container: [app: "visionary-dls"],
				                   notificationChannel: "#jenkins-trashbin",
				                   configureInstallOptions: "--target='*'")
			}
			assertBuildResult("FAILURE") {
				// Target may not be modified, the pipeline runs for default and '*' internally
				wafDefaultPipeline(projects: ["frickel-dls@v3testing"],
				                   container: [app: "visionary-dls"],
				                   notificationChannel: "#jenkins-trashbin",
				                   testOptions: "--target='*'")
			}
			assertBuildResult("FAILURE") {
				// Test handling may not be modified, the pipeline does it internally
				wafDefaultPipeline(projects: ["frickel-dls@v3testing"],
				                   container: [app: "visionary-dls"],
				                   notificationChannel: "#jenkins-trashbin",
				                   configureInstallOptions: "--test-execnone")
			}
		}

		stage("wafSetupTest") {
			// Test checkout a seldom altered project with minimal dependencies and a stable CI flow
			wafSetup(projects: ["frickel-dls@v3testing"])

			// Multiple projects
			wafSetup(projects: ["frickel-dls@v3testing", "hicann-dls-scripts@v3testing"])

			// Unsupported command line options
			assertBuildResult("FAILURE") {
				wafSetup()
			}
			assertBuildResult("FAILURE") {
				wafSetup(projects: "frickel-dls")
			}
		}

		stage("withModulesTest") {
			noModulePath = jesh(script: 'echo $PATH', returnStdout: true)

			withModules(modules: ["localdir"]) {
				localdirPath = jesh(script: 'echo $PATH', returnStdout: true)
			}
			assert (noModulePath != localdirPath): "$noModulePath should not be $localdirPath"

			withModules(modules: ["localdir"]) {
				withModules(purge: true, modules: []) {
					purgedLocaldirPath = jesh(script: 'echo $PATH', returnStdout: true)
				}
			}
			assert (noModulePath == purgedLocaldirPath): "$noModulePath should be $purgedLocaldirPath"

			withModules(modules: [], prependModulePath: "foo/bar") {
				assert (jesh(script: "echo \$MODULEPATH", returnStdout: true).contains("foo/bar"))
			}

			// Fail if module load does not succeed
			assertBuildResult("FAILURE") {
				withModules(modules: ["jenlibNonExistingModule"]) {}
			}

			// Fail early on bad input
			assertBuildResult("FAILURE") {
				// 'modules' has to be of type List<String>
				withModules(modules: "git") {}
			}
			assertBuildResult("FAILURE") {
				// 'purge' has to be of type boolean
				withModules(purge: "git") {}
			}
			assertBuildResult("FAILURE") {
				// 'moduleInitPath' has to be of type String
				withModules(moduleInitPath: true) {}
			}
		}

		stage("getGerritUsernameTest") {
			// We expect this to be hudson in the general case
			assert (getGerritUsername().equals("hudson"))

			// Local repos have priority
			String repoDir = UUID.randomUUID().toString()
			jesh("mkdir -p ${repoDir}")
			jesh("cd ${repoDir} && git init")
			jesh("cd ${repoDir} && git config gitreview.username foobar")

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
					jesh "hostname"
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
				stdout = jesh(returnStdout: true, script: "waf --help")
				assert (stdout.contains("waf [commands] [options]"))

				inSingularity {
					stdout_singularity = jesh(returnStdout: true, script: "waf --help")
				}
				assert (stdout_singularity.contains("waf [commands] [options]"))
			}

			withWaf(gerrit_changes: "3981") {
				stdout = jesh(returnStdout: true, script: "waf --help")
				assert (stdout.contains("waf [commands] [options]"))
			}
		}

		stage("fillTemplateTest") {
			template = 'Hello <% out.print firstname %> ${lastname}'
			result = fillTemplate(template, [firstname: "Jenkins", lastname: "Hudson"])
			assert (result == 'Hello Jenkins Hudson'): result
		}

		stage('deployModuleTest') {
			jesh "mkdir -p $WORKSPACE/source"
			jesh "mkdir -p $WORKSPACE/source/bin"
			jesh "mkdir -p $WORKSPACE/source/lib"
			jesh "echo '#!/bin/bash\necho bla' > $WORKSPACE/source/bin/test_executable"
			jesh "chmod +x $WORKSPACE/source/bin/test_executable"

			inSingularity() {
				deployModule([name      : "testmodule",
				              moduleRoot: "$WORKSPACE/module",
				              targetRoot: "$WORKSPACE/install",
				              source    : "$WORKSPACE/source"])
			}

			withEnv(["MODULEPATH+LOCAL=$WORKSPACE/module"]) {
				assert (jesh(returnStdout: true,
				             script: "module load testmodule && test_executable").contains("bla"))
			}

			withModules(modules: ["testmodule"],
			            prependModulePath: "$WORKSPACE/module") {
				assert (jesh(returnStdout: true,
				             script: "echo \$LD_LIBRARY_PATH").contains("$WORKSPACE/install/testmodule"))
			}

			withModules(modules: ["testmodule"],
			            prependModulePath: "$WORKSPACE/module") {
				inSingularity() {
					assert (jesh(returnStdout: true,
					             script: "echo \$LD_LIBRARY_PATH").contains("$WORKSPACE/install/testmodule"))
				}
			}

			// test increasing counter of module directory
			num_before = sish(returnStdout: true,
			                  script: "find $WORKSPACE/install/testmodule/* -maxdepth 0 -type d | wc -l").toInteger()
			inSingularity() {
				deployModule([name      : "testmodule",
				              moduleRoot: "$WORKSPACE/module",
				              targetRoot: "$WORKSPACE/install",
				              source    : "$WORKSPACE/source"])
			}
			num_after = sish(returnStdout: true,
			                 script: "find $WORKSPACE/install/testmodule/* -maxdepth 0 -type d | wc -l").toInteger()
			assert (num_before == 1)
			assert (num_after == 2)

			// test fail without being in inSingularity closure
			assertBuildResult("FAILURE") {
				deployModule([name      : "testmodule",
				              moduleRoot: "$WORKSPACE/module",
				              targetRoot: "$WORKSPACE/install",
				              source    : "$WORKSPACE/source"])
			}
			jesh "rm -rf $WORKSPACE/install $WORKSPACE/module $WORKSPACE/source"
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
