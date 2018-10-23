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
 *                    <li><b>app</b> (mandatory): Container app to be used
 *                    <li><b>configureInstallOptions</b> (optional): Options passed to the
 *                                                        <code>waf configure install</code> call.
 *                                                        Defaults to <code>""</code>, <code>--test-execnone</code> is always set.
 *                    <li><b>testSlurmResource</b> (optional): Slurm resource definition tests are run on.
 *                                                             Defaults to <code>[partition: "jenkins", "cpus-per-task": "8"]</code>
 *                    <li><b>testOptions</b> (optional): Options passed to the test execution waf call.
 *                                                       Defaults to <code>"--test-execall"</code>
 *                    <li><b>warningsIgnorePattern</b> (optional): Compiler warnings to be ignored.
 *                </ul>
 */
def call(Map<String, Object> options = [:]) {
	if (options.get("app") == null) {
		throw new IllegalArgumentException("Container app is a mandatory argument.")
	}

	if (options.get("configureInstallOptions")?.contains("--target")) {
		throw new IllegalArgumentException("Cannot overwrite target definition.")
	}

	if (options.get("configureInstallOptions")?.contains("test")) {
		throw new IllegalArgumentException("Cannot overwrite test definition.")
	}

	if (options.get("testOptions")?.contains("--target")) {
		throw new IllegalArgumentException("Cannot overwrite target definition.")
	}

	String app = options.get("app")
	String configureInstallOptions = options.get("configureInstallOptions", "")
	Map<String, String> testSlurmResource = (Map<String, String>) options.get("testSlurmResource",
	                                                                          [partition: "jenkins", "cpus-per-task": "8"])
	String testOptions = options.get("testOptions", "--test-execall")
	String warningsIgnorePattern = options.get("warningsIgnorePattern", "")

	stage("Cleanup") {
		runOnSlave(label: "frontend") {
			cleanWs()
		}
	}

	// Setup and build the project
	wafSetup(options)

	// Directories test-result XML files are written to
	LinkedList<String> testResultDirs = new LinkedList<String>()

	inSingularity(app: app) {
		withWaf() {
			// Build and run tests with default target and target="*"
			for (String wafTargetOption in ["", "--target='*'"]) {
				String testOutputDir = "testOutput_" + UUID.randomUUID().toString()
				testResultDirs.add("build/" + testOutputDir)

				stage("Build ${wafTargetOption}".trim()) {
					onSlurmResource(partition: "jenkins", "cpus-per-task": "8") {
						sish("waf configure install " +
						     "--test-xml-summary=${testOutputDir} " +
						     "--test-execnone " +
						     "${wafTargetOption} ${configureInstallOptions}")
					}
				}

				// Run tests defined in waf
				stage("Tests ${wafTargetOption}".trim()) {
					onSlurmResource(testSlurmResource) {
						sish("waf build ${testOptions}")
					}
				}
			}
		}
	}

	// Evaluate waf test results
	stage("Test Evaluation") {
		runOnSlave(label: "frontend") {
			step([$class       : 'XUnitBuilder',
			      thresholdMode: 1,
			      thresholds   : [[$class           : 'FailedThreshold',
			                       unstableThreshold: '0'],
			      ],
			      tools        : [[$class               : 'GoogleTestType',
			                       deleteOutputFiles    : true,
			                       failIfNotNew         : true,
			                       pattern              : testResultDirs.join("/**/*.xml, ") + "/**/*.xml",
			                       skipNoTestFiles      : false,
			                       stopProcessingIfError: true]
			      ]
			])
		}
	}

	// Scan for compiler warnings
	stage("Compiler Warnings") {
		runOnSlave(label: "frontend") {
			warnings canComputeNew: false,
			         canRunOnFailed: true,
			         consoleParsers: [[parserName: 'GNU C Compiler 4 (gcc)'],
			                          [parserName: 'Clang (LLVM based)']],
			         excludePattern: ".*/opt/spack.*,${warningsIgnorePattern}"
			unstableTotalAll: '0'
		}
	}
}
