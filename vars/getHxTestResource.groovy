import static java.util.UUID.randomUUID

/**
 * Request a BrainScaleS-2 HICANN-X system matching a given filter.
 *
 * @param setupFilter Closure that decides whether an HxCubeSetup is eligible.
 * @return Map of arguments for a slurm job.
 */
def call(Closure<Boolean> setupFilter) {
	final List<HxCubeSetup> availableSetups = getSetups().findAll(setupFilter)

	if (availableSetups.isEmpty()) {
		throw new IllegalStateException("No matching HxCube setup found")
	}

	final HxCubeSetup randomSetup = availableSetups[new Random().nextInt(availableSetups.size())]
	return ["cpus-per-task"    : 8,
	        wafer              : randomSetup.waferId,
	        "fpga-without-aout": randomSetup.fpgaId]
}

/**
 * [LEGACY] Request a BrainScaleS-2 HICANN-X system with the given chip revision as test resource.
 *
 * @param chipRevision HICANN-X ASIC revision to be used.
 * @return Map of arguments for a slurm job.
 */
def call(int chipRevision) {
	return call({ it.chipRevision == chipRevision })
}

class HxCubeSetup implements Serializable {
	int waferId
	int fpgaId
	int chipRevision
	int handwrittenChipSerial

	HxCubeSetup(int waferId, int fpgaId, int chipRevision, int handwrittenChipSerial) {
		this.waferId = waferId
		this.fpgaId = fpgaId
		this.chipRevision = chipRevision
		this.handwrittenChipSerial = handwrittenChipSerial
	}

	static HxCubeSetup deserialize(String input) {
		List<String> splitValues = input.split(",")
		return new HxCubeSetup(splitValues[0].toInteger(),
		                       splitValues[1].toInteger(),
		                       splitValues[2].toInteger(),
		                       splitValues[3].toInteger())
	}
}

private List<HxCubeSetup> getSetups() {
	String hwdbQuery = """
from dataclasses import dataclass
import pyhwdb

@dataclass
class CubeWing:
    wafer_id: int
    fpga_id: int
    chip_revision: int
    handwritten_chip_serial: int

    def serialize(self) -> str:
        return f"{self.wafer_id},{self.fpga_id},{self.chip_revision},{self.handwritten_chip_serial}"

if __name__ == '__main__':
    db = pyhwdb.database()
    db.load(db.get_default_path())

    for hxcube_id in db.get_hxcube_ids():
        setup = db.get_hxcube_setup_entry(hxcube_id)

        for fpga_id, fpga_entry in setup.fpgas.items():
            if not fpga_entry.wing:
                continue

            if not fpga_entry.ci_test_node:
                continue

            print(CubeWing(
                hxcube_id + 60,
                fpga_id,
                fpga_entry.wing.chip_revision,
                fpga_entry.wing.handwritten_chip_serial
            ).serialize())
"""
	runOnSlave(label: "apptainer") {
		String tempFilePath = "${pwd(tmp: true)}/${randomUUID().toString()}.py"
		writeFile(file: tempFilePath, text: hwdbQuery)
		inSingularity(app: "dls-core",
		              image: "/containers/stable/latest") {
			withModules(modules: ["hwdb_bss2"]) {
				jesh(script: "python ${tempFilePath}", returnStdout: true)
						.trim()
						.split("\n")
						.collect { HxCubeSetup.deserialize(it) }
			}
		}
	}
}
