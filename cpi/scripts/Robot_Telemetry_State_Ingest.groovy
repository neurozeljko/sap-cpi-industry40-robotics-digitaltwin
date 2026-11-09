package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

Message processData(Message message) {
    def reader = message.getBody(java.io.Reader)
    def jsonSlurper = new JsonSlurper()
    def payload = jsonSlurper.parse(reader)

    String cellId = payload.robotCellId ?: "CELL-KUKA-UNKNOWN"
    String lineId = payload.assemblyLineId ?: "LINE-01"
    String targetParentMaterial = payload.parentMaterialNumber ?: "MAT-CHASSIS-EV-900"
    def bomChanges = payload.bomItemModifications ?: []
    def robotTelemetry = payload.telemetrySnapshot ?: [:]

    if (bomChanges.isEmpty()) {
        throw new IllegalArgumentException("ROBOTICS_PAYLOAD_INVALID: Dynamic BOM modification list is empty.")
    }

    double actuatorTorqueNm = (robotTelemetry.actuatorTorqueNm ?: 0.0) as Double
    double axisTemperatureC = (robotTelemetry.axisTemperatureC ?: 0.0) as Double
    boolean isRobotFault = (axisTemperatureC > 85.0 || actuatorTorqueNm > 450.0 || robotTelemetry.faultCode != null)

    message.setProperty("Prop_Robot_Cell_ID", cellId)
    message.setProperty("Prop_Assembly_Line", lineId)
    message.setProperty("Prop_Parent_Material", targetParentMaterial)
    message.setProperty("Prop_Robot_Fault_Active", isRobotFault.toString())
    message.setProperty("Prop_BOM_Changes_Count", bomChanges.size())
    message.setProperty("Prop_Raw_BOM_Payload", JsonOutput.toJson(bomChanges))

    def twinStateSummary = [
        digitalTwinState: [
            cellId: cellId,
            assemblyLine: lineId,
            parentMaterial: targetParentMaterial,
            robotFaultTriggered: isRobotFault,
            telemetry: [
                axisTemperatureC: axisTemperatureC,
                actuatorTorqueNm: actuatorTorqueNm,
                operationalMode: isRobotFault ? "LINE_RECONFIGURATION_REQUIRED" : "NOMINAL_DISPATCH"
            ],
            bomModificationsQueued: bomChanges.size(),
            evaluatedAt: new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        ]
    ]

    message.setBody(JsonOutput.toJson(twinStateSummary))
    return message
}
