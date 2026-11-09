# ADR-018: Industry 4.0 Digital Twin & Robotic Assembly Line Synchronization

## Status
Accepted

## Context
Dynamic assembly line adjustments across thousands of industrial robots can generate circular Bill of Materials (BOM) references during component substitutions. Committing circular dependencies directly into SAP S/4HANA Manufacturing causes database locking and factory downtime. Furthermore, cloud ERP maintenance windows (HTTP 503) must not disrupt manufacturing output.

## Decision
1. Terminate SCADA and robotic telemetry streams at SAP Integration Suite over secured WebSocket and REST endpoints.
2. Implement an in-memory Depth-First Search (DFS) algorithm in Groovy to traverse directed BOM graphs and detect circular references prior to ERP ingestion.
3. Decouple ERP maintenance windows using an asynchronous JMS Dead-Letter Queue buffer with automated re-drive upon recovery and instant Microsoft Teams alerting.

## Consequences
- Eliminates ERP database thread locks and deadlock crashes caused by circular graph topologies.
- Guarantees zero downtime on the physical manufacturing floor during cloud ERP upgrades.
- Maintains strict Clean Core compliance by utilizing standard S/4HANA OData V4 APIs.
