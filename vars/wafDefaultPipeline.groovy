/**
 * Pipeline for verifying "typical" waf projects:
 * <ul>
 *     <li>Cleanup, setup, build of the given projects
 *     <li>Run tests
 *     <li>Evaluate test results
 * </ul>
 *
 * By default, the project is built and tested for the default target definition.
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
 *                    <li><b>buildRunner</b> (optional): <code>Closure</code> that receives a <code>Closure</code> that executes
 *                                                       the waf build calls. Used to override how or where the build waf calls are run.
 *                                                       Defaults to <code>onSlurmResource.&call.curry("cpus-per-task": 8)</code>
 *                    <li><b>testRunners</b> (optional): Map of <code>[String id: Closure<]/code>, where the value receives a <code>Closure</code> as argument,
 *                                                       that runs the tests. If multiple entries are given, tests are run via each one.
 *                                                       Defaults to <code>["sw": onSlurmResource.&call.curry("cpus-per-task": 8)]</code>
 *                    <li><b>testOptions</b> (optional): Options passed to the test execution waf call.
 *                                                       Defaults to <code>"--test-execall"</code>
 *                    <li><b>testTimeout</b> (optional): Timeout of waf test execution call.
 *                    <li><b>warningsIgnorePattern</b> (optional): Compiler warnings to be ignored.
 *                    <li><b>wafTargetOptions</b> (optional): List of targets to be built.
 *                                                            Defaults to <code>[""]</code>, representing only the default target set.
 *                    <li><b>enableCcache</b> (optional): Enable ccache.
                                                               Defaults to <code>isTriggeredByGerrit()</code>.
 *                    <li><b>enableClangFormat</b> (optional): Enable clang-format checks.
                                                               Defaults to <code>true</code>.
 *                    <li><b>enableClangFormatFullDiff</b> (optional): Enable clang-format to check on the complete project instead
                                                               of the difference of the last commit.
                                                               Defaults to <code>false</code>.
 *                    <li><b>enableCppcheck</b> (optional): Enable cppcheck checks. This needs `bear` to be available.
                                                               Defaults to <code>env.JOB_NAME.contains("nightly")</code> (due to long runtime).
 *                    <li><b>enableCppcheckVote</b> (optional): Enable cppcheck voting unstable if warnings/errors are found.
                                                               Defaults to <code>false</code>.
 *                    <li><b>enableClangTidy</b> (optional): Enable clang-tidy checks. This needs `bear` to be available.
                                                               Defaults to <code>env.JOB_NAME.contains("nightly")</code> (due to long runtime).
 *                    <li><b>enableClangTidyVote</b> (optional): Enable clang-tidy voting unstable if warnings/errors are found.
                                                               Defaults to <code>false</code>.
 *                    <li><b>enableDoxygenCheck</b> (optional): Enable doxygen warning checks.
                                                               Defaults to <code>true</code>.
 *                    <li><b>enableDoxygenCheckVote</b> (optional): Enable doxygen warning checks voting unstable if warnings/errors are found.
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

			Closure buildRunner = options.get("buildRunner", onSlurmResource.&call.curry("cpus-per-task": 8))

			if (options.get("testSlurmResource") != null) {
				echo "[WARNING] testSlurmResource is deprecated! Use testRunners instead!"
			}
			if (options.get("testRunners") != null && options.get("testSlurmResource") != null) {
				throw new IllegalArgumentException("Cannot specify testRunners and testSlurmResource.")
			}

			Map<String, Closure> testRunners = options.get("testRunners", ["sw": onSlurmResource.&call.curry("cpus-per-task": 8)])
			if (options.get("testSlurmResource") instanceof Map) {
				testRunners = [(options.get("testSlurmResource").toString()): onSlurmResource.&call.curry(options.get("testSlurmResource"))]
			} else if (options.get("testSlurmResource") instanceof List) {
				testRunners = options.get("testSlurmResource").collectEntries { resource ->
					[(resource.toString()): onSlurmResource.&call.curry(resource)]
				}
			} else if (options.get("testSlurmResource") != null) {
				throw new IllegalArgumentException("testSlurmResource argument is malformed.")
			}
			if (testRunners.size() == 0) {
				echo "[WARNING] zero entry map passed to testRunners, this means no tests will be run!"
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

			Boolean enableCcache = options.get("enableCcache", isTriggeredByGerrit())
			Closure withOptionalCcache = enableCcache ? withCcache.&call : { it() }

			Boolean enableCppcheck = options.get("enableCppcheck", env.JOB_NAME.contains("nightly"))
			Boolean enableCppcheckVote = options.get("enableCppcheckVote", false)

			Boolean enableClangTidy = options.get("enableClangTidy", env.JOB_NAME.contains("nightly"))
			Boolean enableClangTidyVote = options.get("enableClangTidyVote", false)

			Boolean requiresBear = enableCppcheck || enableClangTidy

			Boolean enableDoxygenCheck = options.get("enableDoxygenCheck", true)
			Boolean enableDoxygenCheckVote = options.get("enableDoxygenCheckVote", false)

			// Pre/post test execution hooks
			Closure preTestHook = (Closure) options.get("preTestHook", {})
			Closure postTestHook = (Closure) options.get("postTestHook", {})

			// Directories test-result XML files are written to
			LinkedList<String> testResultDirs = new LinkedList<String>()

			// Annotate the build description with the commit message for gerrit builds
			if (env.GERRIT_CHANGE_COMMIT_MESSAGE != null) {
				currentBuild.description = decodeBase64(env.GERRIT_CHANGE_COMMIT_MESSAGE).split("\n")[0].trim()
			}

			withOptionalCcache {
				inSingularity(containerOptions) {
					withWaf() {
						// Setup and build the project
						wafSetup(options)

						runOnSlave(label: "apptainer") {
							jesh("waf repos-log > repos_log.txt")
						}

						for (String wafTargetOption in options.get("wafTargetOptions", [""])) {
							stage("Build ${wafTargetOption}".trim()) {
								buildRunner {
									withModules(moduleOptions) {
										// we prefix doxygen warnings with (doxygen) and filter these out of stderr into doxygen.txt
										jesh("${requiresBear ? "bear -- " : ""}waf configure install " +
											"${testTimeout} " +
											"--test-execnone " +
											"${wafTargetOption} ${configureInstallOptions} " +
											"${enableDoxygenCheck ? " 2> >(tee >(grep \"(doxygen)\" | sed \"s,(doxygen) ,,g\" > doxygen.txt))" : ""}")
									}
								}
							}

							// Run tests defined in waf for all given test resources
							for (Map.Entry<String, Closure> runner : testRunners.entrySet()) {
								String testOutputDir = "testOutput_" + UUID.randomUUID().toString()
								testResultDirs.add(testOutputDir)

								stage("Tests (" + "${runner.key} ${wafTargetOption}".trim() +")") {
									runner.value {
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
			}

			// Evaluate waf test results
			conditionalStage(name: "Test Evaluation", skip: (testRunners.size() == 0)) {
				runOnSlave(label: "lightweight") {
					String xmlResultPattern = testResultDirs.join("/**/*.xml, ") + "/**/*.xml"
					String allTestResultPattern = testResultDirs.join("/**/*, ") + "/**/*"

					// Always keep all plain results
					archiveArtifacts allTestResultPattern

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
				runOnSlave(label: "apptainer") {
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
				buildRunner {
					inSingularity(containerOptions) {
						jesh("cppcheck --xml --project=compile_commands.json -j\$(nproc) --suppress=syntaxError:* " +
						     "-i \$(readlink -f build) --enable=warning 2> cppcheck.xml || exit 0")
					}
				}
			}

			conditionalStage(name: "Test clang-tidy", skip: !enableClangTidy) {
				buildRunner {
					inSingularity(containerOptions) {
						// Issue #3979: Script should be installed in PATH.
						// (mis-)use PYTHONHOME to get the app's root directory
						jesh("python \$PYTHONHOME/share/clang/run-clang-tidy.py -j8 -p . &> clang-tidy.txt || true")
					}
				}
			}

			// Scan for compiler and linting warnings
			stage("Compiler/Linting Warnings") {
				runOnSlave(label: "lightweight") {
					recordIssues(qualityGates: [[threshold: 1,
					                             type     : 'TOTAL',
					                             unstable : true]],
					             blameDisabled: true,
					             skipPublishingChecks: true,
					             filters: [excludeFile(".*usr/include.*"),
					                       excludeFile(".*opt/spack.*"),
					                       excludeMessage(".*Problems running dot:.*")] +
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
						             sourceDirectories: [[path: "build"]],
						             tools: [clangTidy(id: "clang_tidy_" + UUID.randomUUID().toString(),
						                               name: "Clang-Tidy", pattern: "clang-tidy.txt")]
						)
					}

					if (enableDoxygenCheck) {
						List<Map<String, Object>> qualityGates = [
						    [threshold: 1,
						     type     : 'TOTAL',
						     unstable : true]]
						recordIssues(qualityGates: enableDoxygenCheckVote ? qualityGates : [],
						             blameDisabled: true,
						             skipPublishingChecks: true,
						             filters: warningsIgnorePattern.split(",").collect({ param -> return excludeFile(param) }),
						             tools: [doxygen(id: "doxygen_" + UUID.randomUUID().toString(),
						                               name: "Doxygen", pattern: "doxygen.txt")]
						)
					}
				}
			}

			// Deploy built html documentation
			stage("Deploy Documentation") {
				String[] projects

				runOnSlave(label: "lightweight") {
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

						// Concurrent deployment from parallel builds of the same job might lead to build errors, if
						// both try to deploy at the same time.
						lock(resource: "JENLIB_HTML_DEPLOYMENT_${JOB_NAME}") {
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
