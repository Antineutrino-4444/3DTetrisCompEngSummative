# 3D Rotating Tetracube (Java Prototype)

This repository now contains a playable Java console prototype of the 3D rotating falling-block concept.

## What is implemented

- `6 x 12 x 6` voxel board.
- Curated tetracube piece set.
- One-side-at-a-time projection-style rendering in text.
- Gravity rotation event every 8 locked pieces.
- Full-layer clearing perpendicular to current gravity.
- Re-settle simulation after each rotation (including chain clears).
- Layer-fill meters and a text x-ray pulse command.

## Controls

- `a` / `d` : move left / right in the current projected view.
- `s` : soft drop.
- `w` : hard drop.
- `q` / `e` : rotate in plane.
- `z` / `c` : roll around depth axis.
- `x` : print x-ray slice readout.
- `p` : quit.

## Run

```bash
javac src/main/java/Main.java
java -cp src/main/java Main
```

## Notes

This is intentionally a **console vertical-slice prototype** focused on mechanics and rules. It does not include a graphics engine, audio, menus, or persistence.
