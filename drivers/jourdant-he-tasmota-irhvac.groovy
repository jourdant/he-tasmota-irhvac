/*
 * Tasmota IRHVAC
 *
 * Exposes Thermostat to HE using Tasmota IRHVAC commands over HTTP.
 * 
 */
 import java.net.URLEncoder

metadata {
    definition(name: "Tasmota IRHVAC", namespace: "jourdant", author: "jourdant", importUrl: "https://raw.githubusercontent.com/jourdant/he-tasmota-irhvac/master/drivers/jourdant-he-tasmota-irhvac.groovy") {
        capability "Thermostat"
        capability "Switch"
    }
}

preferences {
    section("Tasmota Settings") {
        input name: "tasmotaIpAddress", type: "string", title: "IP Address", defaultValue: "192.168.1.254", required: true
        input name: "tasmotaUsername", type: "string", title: "Username", defaultValue: "admin", required: false
        input name: "tasmotaPassword", type: "password", title: "Password", required: false
    }
    section("Thermostat Settings") {
        //sample payload:
        //{"IRHVAC":{"Vendor":"PANASONIC_AC","Model":1,"Power":"Off","Mode":"Off","Celsius":"On","Temp":21,"FanSpeed":"Medium","SwingV":"Off","SwingH":"Off","Quiet":"Off","Turbo":"Off","Econo":"Off","Light":"Off","Filter":"Off","Clean":"Off","Beep":"Off","Sleep":-1}}
        //
        input name: "hvacVendor", type: "enum", title: "Vendor", defaultValue: "PANASONIC_AC", required: true, options: ["COOLIX","DAIKIN","KELVINATOR","MITSUBISHI_AC","GREE","ARGO","TROTEC","TOSHIBA_AC","FUJITSU_AC","MIDEA","HAIER_AC","HITACHI_AC","HAIER_AC_YRW02","WHIRLPOOL_AC","SAMSUNG_AC","ELECTRA_AC","PANASONIC_AC","DAIKIN2","VESTEL_AC","TECO","TCL112AC","MITSUBISHI_HEAVY_88","MITSUBISHI_HEAVY_152","DAIKIN216","SHARP_AC","GOODWEATHER","DAIKIN160","NEOCLIMA","DAIKIN176","DAIKIN128","AMCOR","DAIKIN152","MITSUBISHI136","MITSUBISHI112","HITACHI_AC424"]
        input name: "hvacModel", type: "number", title: "Model Number", defaultValue: 0, required: true

        input name: "hvacPower", type: "bool", title: "Power", defaultValue: true, required: true

        input name: "hvacMode", type: "enum", title: "Mode", defaultValue: "Auto", required: true, options: ["Off", "Auto", "Cool", "Heat", "Dry", "Fan"]
        
        input name: "hvacTemp", type: "number", title: "Temperature", defaultValue: 18, required: true, range: "18..100"
        
        input name: "hvacFanSpeed", type: "enum", title: "Fan Speed", defaultValue: "Auto", required: true, options: ["Auto", "Min", "Low", "Med", "High", "Max"]
        
        input name: "hvacSwingV", type: "enum", title: "Swing Vertical", defaultValue: "Auto", required: true, options: ["Auto", "Off", "Min", "Low", "Mid", "High", "Max"]
        
        input name: "hvacSwingH", type: "enum", title: "Swing Horizontal", defaultValue: "Auto", required: true, options: ["Auto", "Off", "LeftMax", "Left", "Mid", "Right", "RightMax", "Wide"]
    }
    section("Mode Settings") {
        input name: "hvacQuiet", type: "bool", title: "Quiet Mode", defaultValue: false, required: true

        input name: "hvacTurbo", type: "bool", title: "Turbo Mode", defaultValue: false, required: true

        input name: "hvacEcono", type: "bool", title: "Economy Mode", defaultValue: false, required: true

        input name: "hvacLight", type: "bool", title: "LED Mode", defaultValue: true, required: true

        input name: "hvacBeep", type: "bool", title: "Beep Mode", defaultValue: true, required: true
    }
    section("Advanced Settings") {
        input name: "hvacStateMode", type: "enum", title: "Send Mode", defaultValue: "SendOnly", required: true, options: ["SendOnly", "StoreOnly", "SendStore"]
    }
}

def updated() {
    log.debug("updated()")

    //normalise temperature based on hubitat preferences
    String hvacCelsius = getTemperatureScale()=="C" ? "On" : "Off"
    String temperature = (hvacCelsius=="On" ? settings.hvacTemp : celsiusToFahrenheit(settings.hvacTemp)).toString()

    //build command
    String command = "IRHVAC {\"Vendor\": \"${settings.hvacVendor}\",\"Model\": \"${settings.hvacModel}\",\"Power\": \"${settings.hvacPower}\",\"Mode\": \"${settings.hvacMode}\",\"Celsius\": \"${hvacCelsius}\",\"Temp\": \"${temperature}\",\"FanSpeed\": \"${settings.hvacFanSpeed}\",\"SwingV\": \"${settings.hvacSwingV}\",\"SwingH\": \"${settings.hvacSwingH}\",\"Quiet\": \"${settings.hvacQuiet}\",\"Turbo\": \"${settings.hvacTurbo}\",\"Econo\": \"${settings.hvacEcono}\",\"Light\": \"${settings.hvacLight}\",\"Beep\": \"${settings.hvacBeep}\"}"

    //build url
    String url = "http://${settings.tasmotaIpAddress}/cm?"
    if (settings.tasmotaPassword?.trim()) { url += "username=${settings.tasmotaUsername}&password=${settings.tasmotaPassword}&" }
    url += "cmnd=${URLEncoder.encode(command)}" 
    log.debug(url)

    //send command
    try {
        httpGet(url) { resp ->
            if (resp.success) {
                log.debug("Success. Response: '${resp.data}'")
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def auto() {
    log.debug("auto()")
}

def cool() {
    log.debug("cool()")
    BigDecimal temp = 20
    settings.hvacTemp = getTemperatureScale()=="C" ? temp : celsiusToFahrenheit(temp)
    settings.hvacMode = "Cool"
    settings.hvacFanSpeed = "Med"
    updated()
}

def emergencyHeat() {
    log.debug("emergencyHeat()")
    
    BigDecimal temp = 27
    settings.hvacTemp = getTemperatureScale()=="C" ? temp : celsiusToFahrenheit(temp)
    settings.hvacMode = "Heat"
    settings.hvacFanSpeed = "Med"
    updated()
}

def fanAuto() {
    log.debug("fanAuto()")

}

def fanCirculate() {
    log.debug("fanCirculate()")

}

def fanOn() {
    log.debug("fanOn()")
    on()
}

def heat() {
    log.debug("heat()")
    
    BigDecimal temp = 27
    settings.hvacTemp = getTemperatureScale()=="C" ? temp : celsiusToFahrenheit(temp)
    settings.hvacMode = "Heat"
    settings.hvacFanSpeed = "Med"
    updated()
}

def on() {
    log.debug("off()")
    settings.hvacPower = true
    updated()
}

def off() {
    log.debug("off()")
    settings.hvacPower = false
    updated()
}

def setCoolingSetpoint(BigDecimal temperature) {
    log.debug("setCoolingSetpoint(${temperature})")
    
    settings.hvacTemp = temperature
    updated()
}

def setHeatingSetpoint(BigDecimal temperature) {
    log.debug("setHeatingSetpoint(${temperature})")
    
    settings.hvacTemp = temperature
    updated()
}

def setSchedule() {
    log.debug("setSchedule()")
}