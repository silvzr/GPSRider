package com.dvhamham.docs

/**
 * Docs: Intent API Documentation for GPS Rider
 * Only the supported features are documented here.
 */
object Docs {
    data class Feature(
        val name: String,
        val action: String,
        val description: String = "",
        val parameters: List<String> = emptyList()
    )

    val features = listOf(
        Feature(
            name = "Start Fake Location",
            action = "com.dvhamham.START_FAKE_LOCATION",
            description = "Start fake location service with current location"
        ),
        Feature(
            name = "Stop Fake Location",
            action = "com.dvhamham.STOP_FAKE_LOCATION",
            description = "Stop fake location service"
        ),
        Feature(
            name = "Toggle Fake Location",
            action = "com.dvhamham.TOGGLE_FAKE_LOCATION",
            description = "Toggle fake location on/off"
        ),
        Feature(
            name = "Get Status",
            action = "com.dvhamham.GET_STATUS",
            description = "Get current fake location status"
        ),
        Feature(
            name = "Get Current Location",
            action = "com.dvhamham.GET_CURRENT_LOCATION",
            description = "Get current fake location coordinates"
        ),
        Feature(
            name = "Set Custom Location",
            action = "com.dvhamham.SET_CUSTOM_LOCATION",
            description = "Set custom location and start fake location",
            parameters = listOf("latitude (float)", "longitude (float)")
        ),
        Feature(
            name = "Set Accuracy",
            action = "com.dvhamham.SET_ACCURACY",
            description = "Set location accuracy",
            parameters = listOf("accuracy (float)")
        ),
        Feature(
            name = "Randomize Location",
            action = "com.dvhamham.RANDOMIZE_LOCATION",
            description = "Randomize location within specified radius",
            parameters = listOf("radius (double, optional, default: 100.0)")
        )
    )
} 