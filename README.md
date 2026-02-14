# CustomJoinItems

Spigot plugin that gives custom items to players when they join the server.

**Built for Spigot 1.21.1**

## Build

```bash
mvn clean package -DskipTests
```

Output: `target/CustomJoinItems-2.0.jar`

## Configuration

Items are defined in `items.yml` using **1.13+ material names** (e.g. `material: BONE`, `material: BOOK`, `material: IRON_INGOT`). Numerical item IDs are not supported.

## Requirements

- Java 21+
- Spigot 1.21.1 (or Paper 1.21.x)

## Installation

Place the JAR in your server's `plugins/` folder and restart (or use `/reload`).
