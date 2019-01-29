try {
	stage('Cleanup') {
		node(label: "frontend") {
			cleanWs()
		}
	}

	stage('Load Library') {
		node(label: "frontend") {
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
	}

	runOnSlave(label: "frontend") {
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

		stage('setJobDescriptionTest') {
			String tmpDescription = getJobDescription()
			String testDescription = UUID.randomUUID().toString()

			setJobDescription(testDescription)
			assert (getJobDescription() == testDescription)

			setJobDescription(tmpDescription)
			assert (getJobDescription() == tmpDescription)
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

		stage("pipelineFromMarkdownTest") {
			String tempFilePath = ""
			runOnSlave(label: "frontend") {
				tempFilePath = "${WORKSPACE}/${UUID.randomUUID().toString()}"
				writeFile(file: tempFilePath,
				          text: libraryResource("org/electronicvisions/MarkdownScriptExtractorTest.md"))
			}

			pipelineFromMarkdown(markdownFilePath: tempFilePath, blockType: "shell")
		}

		stage('isWeekendTest') {
			boolean bashIsWeekend = jesh(script: "[[ \$(date +%u) -lt 6 ]]", returnStatus: true)
			boolean jenlibIsWeekend = isWeekend()
			assert (jenlibIsWeekend == bashIsWeekend): "Bash says weekend: ${bashIsWeekend}, " +
			                                           "jenlib: ${jenlibIsWeekend}"
		}

		stage('isAsicJenkinsTest') {
			// This file can only run on F9 jenkins
			assert isAsicJenkins() == false: "ASIC Jenkins detected, but running on Softie Jenkins."
		}

		stage('withCcacheTest') {
			withCcache() {
				inSingularity(app: "visionary-wafer") {
					jesh(script: "ln -s \$(which gcc) ccache")
					ccacheVersion = jesh(script: "./ccache --version | head -n1", returnStdout: true)
					jesh(script: "rm -f ccache")
					assert (ccacheVersion.contains("ccache")): "$ccacheVersion does not contain 'ccache'"
				}
			}

			// Fail if ccacheNoHashDir is not boolean
			assertBuildResult("FAILURE") {
				withCcache(ccacheNoHashDir: "no") {}
			}
		}

		stage("isTriggeredByGerritTest") {
			// We assume that this pipeline is never triggered from an upstream job, otherwise this test will fail!
			assert (isTriggeredByGerrit() == (env.GERRIT_CHANGE_NUMBER ? true : false))
		}

		stage("isTriggeredByUserActionTest") {
			// We assume that this pipeline is never triggered manually, otherwise this test will fail!
			assert (isTriggeredByUserAction() == false):
					"Manual trigger detected, disable 'isTriggeredByUserActionTest' when running this pipeline manually!"
		}

		stage("addBuildParameterTest") {
			String parameterName = UUID.randomUUID().toString()
			String parameterValue = UUID.randomUUID().toString()
			assert (params.get(parameterName) == null)

			// Check parameters can be added
			addBuildParameter(string(name: parameterName, defaultValue: parameterValue))
			assert (params.get(parameterName) == parameterValue): "Build parameter was not added."

			// Cleanup: Remove all build parameters: This pipeline is not supposed to have any
			removeAllBuildParameters()
		}

		stage("removeAllBuildParametersTest") {
			// Add some parameter
			addBuildParameter(string(name: "foo", defaultValue: "bar"))
			assert (params.foo == "bar"): "Could not add build parameter."

			// Remove all parameters, this pipeline is not supposed to have any
			removeAllBuildParameters()
			assert (params.foo == null): "Build parameter survived removal."

			// Make sure removal works if none were present
			removeAllBuildParameters()
		}

		stage('checkPatternInFileTest') {
			String testFile = UUID.randomUUID().toString()
			writeFile(file: testFile, text: "foo\tbar")

			// Pattern matches, no effect on build
			checkPatternInFile("^foo\tbar\$", testFile)

			// Missing file, fail build
			assertBuildResult("FAILURE") {
				checkPatternInFile("^foo\tbar\$", UUID.randomUUID().toString())
			}

			// Pattern does not match, unstable build
			assertBuildResult("UNSTABLE") {
				checkPatternInFile("^foo bar\$", testFile)
			}
		}

		stage('checkPatternNotInFileTest') {
			String testFile = UUID.randomUUID().toString()
			writeFile(file: testFile, text: "foo\tbar")

			// Pattern matches, unstable build
			assertBuildResult("UNSTABLE") {
				checkPatternNotInFile("^foo\tbar\$", testFile)
			}

			// Missing file, fail build
			assertBuildResult("FAILURE") {
				checkPatternNotInFile("^foo\tbar\$", UUID.randomUUID().toString())
			}

			// Pattern does not match, no effect on build
			checkPatternNotInFile("^foo bar\$", testFile)
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

		stage("deployDocumentationRemoteTest") {
			repositoryUrl = "ssh://hudson@brainscales-r.kip.uni-heidelberg.de:29418/jenlib"
			upstreamBranch = "sandbox/hudson/deploy_documentation_test"
			jesh "mkdir upstream"
			dir ("upstream") {
				jesh "git init"
				jesh "git commit --allow-empty -m 'Documentation'"
				jesh "git push -f ${repositoryUrl} HEAD:${upstreamBranch}"
			}
			jesh "mkdir docu"
			content = jesh(script: "echo -n 'my docu ' \$(date)", returnStdout: true)
			jesh "echo -n '${content}' > docu/docu.txt"
			deployDocumentationRemote([folders: ["docu"],
			                          repositoryUrl: repositoryUrl,
			                          upstreamBranch: upstreamBranch])
			jesh "git clone --branch ${upstreamBranch} ${repositoryUrl}"
			assert fileExists("jenlib/docu/docu.txt")
			String docu_content = jesh(script: "cat jenlib/docu/docu.txt", returnStdout: true)
			assert docu_content.contains(content)
			jesh "rm -rf docu jenlib upstream"
		}

		stage('withWafTest') {
			withWaf() {
				stdout = jesh(returnStdout: true, script: "waf --help")
				assert (stdout.contains("waf [commands] [options]"))

				// nested withWaf
				withWaf() {
					stdout = jesh(returnStdout: true, script: "waf --help")
					assert (stdout.contains("waf [commands] [options]"))
				}

				inSingularity {
					stdout_singularity = jesh(returnStdout: true, script: "waf --help")
				}
				assert (stdout_singularity.contains("waf [commands] [options]"))
			}
		}

		stage("wafSetupTest") {
			// Test checkout a seldom altered project with minimal dependencies and a stable CI flow
			wafSetup(projects: ["frickel-dls@v3testing"])

			// Multiple projects
			wafSetup(projects: ["frickel-dls@v3testing", "hicann-dls-scripts@v3testing"])

			// Setup in subfolder
			String subfolder = UUID.randomUUID().toString()
			dir(subfolder) {
				wafSetup(projects: ["hate"])
			}
			assert fileExists("${subfolder}/wscript")

			// Unsupported command line options
			assertBuildResult("FAILURE") {
				wafSetup()
			}
			assertBuildResult("FAILURE") {
				wafSetup(projects: "frickel-dls")
			}
		}

		stage('checkClangFormatTest') {
			dir ("good_repo") {
				jesh "git init"
				jesh "echo initial > initial && git add ."
				jesh "git commit -m='first'"
				jesh "echo 'void function() {}' > file.h"
				jesh "git add ."
				jesh "git commit -m='second'"
			}

			assertBuildResult("SUCCESS") {
				inSingularity(app: "dls") {
					checkClangFormat(folder: "good_repo")
				}
			}

			assertBuildResult("SUCCESS") {
				inSingularity(app: "dls") {
					checkClangFormat(folder: "good_repo", fullDiff: true)
				}
			}

			dir ("no_change_repo") {
				jesh "git init"
				jesh "echo initial > initial && git add ."
				jesh "git commit -m='first'"
				jesh "echo 'def fun(): pass; foo()' > file.py"
				jesh "git add ."
				jesh "git commit -m='second'"
			}

			assertBuildResult("SUCCESS") {
				inSingularity(app: "dls") {
					checkClangFormat(folder: "no_change_repo")
				}
			}

			assertBuildResult("SUCCESS") {
				inSingularity(app: "dls") {
					checkClangFormat(folder: "no_change_repo", fullDiff: true)
				}
			}

			dir ("bad_repo") {
				jesh "git init"
				jesh "echo initial > initial && git add ."
				jesh "git commit -m='first'"
				jesh "echo 'void function() {' > file.h"
				jesh "echo '}' >> file"
				jesh "git add ."
				jesh "git commit -m='second'"
			}

			assertBuildResult("UNSTABLE") {
				inSingularity(app: "dls") {
					checkClangFormat(folder: "bad_repo")
				}
			}

			assertBuildResult("UNSTABLE") {
				inSingularity(app: "dls") {
					checkClangFormat(folder: "bad_repo", fullDiff: true)
				}
			}

			assertBuildResult("FAILURE") {
				checkClangFormat(folder: "good_repo")
			}

			assertBuildResult("FAILURE") {
				inSingularity(app: "dls") {
					checkClangFormat(nofolder: "good_repo")
				}
			}

			jesh "rm -rf good_repo bad_repo"
		}

		stage("wafDefaultPipelineTest") {
			// Test build a seldom altered project with minimal dependencies and a stable CI flow
			wafDefaultPipeline(projects: ["frickel-dls@v3testing"],
			                   container: [app: "visionary-dls"],
			                   notificationChannel: "#jenkins-trashbin")
			cleanWs()

			// Test a small project on multiple test resources
			wafDefaultPipeline(projects: ["hate"],
			                   container: [app: "visionary-dls"],
			                   testSlurmResource: [[partition: "jenkins"],
			                                       [partition: "compile"]],
			                   notificationChannel: "#jenkins-trashbin")
			cleanWs()

			// Unsupported command line options
			assertBuildResult("FAILURE") {
				// No pipeline without projects
				wafDefaultPipeline(notificationChannel: "#jenkins-trashbin")
			}
			assertBuildResult("FAILURE") {
				// 'projects' has to be of type List<String>
				wafDefaultPipeline(projects: "frickel-dls",
				                   notificationChannel: "#jenkins-trashbin")
			}
			assertBuildResult("FAILURE") {
				// container app has to be specified
				wafDefaultPipeline(projects: ["frickel-dls"],
				                   notificationChannel: "#jenkins-trashbin")
			}
			assertBuildResult("FAILURE") {
				// No pipeline without projects
				wafDefaultPipeline(projects: [],
				                   notificationChannel: "#jenkins-trashbin")
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

			// Test a small project without clang-format test
			wafDefaultPipeline(projects: ["hate"],
			                   container: [app: "visionary-dls"],
			                   testSlurmResource: [partition: "jenkins"],
			                   notificationChannel: "#jenkins-trashbin",
			                   enableClangFormat: false)
			cleanWs()
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

			// Test module load in container
			inSingularity {
				noModulePath = jesh(script: 'echo $PATH', returnStdout: true)

				withModules(modules: ["localdir"]) {
					localdirPath = jesh(script: 'echo $PATH', returnStdout: true)
				}
				assert (noModulePath != localdirPath): "$noModulePath should not be $localdirPath"
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
			notifyFailure(mattermostChannel: "jenkins-trashbin")

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
				// Too many tasks for a single node
				onSlurmResource(partition: "jenkins", ntasks: 32) {
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

			// Make sure the workspace fulfills the expected pattern
			runOnSlave(name: env.NODE_NAME) {
				assert (WORKSPACE ==~ /(?!.*__.+$)^\/jenkins\/jenlib_workspaces_f9\/.+$/):
						"Workspace '$WORKSPACE' not matching the expected pattern."
			}

			// Directory switching around runOnSlave has an effect
			String targetDir = UUID.randomUUID().toString()
			dir(targetDir) {
				runOnSlave(label: "frontend") {
					assert (pwd().contains(targetDir)): "Switching directories to ${targetDir} was not succesful."
				}
			}

			// Cannot use runOnSlave when in a generic workspace
			assertBuildResult("FAILURE") {
				node {
					ws(pwd()) {
						runOnSlave(label: "frontend") {
							jesh("hostname")
						}
					}
				}
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

			String moduleAndVersion = "testmodule"

			inSingularity() {
				moduleAndVersion = deployModule([name      : "testmodule",
				                                 moduleRoot: "$WORKSPACE/module",
				                                 targetRoot: "$WORKSPACE/install",
				                                 source    : "$WORKSPACE/source"])
			}

			withModules(modules: [moduleAndVersion], prependModulePath: "$WORKSPACE/module") {
				assert (jesh(returnStdout: true, script: "test_executable").contains("bla"))
			}

			withModules(modules: [moduleAndVersion],
			            prependModulePath: "$WORKSPACE/module") {
				assert (jesh(returnStdout: true,
				             script: "echo \$LD_LIBRARY_PATH").contains("$WORKSPACE/install/${moduleAndVersion}"))
			}

			withModules(modules: [moduleAndVersion],
			            prependModulePath: "$WORKSPACE/module") {
				inSingularity() {
					assert (jesh(returnStdout: true,
					             script: "echo \$LD_LIBRARY_PATH").contains("$WORKSPACE/install/${moduleAndVersion}"))
				}
			}

			// test increasing counter of module directory
			num_before = sish(returnStdout: true,
			                  script: "find `dirname $WORKSPACE/install/$moduleAndVersion`/* -maxdepth 0 -type d | wc -l").toInteger()
			inSingularity() {
				moduleAndVersion = deployModule([name      : "testmodule",
				                                 moduleRoot: "$WORKSPACE/module",
				                                 targetRoot: "$WORKSPACE/install",
				                                 source    : "$WORKSPACE/source"])
			}
			num_after = sish(returnStdout: true,
			                 script: "find `dirname $WORKSPACE/install/$moduleAndVersion`/* -maxdepth 0 -type d | wc -l").toInteger()
			assert (num_before == 1)
			assert (num_after == 2)

			// test custom version identifier
			inSingularity() {
				moduleAndVersion = deployModule([name      : "testmodule",
				                                 moduleRoot: "$WORKSPACE/module",
				                                 targetRoot: "$WORKSPACE/install",
				                                 source    : "$WORKSPACE/source",
				                                 version   : UUID.randomUUID().toString()],)
			}

			withModules(modules: [moduleAndVersion], prependModulePath: "$WORKSPACE/module") {
				assert (jesh(returnStdout: true, script: "test_executable").contains("bla"))
			}

			// test fail without being in inSingularity closure
			assertBuildResult("FAILURE") {
				deployModule([name      : "testmodule",
				              moduleRoot: "$WORKSPACE/module",
				              targetRoot: "$WORKSPACE/install",
				              source    : "$WORKSPACE/source"])
			}
			jesh "rm -rf $WORKSPACE/install $WORKSPACE/module $WORKSPACE/source"
		}
	}
} catch (Throwable t) {
	post_error_build_action()
	throw t
} finally {
	post_all_build_action()
}

// Some Jenkins steps fail a build without raising (e.g. archiveArtifacts)
if (currentBuild.currentResult != "SUCCESS") {
	post_error_build_action()
}


/*
/* HELPER FUNCTIONS
*/

void post_all_build_action() {
	// Always clean the workspace
	node(label: "frontend") {
		cleanWs()
	}
}

void post_error_build_action() {
	node(label: "frontend") {
		notifyFailure(mattermostChannel: "#softies")
	}
}
