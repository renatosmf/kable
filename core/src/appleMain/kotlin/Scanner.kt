package com.juul.kable

import com.benasher44.uuid.Uuid
import com.juul.kable.CentralManagerDelegate.Response.DidDiscoverPeripheral
import com.juul.kable.logs.Logging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnsupported

public class AppleScanner internal constructor(
    central: CentralManager,
    services: List<Uuid>?,
    logging: Logging,
) : Scanner {

    public override val advertisements: Flow<Advertisement> =
        central.delegate
            .response
            .onStart {
                central.awaitPoweredOn()
                central.scanForPeripheralsWithServices(services, options = null)
            }
            .onCompletion {
                central.stopScan()
            }
            .filterIsInstance<DidDiscoverPeripheral>()
            .map { (cbPeripheral, rssi, advertisementData) ->
                Advertisement(rssi.intValue, advertisementData, cbPeripheral)
            }
}

private suspend fun CentralManager.awaitPoweredOn() {
    delegate.state
        .onEach {
            if (it == CBManagerStateUnsupported ||
                it == CBManagerStateUnauthorized
            ) error("Invalid bluetooth state: $it")
        }
        .first { it == CBManagerStatePoweredOn }
}
