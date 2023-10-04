package eu.mobileApp.DriverApp.mapComponents


class MarkerSettings(val on:Boolean) {
    companion object {
        var globalState=false //Set this for first time
        /**
         * In some devices change event is called twice. We limit this with internal state.
         */
        fun setChangeAndPost(_on:Boolean) {
            if (globalState !=_on) { //Send Just Change
                globalState = _on;
                //EventBus.getDefault().post(MarkerSettings(_on))
            }
        }
    }
}