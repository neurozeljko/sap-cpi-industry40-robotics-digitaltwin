package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

Message processData(Message message) {
    String parentMat = message.getProperty("Prop_Parent_Material")
    String cellId = message.getProperty("Prop_Robot_Cell_ID")
    String lineId = message.getProperty("Prop_Assembly_Line")
    String cyclePath = message.getProperty("Prop_Detected_Cycle_Path")
    String correlationId = message.getProperty("Prop_Correlation_ID") ?: UUID.randomUUID().toString()

    def quarantinePayload = [
        circularDependencyQuarantine: [
            incidentTrackingId: correlationId,
            robotCellId: cellId,
            assemblyLineId: lineId,
            rootMaterial: parentMat,
            detectedCycle: cyclePath,
            threatMitigation: "BLOCKED_BEFORE_S4HANA_EXECUTION_DEADLOCK_PREVENTED",
            recommendedAction: "INSPECT_DYNAMIC_LINE_RECONFIGURATION_RULES",
            quarantineTimestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        ]
    ]

    message.setBody(JsonOutput.prettyPrint(JsonOutput.toJson(quarantinePayload)))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("X-BOM-Integrity-Status", "QUARANTINED_CYCLE_DETECTED")
    return message
}
