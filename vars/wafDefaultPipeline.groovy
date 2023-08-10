/**
 * Pipeline for verifying "typical" waf projects:
 * <ul>
 *     <li>Cleanup, setup, build of the given projects
 *     <li>Run tests
 *     <li>Evaluate test results
 * </ul>
 *
 * The project is built and tested for the default target definition as well as <code>--target='*'</code>.
 *
 * @param options Map of options for the pipeline:
 *                <ul>
 *                    <li><b>projects</b> (mandatory): see <code>wafSetup</code>
 *                    <li><b>setupOptions</b> (optional): see <code>wafSetup</code>
 *                    <li><b>moduleOptions</b> (optional): Map of options to be passed to <code>withModules</code>.
 *                    <li><b>container</b> (mandatory): Map of options to be passed to <code>inSingularity</code>.
 *                                                      <code>app</code> key is mandatory.
 *                    <li><b>notificationChannel</b> (mandatory): Channel to be notified in case of failure
 *                                                                (e.g. <code>#softies</code>)
 *                    <li><b>configureInstallOptions</b> (optional): Options passed to the
 *                                                        <code>waf configure install</code> call.
 *                                                        Defaults to <code>""</code>, <code>--test-execnone</code> is always set.
 *                    <li><b>testSlurmResource</b> (optional): List of Slurm resource definitions tests
 *                                                             are run on. If multiple resources are given,
 *                                                             tests are executed once on all given
 *                                                             resources.
 *                                                             Arguments that are not of type list are
 *                                                             transformed into a list of length 1.
 *                                                             Defaults to <code>[partition: "jenkins", "cpus-per-task": "8"]</code>
 *                    <li><b>testOptions</b> (optional): Options passed to the test execution waf call.
 *                                                       Defaults to <code>"--test-execall"</code>
 *                    <li><b>testTimeout</b> (optional): Timeout of waf test execution call.
 *                    <li><b>warningsIgnorePattern</b> (optional): Compiler warnings to be ignored.
 *                    <li><b>wafTargetOptions</b> (optional): List of targets to be built.
 *                                                            Defaults to <code>[""]</code>, representing only the default target set.
 *                    <li><b>enableClangFormat</b> (optional): Enable clang-format checks.
                                                               Defaults to <code>true</code>.
 *                    <li><b>enableClangFormatFullDiff</b> (optional): Enable clang-format to check on the complete project instead
                                                               of the difference of the last commit.
                                                               Defaults to <code>false</code>.
 *                    <li><b>enableCppcheck</b> (optional): Enable cppcheck checks. This needs `bear` to be available.
                                                               Defaults to <code>true</code>.
 *                    <li><b>enableCppcheckVote</b> (optional): Enable cppcheck voting unstable if warnings/errors are found.
                                                               Defaults to <code>false</code>.
 *                    <li><b>enableClangTidy</b> (optional): Enable clang-tidy checks. This needs `bear` to be available.
                                                               Defaults to <code>true</code>.
 *                    <li><b>enableClangTidyVote</b> (optional): Enable clang-tidy voting unstable if warnings/errors are found.
                                                               Defaults to <code>false</code>.
 *                    <li><b>preTestHook</b> (optional): Closure to be run on each test allocation prior to running the tests.
 *                                                       Values returned by this Hook are passed as a single argument to postTestHook.
 *                    <li><b>postTestHook</b> (optional): Closure to be run on each test allocation after running the tests.
 *                                                        Receives the value returned by preTestHook as its only parameter.
 *                </ul>
 */
def call(Map<String, Object> options = [:]) {
	timestamps {

		if (options.containsKey("prePipelineCleanup") | options.containsKey("prePipelineCleanup")) {
			echo "[WARNING] Pipeline cleanup is deprecated! Builds use unique workspaces."
		}

		/*
		 * Default failure notification channel: This is only used when a non-gerrit triggered (nightly)
		 * pipeline fails without setting the (mandatory) {@code notificationChannel} argument.
		 */
		String notificationChannel = "#softies"

		try {
			if (options.get("notificationChannel") == null) {
				throw new IllegalArgumentException("Notification channel is a mandatory argument.")
			}
			notificationChannel = options.get("notificationChannel")

			if (options.get("configureInstallOptions")?.contains("--target")) {
				throw new IllegalArgumentException("Cannot overwrite target definition.")
			}

			if (options.get("configureInstallOptions")?.contains("test")) {
				throw new IllegalArgumentException("Cannot overwrite test definition.")
			}

			if (options.get("testOptions")?.contains("--target")) {
				throw new IllegalArgumentException("Cannot overwrite target definition.")
			}

			LinkedHashMap<String, String> containerOptions
			if (options.get("app") != null) {
				echo "[WARNING] 'app' pipeline parameter is deprecated. Use 'container: [app: ]' instead.'"
				containerOptions = [app: (String) options.get("app")]
			} else {
				containerOptions = (LinkedHashMap<String, String>) options.get("container")
			}

			Map<String, Object> moduleOptions = (Map<String, Object>) options.get("moduleOptions", [modules: []])
			String testTimeout = ""
			if (options.get("testTimeout") != null) {
				echo("WARNING: The 'testTimeout' option in 'wafDefaultPipeline' is deprecated! " +
				     "Timeouts should be annotated in the respective wscript.")
				testTimeout = "--test-timeout=" + (int) options.get("testTimeout")
			}
			String configureInstallOptions = options.get("configureInstallOptions", "")

			List<Map<String, String>> testResources
			if (options.get("testSlurmResource") instanceof Map) {
				testResources = [options.get("testSlurmResource")] as List<Map<String, String>>
			} else if (options.get("testSlurmResource") instanceof List) {
				testResources = options.get("testSlurmResource") as List<Map<String, String>>
			} else if (options.get("testSlurmResource") == null) {
				testResources = [[partition: "jenkins", "cpus-per-task": "8"]]
			} else {
				throw new IllegalArgumentException("testSlurmResource argument is malformed.")
			}

			String testOptions = options.get("testOptions", "--test-execall")
			String warningsIgnorePattern = options.get("warningsIgnorePattern", "")

			if (options.get("deployDocumentationRemoteOptions") != null) {
				deployDocumentationRemoteOptions = options.get("deployDocumentationRemoteOptions") as Map<String, Object>
				if (deployDocumentationRemoteOptions.containsKey("folders")) {
					throw new IllegalArgumentException("folders-argument in deployDocumentationRemoteOptions would be overwritten.")
				}
			} else {
				deployDocumentationRemoteOptions = null
			}

			Boolean enableCppcheck = options.get("enableCppcheck", true)
			Boolean enableCppcheckVote = options.get("enableCppcheckVote", false)

			Boolean enableClangTidy = options.get("enableClangTidy", true)
			Boolean enableClangTidyVote = options.get("enableClangTidyVote", false)

			Boolean requiresBear = enableCppcheck || enableClangTidy

			// Pre/post test execution hooks
			Closure preTestHook = (Closure) options.get("preTestHook", {})
			Closure postTestHook = (Closure) options.get("postTestHook", {})

			// Directories test-result XML files are written to
			LinkedList<String> testResultDirs = new LinkedList<String>()

			// Annotate the build description with the commit message for gerrit builds
			if (env.GERRIT_CHANGE_COMMIT_MESSAGE != null) {
				currentBuild.description = decodeBase64(env.GERRIT_CHANGE_COMMIT_MESSAGE).split("\n")[0].trim()
			}

			inSingularity(containerOptions) {
				withWaf() {
					// Setup and build the project
					wafSetup(options)

					runOnSlave(label: "frontend") {
						jesh("waf repos-log > repos_log.txt")
					}

					for (String wafTargetOption in options.get("wafTargetOptions", [""])) {
						stage("Build ${wafTargetOption}".trim()) {
							onSlurmResource(partition: "jenkins", "cpus-per-task": "8") {
								withModules(moduleOptions) {
									jesh("${requiresBear ? "bear -- " : ""}waf configure install " +
									     "${testTimeout} " +
									     "--test-execnone " +
									     "${wafTargetOption} ${configureInstallOptions}")
								}
							}
						}

						// Run tests defined in waf for all given test resources
						for (Map<String, String> testSlurmResource in testResources) {
							String testOutputDir = "testOutput_" + UUID.randomUUID().toString()
							testResultDirs.add(testOutputDir)

							stage("Tests ${wafTargetOption} ${testSlurmResource}".trim()) {
								onSlurmResource(testSlurmResource) {
									withModules(moduleOptions) {
										def preTestHookResult = preTestHook()
										jesh("waf build ${wafTargetOption} ${testOptions}")
										jesh("mv build/test_results ${testOutputDir}")
										postTestHook(preTestHookResult)
									}
								}
							}
						}
					}
				}
			}

			// Evaluate waf test results
			stage("Test Evaluation") {
				runOnSlave(label: "frontend") {
					String xmlResultPattern = testResultDirs.join("/**/*.xml, ") + "/**/*.xml"

					// Always keep the plain results
					archiveArtifacts xmlResultPattern

					// Parse test results
					step([$class       : 'XUnitPublisher',
					      thresholdMode: 1,
					      thresholds   : [[$class           : 'FailedThreshold',
					                       unstableThreshold: '0'],
					      ],
					      tools        : [[$class               : 'GoogleTestType',
					                       deleteOutputFiles    : true,
					                       failIfNotNew         : true,
					                       pattern              : xmlResultPattern,
					                       skipNoTestFiles      : false,
					                       stopProcessingIfError: true]
					      ]
					])
				}
			}

			// Check C/C++ source formatting
			conditionalStage(name: "Test clang-format", skip: !options.get("enableClangFormat", true)) {
				runOnSlave(label: "frontend") {
					inSingularity(containerOptions) {
						Boolean enableClangFormatFullDiff = options.get("enableClangFormatFullDiff", false)
						for (String project in options.get("projects")) {
							checkClangFormat([folder  : project.split("@")[0],
							                  fullDiff: (enableClangFormatFullDiff && !isTriggeredByGerrit())])
						}
					}
				}
			}

			conditionalStage(name: "Test cppcheck", skip: !enableCppcheck) {
				onSlurmResource(partition: "jenkins", "cpus-per-task": "8") {
					inSingularity(containerOptions) {
						// FIXME: Issue #3537 cppcheck needs local copy of cfg directory
						jesh("cp -r /opt/spack_views/visionary-wafer/cfg/ .")
						jesh("cppcheck --xml --project=compile_commands.json -j\$(nproc) --suppress=syntaxError:* " +
						     "-i \$(readlink -f build) --enable=warning 2> cppcheck.xml")
					}
				}
			}

			conditionalStage(name: "Test clang-tidy", skip: !enableClangTidy) {
				onSlurmResource(partition: "jenkins", "cpus-per-task": "8") {
					inSingularity(containerOptions) {
						// Issue #3979: Script should be installed in PATH.
						// (mis-)use PYTHONHOME to get the app's root directory
						jesh("python \$PYTHONHOME/share/clang/run-clang-tidy.py -j8 -p . &> clang-tidy.txt || true")
					}
				}
			}

			// Scan for compiler and linting warnings
			stage("Compiler/Linting Warnings") {
				runOnSlave(label: "frontend") {
					recordIssues(qualityGates: [[threshold: 1,
					                             type     : 'TOTAL',
					                             unstable : true]],
					             blameDisabled: true,
					             skipPublishingChecks: true,
					             filters: [excludeFile(".*usr/include.*"),
					                       excludeFile(".*opt/spack.*")] +
					                      warningsIgnorePattern.split(",").collect({ param -> return excludeFile(param) }),
					             tools: [gcc(id: "gcc_" + UUID.randomUUID().toString(),
					                         name: "GCC")]
					)

					recordIssues(qualityGates: [[threshold: 1,
					                             type     : 'TOTAL',
					                             unstable : true]],
					             blameDisabled: true,
					             skipPublishingChecks: true,
					             tools: [pyLint(pattern: testResultDirs.join("/**/*.pylint, ") + "/**/*.pylint",
					                            id: "pylint_" + UUID.randomUUID().toString(),
					                            name: "Pylint Warnings"),
					                     pep8(pattern: testResultDirs.join("/**/*.pycodestyle, ") + "/**/*.pycodestyle",
					                          id: "pep8_" + UUID.randomUUID().toString(),
					                          name: "PEP8")]
					)

					if (enableCppcheck) {
						List<Map<String, Object>> qualityGates = []
						if (enableCppcheckVote) {
							qualityGates.add([threshold: 1,
							                  type     : 'TOTAL',
							                  unstable : true])
						}
						recordIssues(qualityGates: qualityGates,
						             blameDisabled: true,
						             skipPublishingChecks: true,
						             filters: [excludeFile(".*usr/include.*"),
						                       excludeFile(".*opt/spack.*")] +
						                      warningsIgnorePattern.split(",").collect({excludeFile(it)}),
						             tools: [cppCheck(id: "cppcheck_" + UUID.randomUUID().toString(),
						                              name: "Cppcheck", pattern: "cppcheck.xml")]
						)
					}

					if (enableClangTidy) {
						List<Map<String, Object>> qualityGates = []
						if (enableClangTidyVote) {
							qualityGates.add([threshold: 1,
							                  type     : 'TOTAL',
							                  unstable : true])
						}
						recordIssues(qualityGates: qualityGates,
						             blameDisabled: true,
						             skipPublishingChecks: true,
						             filters: [excludeFile(".*usr/include.*"),
						                       excludeFile(".*opt/spack.*"),
						                       excludeFile(".*\\.dox\$")] +
						                      warningsIgnorePattern.split(",").collect({ param -> return excludeFile(param) }),
						             tools: [clangTidy(id: "clang_tidy_" + UUID.randomUUID().toString(),
						                               name: "Clang-Tidy", pattern: "clang-tidy.txt")]
						)
					}
				}
			}

			// Deploy built html documentation
			stage("Deploy Documentation") {
				String[] projects

				runOnSlave(label: "frontend") {
					int projects_return = jesh(script: "ls -d doc/*/html", returnStatus: true)
					if (projects_return == 0) {
						String projects_string = jesh(script: "ls -d doc/*/html", returnStdout: true)
						projects = projects_string.split()

						// save documentation into artifact
						archiveArtifacts artifacts: "doc/**/*", onlyIfSuccessful: true
					} else {
						echo("No documentation found to deploy.")
					}

					for (String project in projects) {
						String name = project.split("/")[1]

						// NOTE: 'JENLIB_HTML_DEPLOYMENT' needs to be a registered 'Lockable Resource'!
						// Concurrent deployment from parallel builds of the same job might lead to build errors, if
						// both try to deploy at the same time.
						lock("JENLIB_HTML_DEPLOYMENT") {
							publishHTML([allowMissing         : false,
							             alwaysLinkToLastBuild: false,
							             keepAll              : false,
							             reportDir            : project,
							             reportFiles          : 'index.html',
							             reportName           : "Documentation (" + name + ")",
							             reportTitles         : ''])
						}
					}

					if (deployDocumentationRemoteOptions) {
						if (env.GERRIT_EVENT_TYPE == "change-merged") {
							if (currentBuild.currentResult == "SUCCESS") {
								deployDocumentationRemoteOptions.put("folders", projects)
								deployDocumentationRemote(deployDocumentationRemoteOptions)
							} else {
								echo("Documentation deployment skipped: Unstable build.")
							}
						}
					}
				}
			}
		} catch (Throwable t) {
			notifyFailure(mattermostChannel: notificationChannel)
			throw t
		}

		if (currentBuild.currentResult != "SUCCESS") {
			notifyFailure(mattermostChannel: notificationChannel)
		}
	}
}
