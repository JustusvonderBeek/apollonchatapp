package com.cloudsheeptech.anzuchat.configuration

import java.net.InetAddress

data class NetworkConfiguration(
    var RemoteAddress : InetAddress,
    var SecureConnection : Boolean,
    var OverwriteDatabase : Boolean,
) {
    constructor() : this(InetAddress.getByName("10.0.2.2"), false, false)
}
