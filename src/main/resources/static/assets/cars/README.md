# Vehicle image assets

Only place a vehicle here when a complete, legally reusable image set is available.

Expected structure:

```text
vehicle-slug/
|-- hero.webp
|-- gallery/
|   `-- detail-01.webp
`-- 360/
    |-- frame-001.webp
    |-- frame-002.webp
    `-- ...
```

The Java `VehicleImageSequence` pattern uses `{frame}` for a three-digit, one-based frame number,
for example `/assets/cars/vehicle-slug/360/frame-{frame}.webp`. Do not enable the viewer until every
declared frame exists and all frames show the same vehicle under consistent lighting.
