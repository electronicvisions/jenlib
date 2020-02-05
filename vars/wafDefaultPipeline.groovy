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

			if (containerOptions.get("app") == null) {
				throw new IllegalArgumentException("Container app needs to be specified.")
			}

			Map<String, Object> moduleOptions = (Map<String, Object>) options.get("moduleOptions", [modules: []])
			String testTimeout = ""
			if (options.get("testTimeout") != null) {
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

			// Directories test-result XML files are written to
			LinkedList<String> testResultDirs = new LinkedList<String>()

			inSingularity(containerOptions) {
				withWaf() {
					// Setup and build the project
					wafSetup(options)

					for (String wafTargetOption in options.get("wafTargetOptions", [""])) {
						stage("Build ${wafTargetOption}".trim()) {
							onSlurmResource(partition: "jenkins", "cpus-per-task": "8") {
								withModules(moduleOptions) {
									jesh("waf configure install " +
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
										jesh("waf build ${wafTargetOption} ${testOptions}")
										jesh("mv build/test_results ${testOutputDir}")
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
					step([$class       : 'XUnitBuilder',
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

			// Scan for compiler and linting warnings
			stage("Compiler/Linting Warnings") {
				runOnSlave(label: "frontend") {
					recordIssues(qualityGates: [[threshold: 1,
					                             type     : 'TOTAL',
					                             unstable : true]],
					             blameDisabled: true,
					             filters: [excludeFile(".*usr/include.*"),
					                       excludeFile(".*opt/spack.*"),
					                       excludeFile(".*\\.dox\b")] +
					                      warningsIgnorePattern.split(",").collect({ param -> return excludeFile(param) }),
					             tools: [gcc(id: "gcc_" + UUID.randomUUID().toString(),
					                         name: "GCC Warnings")]
					)

					recordIssues(qualityGates: [[threshold: 1,
					                             type     : 'TOTAL',
					                             unstable : true]],
					             blameDisabled: true,
					             tools: [pyLint(pattern: testResultDirs.join("/**/*.pylint, ") + "/**/*.pylint",
					                            id: "pylint_" + UUID.randomUUID().toString(),
					                            name: "Pylint Warnings"),
					                     pep8(pattern: testResultDirs.join("/**/*.pycodestyle, ") + "/**/*.pycodestyle",
					                          id: "pep8_" + UUID.randomUUID().toString(),
					                          name: "PEP8 Warnings")]
					)
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
					} else {
						echo("No documentation found to deploy.")
					}

					for (String project in projects) {
						String name = project.split("/")[1]

						publishHTML([allowMissing         : false,
						             alwaysLinkToLastBuild: false,
						             keepAll              : false,
						             reportDir            : project,
						             reportFiles          : 'index.html',
						             reportName           : "Documentation (" + name + ")",
						             reportTitles         : ''])
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
