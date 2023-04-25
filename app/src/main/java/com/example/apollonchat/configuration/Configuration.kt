package com.example.apollonchat.configuration

import java.net.InetAddress

data class Configuration(
    var RemoteAddress : InetAddress,
    var SecureConnection : Boolean,
    var OverwriteDatabase : Boolean,
) {
    constructor() : this(InetAddress.getByName("10.0.2.2"), false, false)
}
