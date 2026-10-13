# Vehicle image assets

Each named vehicle has its own generated 1672 x 941 studio hero at
`/<vehicle-slug>/hero.webp`. The Java repository must point to the directory whose slug matches the
vehicle so one model can never silently reuse another model's photograph.
Each directory also contains `hero-960.webp`, a responsive derivative of the same image for cards
and narrow screens.

Only add a 360-degree sequence when a complete, legally reusable image set is available.

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
