import groovy.transform.Field
import org.jenkinsci.plugins.workflow.libs.LibraryStep

@Field LibraryStep.LoadedClasses jenlib = null

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

				jenlib = library "jenlib@${tmp_branch_name}"
			} catch (MissingPropertyException ignored) {
				jenlib = library 'jenlib'
			} finally {
				// Cleanup temporary branch
				dir("jenlib_tmp") {
					sh "git push --delete origin ${tmp_branch_name} || exit 0"
				}
			}
		}
	}

	/**
	 * Prevent 'java.lang.IllegalAccessException' due to broken class caches:
	 * Functions called on both,
	 *   * the dynamically loaded library ('jenlib')
	 *   * within global steps
	 * need to be called on the loaded library once before test execution.
	 */
	jenlib.org.electronicvisions.jenlib.ShellManipulator.fromEnvironment(this)

	// Reflection does not seem to play nicely with Jenkins CPS, this nevertheless needs beautification
	testConditionalStage()
	testSetBuildState()
	testAssertBuildResult()
	testGetCurrentParallelBranchIds()
	testShellManipulator()
	testCleanLocks()
	testConditionalTimeout()
	testCreateEnumeratedDirectory()
	testCreateDeploymentDirectory()
	testSetJobDescription()
	testJesh()
	testPipelineFromMarkdown()
	testIsWeekend()
	testIsAsicJenkins()
	testWithCcache()
	testIsTriggeredByGerrit()
	testIsTriggeredByUserAction()
	testAddBuildParameter()
	testRemoveAllBuildParameters()
	testCheckPatternInFile()
	testCheckPatternNotInFile()
	testDecodeBase64()
	testEncodeBase64()
	testInSingularity()
	testGetDefaultFixturePath()
	testGetDefaultContainerPath()
	testGetContainerApps()
	testGetHxTestResource()
	testDeployDocumentationRemote()
	testWithWaf()
	testWafSetup()
	testCheckClangFormat()
	testWafDefaultPipeline()
	testWithModules()
	testGetGerritUsername()
	testNotifyFailure()
	testOnSlurmResource()
	testRunOnSlave()
	testConfigureHxCubeBitfile()
	testFillTemplate()
	testDeployModule()

} catch (Throwable t) {
	notifyFailure(mattermostChannel: "#softies")
	node(label: "frontend") {
		cleanWs()
	}
	throw t
} finally {
	node(label: "frontend") {
		cleanWs()
	}
}

// Some Jenkins steps fail a build without raising (e.g. archiveArtifacts)
if (currentBuild.currentResult != "SUCCESS") {
	post_error_build_action()
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                 TEST METHODS                                                       //
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

void testConditionalStage() {
	stage('testConditionalStage') {
		conditionalStage(name: "NotExecuted", skip: true) {
			env.JenlibConditionalStageTest = "foobar"
		}
		assert env.JenlibConditionalStageTest == null, "Environment has been modified in skipped stage!"

		conditionalStage(name: "Executed", skip: false) {
			env.JenlibConditionalStageTest = "foobar"
		}
		assert env.JenlibConditionalStageTest == "foobar", "Environment has not been modified in non-skipped stage!"
	}
}

void testSetBuildState() {
	stage('testSetBuildState') {
		for (result in ["NOT_BUILT", "UNSTABLE", "SUCCESS", "FAILURE", "ABORTED"]) {
			assert (currentBuild.currentResult == "SUCCESS")
			setBuildState(state: result, reason: "testSetBuildState: set tested state")
			assert (currentBuild.currentResult == result)
			setBuildState(state: "SUCCESS", reason: "testSetBuildState: clear old state")
		}

		// 'state' and 'reason' arguments are mandatory
		assertBuildResult("FAILURE") {
			setBuildState(state: "SUCCESS")
		}
		assertBuildResult("FAILURE") {
			setBuildState(reason: "testSetBuildState")
		}
	}
}

void testAssertBuildResult() {
	stage('testAssertBuildResult') {
		assert (currentBuild.currentResult == "SUCCESS")

		// Check if all supported states work
		for (result in ["NOT_BUILT", "UNSTABLE", "SUCCESS", "FAILURE", "ABORTED"]) {
			assertBuildResult(result) {
				setBuildState(state: result, reason: "testAssertBuildResult: result assertion")
			}
		}
		assert (currentBuild.currentResult == "SUCCESS")

		// Expected exceptions should be catched
		assertBuildResult("FAILURE") {
			jesh "exit 1"
		}
		assert (currentBuild.currentResult == "SUCCESS")
	}
}

void testGetCurrentParallelBranchIds() {
	stage('testGetCurrentParallelBranchId') {
		assert (getCurrentParallelBranchIds().size() == 0): "Parallel branch ids found in sequential code!"

		List seenIds = Collections.synchronizedList([])
		Map parallelStages = [:]
		for (int i = 0; i < 10; i++) {
			parallelStages[i] = {
				List<String> parallelContextIds = getCurrentParallelBranchIds()
				assert (parallelContextIds.size() == 1): "Found too many/few branch ids: ${parallelContextIds}."
				String parallelContextId = parallelContextIds[0]
				assert (!seenIds.contains(parallelContextId)): "Already seen id '${parallelContextId}'!"
				seenIds.add(parallelContextId)

				stage("Parent in parallel context") {
					List<String> parallelParentIds = getCurrentParallelBranchIds()
					assert (parallelParentIds == parallelContextIds):
							"Branch ids changed between context ('${parallelContextIds}') " +
							"and parent ('${parallelParentIds}')!"

					stage("Sub-stage in parallel context") {
						List<String> parallelSubStageIds = getCurrentParallelBranchIds()
						assert (parallelSubStageIds == parallelParentIds):
								"Branch ids changed between context ('${parallelParentIds}') " +
								"and parent ('${parallelSubStageIds}')!"
					}

					Map innerStages = [:]
					for (int j = 0; j < 10; j++) {
						innerStages[j] = {
							List<String> nestedParallelIds = getCurrentParallelBranchIds()
							assert (nestedParallelIds.size() == 2): "Found too many/few branch ids: " +
							                                        "${nestedParallelIds}."
							String innerBranchId = nestedParallelIds[0]
							String outerBranchId = nestedParallelIds[1]
							assert (!seenIds.contains(innerBranchId)): "Already seen id '${innerBranchId}'!"
							seenIds.add(innerBranchId)
							assert (outerBranchId == parallelContextId): "Outer branch id has changed " +
							                                             "(was: '${parallelContextId}', " +
							                                             "is: '${outerBranchId}')."
						}
					}
					parallel(innerStages)
				}
			}
		}

		echo("Starting parallel stage tests in 'testGetCurrentParallelBranchId' (first run).")
		parallel(parallelStages)
		echo("Starting parallel stage tests in 'testGetCurrentParallelBranchId' (second run).")
		parallel(parallelStages)  // everything should be run-unique
	}
}

void testShellManipulator() {
	stage('testShellManipulator') {
		// Shell manipulations shall work in parallel branches
		String testEnvironmentVariable = "JENLIB_testShellManipulator"
		Map parallelStages = [:]
		for (int i = 0; i < 10; i++) {
			int localIterator = i
			parallelStages[i] = {
				manipulator = jenlib.org.electronicvisions.jenlib.ShellManipulator.fromEnvironment(this)
				manipulator.add("${testEnvironmentVariable}=${localIterator} ", "")
				String result = jesh(script: "echo \${${testEnvironmentVariable}}", returnStdout: true).trim()
				assert (localIterator == result.toInteger()): "Wrong shell manipulation active: Found " +
				                                              "'${result.toInteger()}', expected: '${localIterator}'."
				manipulator.restore()
			}
		}

		runOnSlave(label: "frontend") {
			parallel(parallelStages)
		}
	}
}

void testCleanLocks() {
	stage("testCleanLocks") {
		jenlibCleanLocks()
	}
}

void testConditionalTimeout() {
	stage('testConditionalTimeout') {
		Map<String, Object> timeoutOptions = [enable: false, time: 5, unit: "SECONDS"]
		Map<String, Object> oldTimeoutOptions = timeoutOptions.clone()

		// timeout still works
		conditionalTimeout(timeoutOptions) {
			sleep(time: 10, unit: "SECONDS")
		}

		// Map of options unchanged
		assert (timeoutOptions == oldTimeoutOptions): "Map of timeout options has changed!"

		// timeout can be disabled
		assertBuildResult("FAILURE") {
			conditionalTimeout(enable: true, time: 5, unit: "SECONDS") {
				sleep(time: 10, unit: "SECONDS")
			}
		}

		// 'enable' is mandatory
		assertBuildResult("FAILURE") {
			conditionalTimeout(time: 5, unit: "SECONDS") {}
		}
	}
}

void testCreateEnumeratedDirectory() {
	stage('testCreateEnumeratedDirectory') {
		String baseDirectory = UUID.randomUUID().toString()
		String incrementedDirectory = UUID.randomUUID().toString()

		runOnSlave(label: "frontend") {
			String firstResult = createEnumeratedDirectory("${WORKSPACE}/${baseDirectory}/${incrementedDirectory}")
			String secondResult = createEnumeratedDirectory("${WORKSPACE}/${baseDirectory}/${incrementedDirectory}")

			[firstResult, secondResult].eachWithIndex { String result, int index ->
				assert (jesh(script: "basename '${result}'", returnStdout: true).contains(incrementedDirectory)):
						"'${result}' does not contain '${incrementedDirectory}' in its basename."
				assert (jesh(script: "basename \"`dirname \\\"${result}\\\"`\"",
				             returnStdout: true).trim() == baseDirectory):
						"'${result}' is not a subfolder of '${baseDirectory}'."
				assert (jesh(script: "[ -d '${result}' ]", returnStatus: true) == 0): "'${result}' is not a directory."
				assert (result.endsWith("${baseDirectory}/${incrementedDirectory}_${index + 1}")):
						"'${result}' Does not match the expected pattern."
			}
		}
	}
}

void testCreateDeploymentDirectory() {
	stage('testCreateDeploymentDirectory') {
		String deploymentRoot = UUID.randomUUID().toString()

		runOnSlave(label: "frontend") {
			final String result = createDeploymentDirectory("${WORKSPACE}/${deploymentRoot}")
			assert result.contains(deploymentRoot): "'${result}' does not contain '${deploymentRoot}'."
			assert (jesh(script: "[ -d '${result}' ]", returnStatus: true) == 0): "'${result}' is not a directory."

			final String date = jesh(returnStdout: true, script: "date --utc --iso").trim()
			if (isTriggeredByGerrit() && env.GERRIT_EVENT_TYPE != "change-merged") {
				assert result.contains("testing")
				assert result.contains(date), "Current date not in deployed folder. " +
				                              "This test is known to be racy on date changes. Check test time!"
				assert result.contains((String) env.GERRIT_CHANGE_NUMBER)
				assert result.contains((String) env.GERRIT_PATCHSET_NUMBER)
			} else {
				assert result.contains("stable")
				assert result.contains(date)
			}
		}
	}
}

void testSetJobDescription() {
	stage('testSetJobDescription') {
		String tmpDescription = getJobDescription()
		String testDescription = UUID.randomUUID().toString()

		setJobDescription(testDescription)
		assert (getJobDescription() == testDescription)

		setJobDescription(tmpDescription)
		assert (getJobDescription() == tmpDescription)
	}
}

void testJesh() {
	stage('testJesh') {
		runOnSlave(label: "frontend") {
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
	}
}

void testPipelineFromMarkdown() {
	stage("testPipelineFromMarkdown") {
		String tempFilePath = ""
		runOnSlave(label: "frontend") {
			tempFilePath = "${WORKSPACE}/${UUID.randomUUID().toString()}"
			writeFile(file: tempFilePath,
			          text: libraryResource("org/electronicvisions/MarkdownScriptExtractorTest.md"))
		}

		pipelineFromMarkdown(markdownFilePath: tempFilePath, blockType: "shell")
		pipelineFromMarkdown(markdownFilePath: tempFilePath, blockType: "shell", concatenateBlocks: false)

		for (failingBlock in ["fail-0", "fail-1", "fail-2"]) {
			assertBuildResult("FAILURE") {
				pipelineFromMarkdown(markdownFilePath: tempFilePath, blockType: failingBlock)
			}
		}
	}
}

void testIsWeekend() {
	stage('testIsWeekend') {
		boolean bashIsWeekend = null
		runOnSlave(label: "frontend") {
			bashIsWeekend = jesh(script: "[[ \$(date +%u) -lt 6 ]]", returnStatus: true)
		}
		boolean jenlibIsWeekend = isWeekend()
		assert (jenlibIsWeekend == bashIsWeekend): "Bash says weekend: ${bashIsWeekend}, " +
		                                           "jenlib: ${jenlibIsWeekend}"
	}
}

void testIsAsicJenkins() {
	stage('testIsAsicJenkins') {
		// This file can only run on F9 jenkins
		assert isAsicJenkins() == nodesByLabel("frontend").contains("ome"):
				"Wrong Jenkins instance detected! Frontend nodes: ${nodesByLabel("frontend")}."
	}
}

void testWithCcache() {
	stage('testWithCcache') {
		withCcache() {
			inSingularity(app: "visionary-wafer") {
				runOnSlave(label: "frontend && singularity") {
					jesh(script: "ln -s \$(which gcc) ccache")
					ccacheVersion = jesh(script: "./ccache --version | head -n1", returnStdout: true)
					jesh(script: "rm -f ccache")
					assert (ccacheVersion.contains("ccache")): "$ccacheVersion does not contain 'ccache'"
				}
			}
		}

		// Fail if ccacheNoHashDir is not boolean
		assertBuildResult("FAILURE") {
			withCcache(ccacheNoHashDir: "no") {}
		}
	}
}

void testIsTriggeredByGerrit() {
	stage("testIsTriggeredByGerrit") {
		// We assume that this pipeline is never triggered from an upstream job, otherwise this test will fail!
		assert (isTriggeredByGerrit() == (env.GERRIT_CHANGE_NUMBER ? true : false))
	}
}

void testIsTriggeredByUserAction() {
	stage("testIsTriggeredByUserAction") {
		// We assume that this pipeline is never triggered manually, otherwise this test will fail!
		assert (isTriggeredByUserAction() == false):
				"Manual trigger detected, disable 'isTriggeredByUserActionTest' when running this pipeline manually!"
	}
}

void testAddBuildParameter() {
	stage("testAddBuildParameter") {
		String parameterName = UUID.randomUUID().toString()
		String parameterValue = UUID.randomUUID().toString()
		assert (params.get(parameterName) == null)

		// Check parameters can be added
		addBuildParameter(string(name: parameterName, defaultValue: parameterValue))
		assert (params.get(parameterName) == parameterValue): "Build parameter was not added."

		// Check parameters can be added without touching the default
		String otherParameterValue = UUID.randomUUID().toString()
		addBuildParameter(string(name: parameterName, defaultValue: otherParameterValue), false)
		assert (params.get(parameterName) == parameterValue): "Default value was overwritten."

		// Cleanup: Remove all build parameters: This pipeline is not supposed to have any
		removeAllBuildParameters()
	}
}

void testRemoveAllBuildParameters() {
	stage("testRemoveAllBuildParameters") {
		// Add some parameter
		addBuildParameter(string(name: "foo", defaultValue: "bar"))
		assert (params.foo == "bar"): "Could not add build parameter."

		// Remove all parameters, this pipeline is not supposed to have any
		removeAllBuildParameters()
		assert (params.foo == null): "Build parameter survived removal."

		// Make sure removal works if none were present
		removeAllBuildParameters()
	}
}

void testCheckPatternInFile() {
	stage('testCheckPatternInFile') {
		runOnSlave(label: "frontend") {
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
	}
}

void testCheckPatternNotInFile() {
	stage('testCheckPatternNotInFile') {
		runOnSlave(label: "frontend") {
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
	}
}

void testDecodeBase64() {
	stage('testDecodeBase64') {
		assert (decodeBase64("Zm9vYmFy") == "foobar")
	}
}

void testEncodeBase64() {
	stage('testEncodeBase64') {
		assert (encodeBase64("barfoo") == "YmFyZm9v")
	}
}

void testInSingularity() {
	stage('testInSingularity') {
		// No node needed for block declaration
		inSingularity {
			runOnSlave(label: "frontend && singularity") {
				// jesh-shell steps are executed in containers
				String containerEnv = jesh(script: "env", returnStdout: true)
				assert (containerEnv.contains("SINGULARITY_CONTAINER"))
			}
		}

		runOnSlave(label: "frontend && singularity") {
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

			// Nested singularity calls, only available for the F9 installation
			if (!isAsicJenkins()) {
				// App shall not be propagated
				inSingularity(app: "dls") {
					inSingularity() {
						jesh("env | grep -E '(APPTAINER|SINGULARITY)_APPNAME=\$'")
					}
				}

				// Environment modifications are passed to the nested container
				withEnv(["SINGULARITYENV_PREPEND_PATH=foobar"]) {
					inSingularity(app: "dls") {
						inSingularity() {
							jesh("env | grep '^PATH=foobar'")
						}
					}
				}
			}
		}
	}
}

void testGetDefaultFixturePath() {
	stage("testGetDefaultFixturePath") {
		String canonicalDefault = "/lib"
		String commitKey = "SomeKey"

		// Default without key commit message
		withEnv(["GERRIT_CHANGE_COMMIT_MESSAGE=${encodeBase64('')}"]) {
			assert getDefaultFixturePath(defaultPathCanonical: canonicalDefault,
			                             commitKey: commitKey,
			                             parameterName: "UNUSED_NO_KEY") == canonicalDefault
		}

		// Result does not change within the same build
		withEnv(["GERRIT_CHANGE_COMMIT_MESSAGE=${encodeBase64(commitKey + ': /usr/lib')}"]) {
			assert getDefaultFixturePath(defaultPathCanonical: canonicalDefault,
			                             commitKey: commitKey,
			                             parameterName: "UNUSED_NO_KEY") == canonicalDefault
		}

		// Special key specified in commit message
		String pathCustomImage = "/bin"

		// NOTE; additional white-space in the commit-messages is intended
		List<String> commitMessagesSuccess = [
"""
Fake commit message subject

Here be dragons!

${commitKey}: ${pathCustomImage}

Change-Id: 12345678
""",

"""
Fake commit message subject

Here be dragons!

${commitKey}:${pathCustomImage}     

Change-Id: 12345678
""",

"""
Fake commit message subject

Change-Id: 12345678
${commitKey}:       ${pathCustomImage}     
"""
		]

		commitMessagesSuccess.eachWithIndex { fakeCommitMessage, i ->
			String encodedFakeCommitMessage = encodeBase64(fakeCommitMessage)

			withEnv(["GERRIT_CHANGE_COMMIT_MESSAGE=$encodedFakeCommitMessage"]) {
				// Generate new options per message to disable caching
				String containerPath = getDefaultFixturePath(defaultPathCanonical: canonicalDefault,
				                                             commitKey: commitKey,
				                                             parameterName: "UNUSED_${i}")
				assert containerPath == pathCustomImage:
						"Expected $pathCustomImage, but container path is $containerPath."
			}
		}

		List<String> commitMessagesFail = [
"""
Fake commit message subject

Here be dragons! And multiple ${commitKey} statements!

${commitKey}:${pathCustomImage}     
${commitKey}: ${pathCustomImage}

Change-Id: 12345678
""",
		]

		commitMessagesFail.eachWithIndex { fakeCommitMessage, i ->
			String encodedFakeCommitMessage = encodeBase64(fakeCommitMessage)

			assertBuildResult("FAILURE") {
				withEnv(['GERRIT_CHANGE_COMMIT_MESSAGE=' + encodedFakeCommitMessage]) {
					getDefaultFixturePath(defaultPathCanonical: "/lib",
					                      commitKey: commitKey,
					                      parameterName: "UNUSED_${i + commitMessagesSuccess.size()}")
				}
			}
		}
	}
}

void testGetDefaultContainerPath() {
	stage("testGetDefaultContainerPath") {
		String defaultPathExpected = null
		runOnSlave(label: "frontend") {
			defaultPathExpected = jesh(script: "readlink -f /containers/stable/latest",
			                           returnStdout: true).trim()
		}

		// Default path is as expected
		withEnv(["GERRIT_CHANGE_COMMIT_MESSAGE=${encodeBase64('')}"]) {
			String defaultPathResult = getDefaultContainerPath()
			assert defaultPathResult == defaultPathExpected, "${defaultPathResult} != ${defaultPathExpected}"
		}
	}
}

void testGetContainerApps() {
	stage("testGetContainerApps") {
		runOnSlave(label: "frontend && singularity") {
			assert getContainerApps().contains("visionary-dls")
			assert getContainerApps(getDefaultContainerPath()).contains("visionary-dls")
		}
	}
}

void testGetHxTestResource() {
	conditionalStage(name: "testGetHxTestResource", skip: isAsicJenkins()) {
		assertBuildResult("FAILURE") {
			getHxTestResource(-100)  // invalid chip revision
		}

		onSlurmResource(getHxTestResource(3)) {
			assert (env.SLURM_FPGA_IPS != null) : "SLURM_FPGA_IPS not defined on what is expected to be a hardware allocation!"
		}
	}
}

void testDeployDocumentationRemote() {
	stage("testDeployDocumentationRemote") {
		runOnSlave(label: "frontend") {
			String groupId = isAsicJenkins() ? "s5" : "f9"
			repositoryUrl = "ssh://hudson@brainscales-r.kip.uni-heidelberg.de:29418/jenlib"
			upstreamBranch = "sandbox/hudson/deploy_documentation_test_${groupId}"
			jesh "mkdir upstream"
			dir("upstream") {
				jesh "git init"
				jesh "git commit --allow-empty -m 'Documentation'"
				jesh "git push -f ${repositoryUrl} HEAD:${upstreamBranch}"
			}
			jesh "mkdir docu"
			content = jesh(script: "echo -n 'my docu ' \$(date)", returnStdout: true)
			jesh "echo -n '${content}' > docu/docu.txt"
			deployDocumentationRemote([folders       : ["docu"],
			                           repositoryUrl : repositoryUrl,
			                           upstreamBranch: upstreamBranch])
			jesh "git clone --branch ${upstreamBranch} ${repositoryUrl}"
			assert fileExists("jenlib/docu/docu.txt")
			String docu_content = jesh(script: "cat jenlib/docu/docu.txt", returnStdout: true)
			assert docu_content.contains(content)
			jesh "rm -rf docu jenlib upstream"
		}
	}
}

void testWithWaf() {
	stage('testWithWaf') {
		List<String> requiredModules = []
		if (isAsicJenkins()) {
			requiredModules.add("python")
			requiredModules.add("git")
		}

		withModules(modules: requiredModules) {
			withWaf() {
				runOnSlave(label: "frontend") {
					stdout = jesh(returnStdout: true, script: "waf --help")
					assert (stdout.contains("waf [commands] [options]"))
				}

				// nested withWaf
				withWaf() {
					runOnSlave(label: "frontend") {
						stdout = jesh(returnStdout: true, script: "waf --help")
						assert (stdout.contains("waf [commands] [options]"))
					}
				}

				inSingularity {
					runOnSlave(label: "frontend && singularity") {
						stdout_singularity = jesh(returnStdout: true, script: "waf --help")
					}
				}
				assert (stdout_singularity.contains("waf [commands] [options]"))
			}

			// multiple parallel branches
			Map parallelStages = [:]
			for (int i = 0; i < 3; i++) {
				final int localIterator = i
				parallelStages[localIterator] = {
					sleep(localIterator * 10) // build sequentially
					withWaf {
						runOnSlave(label: "frontend") {
							stdout = jesh(returnStdout: true, script: "waf --help")
							assert (stdout.contains("waf [commands] [options]"))
						}
						sleep(30) // stay in withWaf during the other builds
					}
				}
			}
			parallel(parallelStages)
		}
	}
}

void testWafSetup() {
	stage("testWafSetup") {
		List<String> requiredModules = []
		if (isAsicJenkins()) {
			requiredModules.add("python")
			requiredModules.add("git")
		}

		withModules(modules: requiredModules) {
			wafSetup(projects: ["jenlib-minimalwaftest"])

			// Multiple projects
			wafSetup(projects: ["jenlib-minimalwaftest", "code-format"])

			// Setup in subfolder
			runOnSlave(label: "frontend") {
				String subfolder = UUID.randomUUID().toString()
				dir(subfolder) {
					wafSetup(projects: ["jenlib-minimalwaftest"])
				}
				assert fileExists("${subfolder}/wscript")
			}

			// Unsupported command line options
			assertBuildResult("FAILURE") {
				wafSetup()
			}
			assertBuildResult("FAILURE") {
				wafSetup(projects: "jenlib-minimalwaftest")
			}
		}
	}
}

void testCheckClangFormat() {
	stage('testCheckClangFormat') {
		runOnSlave(label: "frontend && singularity") {
			dir("good_repo") {
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

			dir("no_change_repo") {
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

			dir("bad_repo") {
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
	}
}

void testWafDefaultPipeline() {

	conditionalStage(name: "testWafDefaultPipeline", skip: isAsicJenkins()) {
		wafDefaultPipeline(projects: ["jenlib-minimalwaftest"],
		                   container: [app: "visionary-dls"],
		                   notificationChannel: "#jenkins-trashbin")
		runOnSlave(label: "frontend") {
			cleanWs()
		}

		// Test on multiple test resources
		wafDefaultPipeline(projects: ["jenlib-minimalwaftest"],
		                   container: [app: "visionary-dls"],
		                   testSlurmResource: [[partition: "batch"],
		                                       [partition: "interactive"]],
		                   notificationChannel: "#jenkins-trashbin")
		runOnSlave(label: "frontend") {
			cleanWs()
		}

		// Test injection of pre/post test hooks does not fail
		wafDefaultPipeline(projects: ["jenlib-minimalwaftest"],
		                   container: [app: "visionary-dls"],
		                   notificationChannel: "#jenkins-trashbin",
		                   preTestHook: { jesh("hostname") },
		                   postTestHook: { jesh("hostname") })
		runOnSlave(label: "frontend") {
			cleanWs()
		}

		// Test injection of test hooks has an effect: if 'build' is deleted, reconfiguration is necessary.
		assertBuildResult("FAILURE") {
			wafDefaultPipeline(projects: ["jenlib-minimalwaftest"],
			                   container: [app: "visionary-dls"],
			                   notificationChannel: "#jenkins-trashbin",
			                   preTestHook: { jesh("rm -rf build/") })
		}
		runOnSlave(label: "frontend") {
			cleanWs()
		}

		// Only injecting a post hook does not fail
		wafDefaultPipeline(projects: ["jenlib-minimalwaftest"],
		                   container: [app: "visionary-dls"],
		                   notificationChannel: "#jenkins-trashbin",
		                   postTestHook: { jesh("hostname") })
		runOnSlave(label: "frontend") {
			cleanWs()
		}

		// Value passing between test hooks works
		wafDefaultPipeline(projects: ["jenlib-minimalwaftest"],
		                   container: [app: "visionary-dls"],
		                   notificationChannel: "#jenkins-trashbin",
		                   preTestHook: { return jesh(script: "hostname", returnStdout: true) },
		                   postTestHook: { String preHookHostname ->
			                   assert preHookHostname == jesh(script: "hostname", returnStdout: true),
					                   "preTestHook hostname ('${preHookHostname}') did not match current hostname!"
		                   }
		)
		runOnSlave(label: "frontend") {
			cleanWs()
		}

		// Unsupported command line options
		assertBuildResult("FAILURE") {
			// No pipeline without projects
			wafDefaultPipeline(notificationChannel: "#jenkins-trashbin")
		}
		assertBuildResult("FAILURE") {
			// 'projects' has to be of type List<String>
			wafDefaultPipeline(projects: "jenlib-minimalwaftest",
			                   notificationChannel: "#jenkins-trashbin")
		}
		assertBuildResult("FAILURE") {
			// No pipeline without projects
			wafDefaultPipeline(projects: [],
			                   notificationChannel: "#jenkins-trashbin")
		}
		assertBuildResult("FAILURE") {
			// Target may not be modified, the pipeline runs for default and '*' internally
			wafDefaultPipeline(projects: ["jenlib-minimalwaftest"],
			                   container: [app: "visionary-dls"],
			                   notificationChannel: "#jenkins-trashbin",
			                   configureInstallOptions: "--target='*'")
		}
		assertBuildResult("FAILURE") {
			// Target may not be modified, the pipeline runs for default and '*' internally
			wafDefaultPipeline(projects: ["jenlib-minimalwaftest"],
			                   container: [app: "visionary-dls"],
			                   notificationChannel: "#jenkins-trashbin",
			                   testOptions: "--target='*'")
		}
		assertBuildResult("FAILURE") {
			// Test handling may not be modified, the pipeline does it internally
			wafDefaultPipeline(projects: ["jenlib-minimalwaftest"],
			                   container: [app: "visionary-dls"],
			                   notificationChannel: "#jenkins-trashbin",
			                   configureInstallOptions: "--test-execnone")
		}

		// Test project without clang-format test
		wafDefaultPipeline(projects: ["jenlib-minimalwaftest"],
		                   container: [app: "visionary-dls"],
		                   testSlurmResource: [partition: "batch"],
		                   notificationChannel: "#jenkins-trashbin",
		                   enableClangFormat: false)
		runOnSlave(label: "frontend") {
			cleanWs()
		}

		// Test project with clang-tidy test
		wafDefaultPipeline(projects: ["jenlib-minimalwaftest"],
		                   container: [app: "visionary-dls"],
		                   testSlurmResource: [partition: "batch"],
		                   notificationChannel: "#jenkins-trashbin",
		                   enableClangTidy: true)
		runOnSlave(label: "frontend") {
			cleanWs()
		}

		// Test project with cppcheck test
		wafDefaultPipeline(projects: ["jenlib-minimalwaftest"],
		                   container: [app: "visionary-dls"],
		                   testSlurmResource: [partition: "batch"],
		                   notificationChannel: "#jenkins-trashbin",
		                   enableCppcheck: true)
		runOnSlave(label: "frontend") {
			cleanWs()
		}
	}
}

void testWithModules() {
	stage("testWithModules") {
		// Module available on F9 as well as S5 nodes
		String alwaysAvailableModule = "xilinx"

		runOnSlave(label: "frontend") {
			noModulePath = jesh(script: 'echo $PATH', returnStdout: true)
		}

		withModules(modules: [alwaysAvailableModule]) {
			runOnSlave(label: "frontend") {
				withModulePath = jesh(script: 'echo $PATH', returnStdout: true)
			}
		}
		assert (noModulePath != withModulePath): "$noModulePath should not be $withModulePath"

		withModules(modules: [alwaysAvailableModule]) {
			withModules(purge: true, modules: []) {
				runOnSlave(label: "frontend") {
					purgedModulePath = jesh(script: 'echo $PATH', returnStdout: true)
				}
			}
		}
		assert (noModulePath == purgedModulePath): "$noModulePath should be $purgedModulePath"

		withModules(modules: [], prependModulePath: "foo/bar") {
			runOnSlave(label: "frontend") {
				assert (jesh(script: "echo \$MODULEPATH", returnStdout: true).contains("foo/bar"))
			}
		}

		// Test module load in container
		inSingularity {
			runOnSlave(label: "frontend && singularity") {
				noModulePath = jesh(script: 'echo $PATH', returnStdout: true)
			}

			withModules(modules: [alwaysAvailableModule]) {
				runOnSlave(label: "frontend && singularity") {
					withModulePath = jesh(script: 'echo $PATH', returnStdout: true)
				}
			}
			assert (noModulePath != withModulePath): "$noModulePath should not be $withModulePath"
		}

		// Fail if module load does not succeed
		// Failure needs to happen upon the first jesh call, since the final environment is not known before.
		withModules(modules: ["jenlibNonExistingModule"]) {
			runOnSlave(label: "frontend") {
				assertBuildResult("FAILURE") {
					jesh("exit 0")
				}
			}
		}

		// Fail early on bad input
		assertBuildResult("FAILURE") {
			// 'modules' has to be of type List<String>
			withModules(modules: alwaysAvailableModule) {}
		}
		assertBuildResult("FAILURE") {
			// 'purge' has to be of type boolean
			withModules(purge: alwaysAvailableModule) {}
		}
		assertBuildResult("FAILURE") {
			// 'moduleInitPath' has to be of type String
			withModules(moduleInitPath: true) {}
		}
	}
}

void testGetGerritUsername() {
	stage("testGetGerritUsername") {
		runOnSlave(label: "frontend") {
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
	}
}

void testNotifyFailure() {
	stage("testNotifyFailure") {
		notifyFailure(mattermostChannel: "jenkins-trashbin")

		// mattermostChannel is mandatory
		assertBuildResult("FAILURE") {
			notifyFailure()
		}
	}
}

void testOnSlurmResource() {
	conditionalStage(name: "testOnSlurmResource", skip: isAsicJenkins()) {
		onSlurmResource(partition: "batch") {
			assert (env.NODE_LABELS.contains("swarm"))
		}

		// PWD stays the same
		runOnSlave(label: "frontend") {
			frontendPwd = pwd()
			onSlurmResource(partition: "batch") {
				slavePwd = pwd()
				assert (slavePwd == frontendPwd): "slavePwd: $slavePwd, frontendPwd: $frontendPwd"
			}
		}

		// Workspace stays the same
		runOnSlave(label: "frontend") {
			frontendWs = WORKSPACE
			onSlurmResource(partition: "batch") {
				slaveWs = WORKSPACE
				assert (slaveWs == frontendWs): "slaveWs: $slaveWs, frontendWs: $frontendWs"
			}
		}
	}
}

void testRunOnSlave() {
	stage("testRunOnSlave") {
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

		runOnSlave(label: "frontend && singularity") {

			// Make sure we stay on the same executor (same name given)
			pipeline_executor = env.EXECUTOR_NUMBER
			runOnSlave(name: env.NODE_NAME) {
				assert (env.EXECUTOR_NUMBER == pipeline_executor)
			}

			// Make sure we stay on the same executor (similar label given)
			runOnSlave(label: "frontend || singularity") {
				assert (env.EXECUTOR_NUMBER == pipeline_executor)
			}

			// Make sure the workspace fulfills the expected pattern
			runOnSlave(name: env.NODE_NAME) {
				String groupId = isAsicJenkins() ? "s5" : "f9"
				assert (WORKSPACE ==~ /(?!.*__.+$)^\/jenkins\/jenlib_workspaces_${groupId}\/.+$/):
						"Workspace '$WORKSPACE' not matching the expected pattern."
			}

			// Directory switching around runOnSlave has an effect
			String targetDir = UUID.randomUUID().toString()
			dir(targetDir) {
				runOnSlave(label: "frontend") {
					assert (pwd().contains(targetDir)): "Switching directories to ${targetDir} was not succesful."
				}
			}

			/**
			  * The following test does not throw anymore, issue #4012
			  *
			  * // Cannot use runOnSlave when in a generic workspace
			  * assertBuildResult("FAILURE") {
			  * 	node {
			  * 		ws(pwd()) {
			  * 			runOnSlave(label: "frontend") {
			  * 				jesh("hostname")
			  * 			}
			  * 		}
			  * 	}
			  * }
			  */
		}
	}
}

void testConfigureHxCubeBitfile() {
	conditionalStage(name: "testConfigureHxCubeBitfile", skip: isAsicJenkins()) {
		onSlurmResource(partition: "batch", wafer: 62, "fpga-without-aout": 3) {
			configureHxCubeBitfile()
		}

		// Should not run without hardware allocation
		assertBuildResult("FAILURE") {
			runOnSlave(label: "frontend") {
				configureHxCubeBitfile()
			}
		}
	}
}

void testFillTemplate() {
	stage("testFillTemplate") {
		template = 'Hello <% out.print firstname %> ${lastname}'
		result = fillTemplate(template, [firstname: "Jenkins", lastname: "Hudson"])
		assert (result == 'Hello Jenkins Hudson'): result
	}
}

void testDeployModule() {
	stage('testDeployModule') {
		runOnSlave(label: "frontend && singularity") {
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
				                                 source    : "$WORKSPACE/source/*"])
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
				                                 source    : "$WORKSPACE/source/*"])
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
				                                 source    : "$WORKSPACE/source/*",
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
				              source    : "$WORKSPACE/source/*"])
			}

			// test setAsDefault
			inSingularity() {
				moduleName = "testmodule_${UUID.randomUUID().toString()}"
				firstModuleAndVersion = deployModule([name        : moduleName,
				                                      moduleRoot  : "$WORKSPACE/module",
				                                      targetRoot  : "$WORKSPACE/install",
				                                      source      : "$WORKSPACE/source/*",
				                                      setAsDefault: true])

				// this should not change the default-loaded module
				deployModule([name        : moduleName,
				              moduleRoot  : "$WORKSPACE/module",
				              targetRoot  : "$WORKSPACE/install",
				              source      : "$WORKSPACE/source/*",
				              setAsDefault: false])

				withModules(modules: [moduleName],
						    prependModulePath: "$WORKSPACE/module") {
					assert (jesh(returnStdout: true,
							script: "echo \$PATH").contains("$WORKSPACE/install/${firstModuleAndVersion}"))
				}
			}

			jesh "rm -rf $WORKSPACE/install $WORKSPACE/module $WORKSPACE/source"
		}
	}
}
