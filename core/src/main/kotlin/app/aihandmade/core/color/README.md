# core/color

The single source of truth for colour state and colour difference across the app. Created to end
the three historical inconsistencies found in the audit:

1. CIEDE2000 was applied to **OKLab** coordinates in the S7 engine (constants are for CIE-Lab).
2. The catalog had **two divergent mappers** (OKLab/ΔE² vs CIE-Lab/ΔE76) that disagreed.
3. Colour math was **duplicated three times** with three different ΔE definitions.

## Core principle: the colour space is part of the type

No bare `FloatArray` carries colour. Each state is a distinct type, so a value in one space can never
be silently fed into a function expecting another. The OKLab-into-CIEDE2000 bug becomes a compile
error, not a runtime mistake.

| Type        | Space                     | Role                                        |
|-------------|---------------------------|---------------------------------------------|
| `Srgb`      | 8-bit sRGB (+alpha)       | I/O boundary (decode/encode)                |
| `LinearRgb` | scene-linear sRGB, D65    | conversion hub; thread blending             |
| `Lab`       | CIE L*a*b*, D65           | metrics, catalog matching, thresholds       |
| `OkLab`     | OKLab                     | quantization / hot inner loops *(to come)*  |

## The ΔE rule (enforced by types)

- **Reporting / matching / thresholds → `deltaE2000(Lab, Lab)`** — canonical CIEDE2000.
- **Hot loops (assignment, greedy residual, nearest) → squared distance in `OkLab`** *(later commit)*
  — ~100x cheaper, and correct *for OKLab*.

`deltaE2000` only accepts `Lab`, so it is structurally impossible to apply it to OKLab.

## Conformance

`deltaE2000` is verified against the published 34-pair Sharma reference table
(`DeltaE2000SharmaTest`), the accepted proof of a correct CIEDE2000 implementation.

## Status

This commit lands the proven anchor only: `Lab` + `deltaE2000` + the Sharma conformance test.
Next: `Srgb` / `LinearRgb` / `OkLab` + conversions (one set of matrices), `deltaSqOk` for hot loops,
`mixThreads` (blend in linear light), and SoA `*Planes`. Consumers (S7, catalog, metrics) migrate
onto this module afterwards.
