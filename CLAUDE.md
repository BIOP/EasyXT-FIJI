# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EasyXT for ImageJ is a Java library that bridges Bitplane Imaris (microscopy software) and FIJI/ImageJ. It provides Java-friendly APIs for interacting with Imaris via ICE (ZeroC) remote procedure calls, enabling XTension development.

**Requires:** Imaris installed and running, Windows, Java 8.

## Build Commands

```bash
# Build (skip tests — tests require running Imaris instance)
mvn clean install -DskipTests

# Run tests (requires Windows + Imaris installed at C:/Program Files/Bitplane/)
mvn test

# Run a single test
mvn test -Dtest=TestDemos#methodName
```

Parent POM is `org.scijava:pom-scijava:37.0.0`. Java source/target is 1.8.

## Architecture

### Core API: `EasyXT.java` (~2400 lines)

The central class is `ch.epfl.biop.imaris.EasyXT` — a large static utility organized into inner classes accessed as `EasyXT.Foo.method()`:

| Inner Class | Purpose |
|---|---|
| `Files` | Open/save Imaris files |
| `Samples` | Download demo data from Zenodo |
| `Scene` | Manage surpass scene (find/add/remove items, groups) |
| `Dataset` | Create and manipulate 3D image datasets, calibration |
| `Surfaces` | 3D surface objects — create, retrieve, statistics |
| `Spots` | Point objects — create, retrieve, track IDs |
| `Stats` | Query and insert statistics on Imaris objects |
| `Tracks` | Tracking data management |
| `Utils` | Connection management, type conversions, RGBA helpers |

### Builder Classes

Complex operations use the builder pattern:
- **`SpotsDetector`** — configures and runs spot detection (wraps `DetectSpots2` / `DetectSpotsRegionGrowing`)
- **`SurfacesDetector`** — configures surface detection with thresholds and filters
- **`ItemTracker`** — configures tracking algorithms (BrownianMotion, Lineage, etc.)
- **`StatsQuery`** — queries statistics filtered by item IDs, stat names, channels, timepoints; returns ImageJ `ResultsTable`
- **`StatsCreator`** — creates statistics for insertion into Imaris items
- **`ItemQuery`** — searches the surpass scene by name/type/parent

### Supporting Classes

- **`ImarisCalibration`** (extends `ij.measure.Calibration`) — wraps Imaris dataset spatial/channel calibration
- **`ItemType`** (enum) — defines 11 Imaris object types with functional interfaces for type checking/conversion

### SciJava Commands (`command/` package)

ImageJ menu commands annotated with `@Plugin`, registered under `Plugins>BIOP>EasyXT>`. These are the user-facing entry points (e.g., `GetImarisDatasetCommand`, `MakeSurfaceCommand`, `PutSurfaceCommand`).

### Demos (`demo/` package)

18 runnable examples demonstrating API usage. These double as integration tests — `TestDemos.java` executes each demo as a JUnit test (conditionally, only when Imaris is available on Windows).

## Key Dependencies

- **com.bitplane:imaris-lib:10.0.0** — proprietary Imaris Java API (ICE-based)
- **net.imagej:imagej** + **net.imagej:ij** — ImageJ/FIJI core
- **org.framagit.mcib3d:mcib3d-core** — 3D image processing
- **fr.inra.ijpb:MorphoLibJ** — morphological operations
- **sc.fiji:imagescience** — advanced image algorithms