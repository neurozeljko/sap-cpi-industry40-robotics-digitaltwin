package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

Message processData(Message message) {
    String rawBomJson = message.getProperty("Prop_Raw_BOM_Payload") as String
    String rootParent = message.getProperty("Prop_Parent_Material") as String
    
    def jsonSlurper = new JsonSlurper()
    def bomChanges = jsonSlurper.parseText(rawBomJson)

    Map<String, List<String>> graph = [:].withDefault { [] }

    bomChanges.each { item ->
        String parent = item.parentMaterialId ?: rootParent
        String child = item.componentMaterialId
        if (child) {
            graph[parent].add(child)
        }
    }

    Map<String, Integer> visited = [:]
    List<String> cyclePath = []
    boolean hasCycle = false

    def dfs
    dfs = { String node, List<String> currentPath ->
        visited[node] = 1
        currentPath.add(node)

        for (String neighbor : graph[node]) {
            if (visited[neighbor] == 1) {
                hasCycle = true
                currentPath.add(neighbor)
                cyclePath.addAll(currentPath)
                return true
            }
            if (visited[neighbor] == null || visited[neighbor] == 0) {
                if (dfs(neighbor, currentPath)) {
                    return true
                }
            }
        }

        currentPath.remove(currentPath.size() - 1)
        visited[node] = 2
        return false
    }

    for (String node : graph.keySet()) {
        if (visited[node] == null || visited[node] == 0) {
            if (dfs(node, [])) {
                break
            }
        }
    }

    message.setProperty("Prop_Circular_Cycle_Found", hasCycle.toString())

    if (hasCycle) {
        String cycleReport = cyclePath.join(" ──► ")
        message.setProperty("Prop_Detected_Cycle_Path", cycleReport)
        message.setHeader("X-BOM-Integrity-Status", "CIRCULAR_DEPENDENCY_DETECTED")
    } else {
        message.setProperty("Prop_Detected_Cycle_Path", "NONE")
        message.setHeader("X-BOM-Integrity-Status", "VALID_ACYCLIC_GRAPH")
    }

    return message
}
