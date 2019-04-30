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
 *                    <li><b>testSlurmResource</b> (optional): Slurm resource definition tests are run on.
 *                                                             Defaults to <code>[partition: "jenkins", "cpus-per-task": "8"]</code>
 *                    <li><b>testOptions</b> (optional): Options passed to the test execution waf call.
 *                                                       Defaults to <code>"--test-execall"</code>
 *                    <li><b>testTimeout</b> (optional): Timeout of waf test execution call.
 *                    <li><b>warningsIgnorePattern</b> (optional): Compiler warnings to be ignored.
 *                    <li><b>prePipelineCleanup</b> (optional): Cleanup the workspace before the pipeline is run.
 *                                                              Defaults to {@code true}
 *                    <li><b>postPipelineCleanup</b> (optional): Cleanup the workspace after the pipeline has been run.
 *                                                               Defaults to {@code true}
 *                </ul>
 */
def call(Map<String, Object> options = [:]) {
	timestamps {

		boolean prePipelineCleanup = options.get("prePipelineCleanup", true)
		boolean postPipelineCleanup = options.get("postPipelineCleanup", true)

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
			Map<String, String> testSlurmResource = (Map<String, String>) options.get("testSlurmResource",
			                                                                          [partition: "jenkins", "cpus-per-task": "8"])
			String testOptions = options.get("testOptions", "--test-execall")
			String warningsIgnorePattern = options.get("warningsIgnorePattern", "")

			if (prePipelineCleanup) {
				stage("Cleanup") {
					runOnSlave(label: "frontend") {
						cleanWs()
					}
				}
			}

			// Setup and build the project
			wafSetup(options)

			// Directories test-result XML files are written to
			LinkedList<String> testResultDirs = new LinkedList<String>()

			withWaf() {
				// Build and run tests with default target and target="*"
				for (String wafTargetOption in ["", "--targets='*'"]) {
					String testOutputDir = "testOutput_" + UUID.randomUUID().toString()
					testResultDirs.add("build/" + testOutputDir)

					stage("Build ${wafTargetOption}".trim()) {
						onSlurmResource(partition: "jenkins", "cpus-per-task": "8") {
							withModules(moduleOptions) {
								inSingularity(containerOptions) {
									jesh("waf configure install " +
									     "--test-xml-summary=${testOutputDir} " +
									     "${testTimeout} " +
									     "--test-execnone " +
									     "${wafTargetOption} ${configureInstallOptions}")
								}
							}
						}
					}

					// Run tests defined in waf
					stage("Tests ${wafTargetOption}".trim()) {
						onSlurmResource(testSlurmResource) {
							withModules(moduleOptions) {
								inSingularity(containerOptions) {
									jesh("waf build ${wafTargetOption} ${testOptions}")
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
					warnings canComputeNew: false,
					         canRunOnFailed: true,
					         consoleParsers: [[parserName: 'GNU C Compiler 4 (gcc)']],
					         excludePattern: ".*opt/spack.*,*.dox,${warningsIgnorePattern}",
					         unstableTotalAll: '0'

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
				}

				for (String project in projects) {
					String name = project.split("/")[1]

					publishHTML([allowMissing: false,
					            alwaysLinkToLastBuild: false,
					            keepAll: false,
					            reportDir: project,
					            reportFiles: 'index.html',
					            reportName: "Documentation (" + name + ")",
					            reportTitles: ''])
				}
			}
		} catch (Throwable t) {
			notifyFailure(mattermostChannel: notificationChannel)
			throw t
		} finally {
			if (postPipelineCleanup) {
				runOnSlave(label: "frontend") {
					cleanWs()
				}
			}
		}

		if (currentBuild.currentResult != "SUCCESS") {
			notifyFailure(mattermostChannel: notificationChannel)
		}
	}
}
