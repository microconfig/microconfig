package io.microconfig.core.configtypes.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.configtypes.ConfigTypeRepository
import io.microconfig.core.configtypes.StandardConfigType

class StandardConfigTypeRepository : ConfigTypeRepository {
    override val configTypes: List<ConfigType> = StandardConfigType.entries
}
