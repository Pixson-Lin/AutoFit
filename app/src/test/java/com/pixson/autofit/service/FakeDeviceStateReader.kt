package com.pixson.autofit.service

class FakeDeviceStateReader(
    private val state: DeviceState = DeviceState(
        batteryLevel = 80,
        screenOn = true,
        charging = false,
    ),
) : DeviceStateSource {
    override fun read(): DeviceState = state
}
