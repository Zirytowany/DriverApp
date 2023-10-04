package eu.mobileApp.DriverApp.login

data class Departure(
    var RoadCardID:String,
    var DepartureDt: String?,
    var DriverDepartureDt: String?,
    var DepartureComment: String?,
    var DepartureDtMin: String?,
    var ArrivalDt: String?,
    var DriverArrivalDt: String?,
    var ArrivalComment: String?,
    var ArrivalDtMax: String?,
    var PlanArrivalDt: String?,
    var PlanArrivalComment: String?,
    var PlanArrivalConfirmation:String?,
    var NextDepartueDTPlan: String?,
    var NextDepartureConfirmation: String?,
    var DepartureArrivalComment: String?,
    var DepartureArrivalCommentDT: String?
)
