# CustomJoinItems

Spigot plugin that gives custom items to players when they join the server.

**Built for Paper 26.1.2**

## Build

```bash
mvn clean package -DskipTests
```

Output: `target/CustomJoinItems-2.1.0.jar`

## Configuration

Items are defined in `items.yml` using **1.13+ material names** (e.g. `material: BONE`, `material: BOOK`, `material: IRON_INGOT`). Numerical item IDs are not supported.

## Requirements

- Java 25+
- Paper 26.1.2+

## Installation

Place the JAR in your server's `plugins/` folder and restart (or use `/reload`).
