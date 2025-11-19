# Decision / Gate Engine v1 (G0–G3)

Гейты работают последовательно и на выходе формируют базовый `ProcessingPlan` без выбора палитры.

```
S2 AnalyzeResult + DecisionInput
       |
      G0 — расчёт целевого размера стежков (физика или авто) + округление/кламп
       |
      G1 — классификация сцены: PHOTO / PIXEL_ART / DISCRETE
       |
      G2 — оценка сложности: SIMPLE / MEDIUM / COMPLEX
       |
      G3 — выбор ветки пайплайна: PIXEL_PIPE / PHOTO_PIPE / DISCRETE_PIPE
       |
   ProcessingPlan (reasons + GateSnapshot)
```

## Примеры reasons
- `G0 size: 160x80 stitches (via physical, clamp=80..300, pick=physical 10.000in * 16.000, roundTo=2)`
- `G1 pixelation high (pixelation=0.700 >= 0.550)`
- `G2 complexity: edge=0.050, entropy=0.400 -> MEDIUM`
- `G3 pipeline: forcePixel=false, scene=PIXEL_ART, pixel=true -> PIXEL_PIPE`

Все пороги и диапазоны собраны в `DecisionParams` (версия профиля: `versionTag`).
