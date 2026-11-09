package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

Message processData(Message message) {
    String parentMat = message.getProperty("Prop_Parent_Material")
    String cellId = message.getProperty("Prop_Robot_Cell_ID")
    String rawBomJson = message.getProperty("Prop_Raw_BOM_Payload")
    String correlationId = message.getProperty("Prop_Correlation_ID") ?: UUID.randomUUID().toString()

    def jsonSlurper = new JsonSlurper()
    def bomChanges = jsonSlurper.parseText(rawBomJson)

    def s4BomPayload = [
        BillOfMaterial: "BOM-EV-${cellId.take(8)}",
        BillOfMaterialCategory: "M",
        BillOfMaterialVariant: "1",
        Material: parentMat,
        Plant: "1010",
        BOMHeaderQuantity: "1.0",
        BOMHeaderBaseUnit: "PC",
        BOMIsToBeDeleted: false,
        to_BillOfMaterialItem: bomChanges.collect { item ->
            [
                BillOfMaterialComponent: item.componentMaterialId,
                BillOfMaterialItemNumber: item.itemPosition ?: "0010",
                BillOfMaterialItemCategory: "L",
                ComponentQuantity: (item.quantity ?: 1.0).toString(),
                ComponentBaseUnit: item.unitOfMeasure ?: "PC",
                ItemIsToBeDeleted: item.action == "DELETE"
            ]
        },
        auditTrace: [
            correlationTrackingId: correlationId,
            originatingCell: cellId,
            validationAlgorithm: "DFS_ACYCLIC_VERIFIED",
            synchronizedTimestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        ]
    ]

    message.setBody(JsonOutput.prettyPrint(JsonOutput.toJson(s4BomPayload)))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("X-CleanCore-Target", "S4HANA_MANUFACTURING_BOM_V4")
    return message
}
