import static java.util.UUID.randomUUID

/**
 * Reconfigure a HX-Cube Kintex7 FPGA with a given bitfile using Xilinx SmartLynq.
 * The step needs to run within a SLURM allocation on the to-be-configured cube setup.
 *
 * The to be configured bitfile defaults to the latest stable bitfile for the corresponding chip revision, it may be
 * overwritten by the build parameter 'OVERWRITE_HX_CUBE_BITFILE' ot the commit message key 'HX-Cube-Bitfile'.
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
		inSingularity(image: getAsicContainerPath()) {
			withModules(modules: ["xilinx/2020.1"]) {
				withWaf {
					wafSetup(projects: ["tools-xilinx"])
					jesh("waf configure")
					jesh("waf install --test-execnone")
				}

				withModules(modules: ["localdir"]) {
					jesh("hxcube_smartlynq_program.py ${bitfilePath}")
				}
			}
		}
	}
}

private String getAsicContainerPath() {
	getDefaultFixturePath(defaultPathCanonical: "/containers/stable/asic_latest",
	                      commitKey: "In-ASIC-Container",
	                      parameterName: "OVERWRITE_DEFAULT_ASIC_CONTAINER_IMAGE")
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
import pyhalco_hicann_v2 as halco


def get_chip_revision(hwdb):
    hw_license = os.environ["SLURM_HARDWARE_LICENSES"]
    wafer_id = int(halco.from_string(hw_license).toWafer())
    hxcube_id = wafer_id - 60
    return hwdb.get_hxcube_entry(hxcube_id).chip_revision


if __name__ == '__main__':
    db = pyhwdb.database()
    db.load(db.get_default_path())
    print(get_chip_revision(db))
"""

	int chipRevision = -1
	String tempFilePath = "${pwd(tmp: true)}/${randomUUID().toString()}.py"
	writeFile(file: tempFilePath, text: hwdbQuery)
	inSingularity(app: "wafer") {
		withModules(modules: ["hwdb_bss1"]) {
			chipRevision = jesh(script: "python ${tempFilePath}", returnStdout: true).trim().toInteger()
		}
	}
	return chipRevision
}
