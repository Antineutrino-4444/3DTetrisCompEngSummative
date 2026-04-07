# 3D Rotating Tetracube (Java Windowed Prototype)

This project now runs as a **windowed Java game** (Swing), not a command-line-only demo.

## What's included

- Separate visual game window (`JFrame`) with real-time rendering.
- 2D orthographic projection of a 3D voxel stack (`6 x 12 x 6`).
- Depth-shaded blocks so front/deeper occupancy is readable.
- Curated tetracube piece set and rotation controls.
- Gravity rotation event every 8 locked pieces.
- Full layer clear perpendicular to gravity + post-rotation re-settle.
- Layer fill meters and optional x-ray panel.

## Controls

- `Left` / `Right` : move piece horizontally in current face.
- `Down` : soft drop.
- `Space` : hard drop.
- `Z` / `X` : rotate in-plane.
- `A` / `S` : roll around depth axis.
- `V` : toggle x-ray overlay.
- `P` : pause/unpause.
- `R` : restart after game over.

## Run

```bash
javac src/main/java/Main.java
java -cp src/main/java Main
```

> Requires a Java environment with Swing (standard JDK desktop runtime).
