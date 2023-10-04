package eu.mobileApp.DriverApp.comm

data class Places(
    val Country: String,
    val City: String,
    val ZipCode: String,
    val Address: String,
    val GeoLat: Float,
    val GeoLon: Float,
    val Date: String,
    val Type: String,
    val ShapeDescription: String?,
    val ShapeType: String?,
    val IsActive: Int,
    val rn : Int,
    val PlaceID: Int
)
