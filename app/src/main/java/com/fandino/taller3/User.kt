package com.fandino.taller3

data class User(
    var name: String,
    var lastName: String,
    var id: Long,
    var latitude: Double,
    var longitude: Double,
    var available: Boolean
) {
    constructor() : this("", "", 0L, 0.0, 0.0, true)
}