import static java.util.UUID.randomUUID

/**
 * Request a BrainScaleS-2 HICANN-X system with the given chip revision as test resource.
 *
 * @param chipRevision HICANN-X ASIC revision to be used.
 * @return Map of arguments for a slurm job.
 */
def call(int chipRevision) {
	final HxCubeSetup randomSetup = getRandomSetup(chipRevision)
	return [partition          : "batch",
	        wafer              : randomSetup.waferId,
	        "fpga-without-aout": randomSetup.fpgaId]
}

class HxCubeSetup implements Serializable {
	int waferId
	int fpgaId

	HxCubeSetup(int waferId, int fpgaId) {
		this.waferId = waferId
		this.fpgaId = fpgaId
	}

	static HxCubeSetup deserialize(String input) {
		List<String> splitValues = input.split(",")
		return new HxCubeSetup(splitValues[0].toInteger(), splitValues[1].toInteger())
	}
}


private HxCubeSetup getRandomSetup(int chipRevision) {
	String hwdbQuery = """
from dataclasses import dataclass
from typing import List
import random
import pyhwdb

TARGET_CHIP_REVISION: int = ${chipRevision}

@dataclass
class CubeWing:
    wafer_id: int
    fpga_id: int

    def serialize(self) -> str:
        return f"{self.wafer_id},{self.fpga_id}"

if __name__ == '__main__':
    db = pyhwdb.database()
    db.load(db.get_default_path())

    test_setups: List[CubeWing] = []

    for hxcube_id in db.get_hxcube_ids():
        setup = db.get_hxcube_setup_entry(hxcube_id)

        for fpga_id, fpga_entry in setup.fpgas.items():
            if not fpga_entry.wing:
                continue

            if not fpga_entry.ci_test_node:
                continue

            if fpga_entry.wing.chip_revision != TARGET_CHIP_REVISION:
                continue

            test_setups.append(CubeWing(hxcube_id + 60, fpga_id))

    print(random.choice(test_setups).serialize())
"""
	runOnSlave(label: "frontend && singularity") {
		String tempFilePath = "${pwd(tmp: true)}/${randomUUID().toString()}.py"
		writeFile(file: tempFilePath, text: hwdbQuery)
		inSingularity(app: "dls-core",
		              image: "/containers/stable/latest") {
			withModules(modules: ["hwdb_bss2"]) {
				setupString = jesh(script: "python ${tempFilePath}", returnStdout: true).trim()
				return HxCubeSetup.deserialize(setupString)
			}
		}
	}
}
