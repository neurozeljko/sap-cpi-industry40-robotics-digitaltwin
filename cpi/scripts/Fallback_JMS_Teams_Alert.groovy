package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

Message processData(Message message) {
    def exception = message.getProperty("CamelExceptionCaught")
    String cellId = message.getProperty("Prop_Robot_Cell_ID") ?: "UNKNOWN_CELL"
    String lineId = message.getProperty("Prop_Assembly_Line") ?: "UNKNOWN_LINE"
    String correlationId = message.getHeader("SAP_MplCorrelationId", String) ?: UUID.randomUUID().toString()
    String errorMsg = exception ? exception.getMessage() : "Manufacturing ERP synchronization unavailable (HTTP 503 Maintenance Mode)."

    def jmsStoragePayload = [
        zeroDowntimeFallback: [
            incidentTrackingId: correlationId,
            targetSystem: "S4HANA_CLOUD_MANUFACTURING",
            robotCellId: cellId,
            assemblyLineId: lineId,
            bufferedPayload: message.getBody(String),
            replayPolicy: "AUTO_REPLAY_ON_ERP_RESTORE_200",
            quarantineJmsQueue: "queue:/industry40/vpp/robotics/replay",
            failureCause: errorMsg,
            timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        ]
    ]

    def teamsWebhookCard = [
        "@type": "MessageCard",
        "@context": "http://schema.org/extensions",
        "themeColor": "D9381E",
        "summary": "Industry 4.0 ERP Outage Alert: Robot Cell ${cellId}",
        "sections": [[
            "activityTitle": "🚨 Manufacturing Digital Twin Disconnected: Zero-Downtime Fallback Active",
            "activitySubtitle": "Incident ID: ${correlationId}",
            "facts": [
                ["name": "Robot Cell:", "value": cellId],
                ["name": "Assembly Line:", "value": lineId],
                ["name": "Fault Status:", "value": "S/4HANA 503 Maintenance - State Buffered in JMS"],
                ["name": "Replay Action:", "value": "Automatic Re-drive when Cloud Core Resumes"]
            ],
            "markdown": true
        ]]
    ]

    message.setProperty("Prop_Teams_Webhook_Payload", JsonOutput.toJson(teamsWebhookCard))
    message.setBody(JsonOutput.prettyPrint(JsonOutput.toJson(jmsStoragePayload)))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("X-Zero-Downtime-Status", "BUFFERED_IN_JMS_REPLAY_QUEUE")
    return message
}
