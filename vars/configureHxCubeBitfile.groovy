import static java.util.UUID.randomUUID

/**
 * Reconfigure a HX-Cube Kintex7 FPGA with a given bitfile using Xilinx SmartLynq.
 * The step needs to run within a SLURM allocation on the to-be-configured cube setup.
 *
 * The to be configured bitfile defaults to the latest stable bitfile for the corresponding chip revision, it may be
 * overwritten by the build parameter 'OVERWRITE_HX_CUBE_BITFILE' ot the commit message key 'HX-Cube-Bitfile'.
 *
 * Requires a 'JENLIB_JTAG_ACCESS' lock.
 *
 * @param options Map of options:
 *                <ul>
 *                    <li><b>bitfilePath</b> (optional): Override the default bitfile path and flash a different one.
 *                                                       This path should point to a directory containing the bitfile.
 *                    </li>
 *                </ul>
 */
void call(Map<String, Object> options = [:]) {
	if (jesh(script: "env | grep \"SLURM_JOB_PARTITION=cube\"", returnStatus: true) != 0) {
		throw new IllegalStateException("'configureHxCubeBitfile' must be called within an allocation on the 'cube' " +
		                                "partition!")
	}
	Map<String, Object> internalOptions = (Map<String, Object>) options.clone()
	String bitfilePath = internalOptions.get("bitfilePath", getDefaultBitfilePath())

	String toolsXilinxTop = "${steps.pwd(tmp: true)}/toolsXilinxTop_${randomUUID().toString()}"
	dir(toolsXilinxTop) {
		inSingularity(image: getDefaultAsicContainerPath()) {
			withModules(modules: ["xilinx/2020.1"]) {
				withWaf {
					wafSetup(projects: ["tools-xilinx"])
					jesh("waf configure")
					jesh("waf install --test-execnone")
				}

				withModules(modules: ["localdir"]) {
					lock("JENLIB_JTAG_ACCESS") {
						jesh("program_fpga_jtag.py --verbose ${bitfilePath}")
					}
				}
			}
		}

		// Make sure the setup is accessible after the reconfiguration
		jesh("hostname")
		jesh("ip route get \${SLURM_FPGA_IPS}")
		jesh("ping -c 5 -i 0.2 \${SLURM_FPGA_IPS}")
	}
}

private String getDefaultBitfilePath() {
	getDefaultFixturePath(defaultPathCanonical: "/ley/data/bitfiles/fcp-kintex/hxfpga/cube/HXv${getChipRevision()}/" +
	                                            "stable/latest",
	                      commitKey: "HX-Cube-Bitfile",
	                      parameterName: "OVERWRITE_HX_CUBE_BITFILE")
}


private int getChipRevision() {
	String hwdbQuery = """
import os
import pyhwdb


def get_chip_revision(hwdb):
    hw_license = os.environ["SLURM_HARDWARE_LICENSES"]
    # license parsing should come from halco (Issue #3701)
    wafer_id = int(hw_license.split('F')[0].split('W')[1])
    fpga_id = int(hw_license.split('F')[1])
    hxcube_id = wafer_id - 60
    return hwdb.get_hxcube_setup_entry(hxcube_id).fpgas[fpga_id].wing.chip_revision


if __name__ == '__main__':
    db = pyhwdb.database()
    db.load(db.get_default_path())
    print(get_chip_revision(db))
"""

	int chipRevision = -1
	String tempFilePath = "${pwd(tmp: true)}/${randomUUID().toString()}.py"
	writeFile(file: tempFilePath, text: hwdbQuery)
	inSingularity(app: "dls-core",
	              image: "/containers/stable/latest") {
		withModules(modules: ["hwdb_bss2"]) {
			chipRevision = jesh(script: "python ${tempFilePath}", returnStdout: true).trim().toInteger()
		}
	}
	return chipRevision
}
