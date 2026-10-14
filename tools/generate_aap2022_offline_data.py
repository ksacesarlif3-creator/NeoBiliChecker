#!/usr/bin/env python3
"""Generate exact hourly AAP 2022 bilirubin thresholds for offline Android use.

Source: AAP 2022 Hyperbilirubinemia Guideline supplemental tables 1-4.
The generated Kotlin file contains values for postnatal ages 1..336 hours.
"""

from __future__ import annotations

import base64
import gzip
import re
import subprocess
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/java/com/neobili/checker/OfflineThresholdData.kt"
WORK = ROOT / "build/offline-threshold-generator"
PDF = WORK / "aap2022-supplement.pdf"
TXT = WORK / "aap2022-supplement.txt"

SOURCES = [
    "https://aap2.silverchair-cdn.com/aap2/content_public/journal/pediatrics/150/3/10.1542_peds.2022-058859/5/peds_2022058859supplementarydata.pdf",
    "https://pqcnc-documents.s3.amazonaws.com/lpi/PQCNCCareoftheLatePretermInfantWebResourcesAAPClinPracHypebili2022.pdf",
]

EXPECTED_GA = {
    1: [35, 36, 37, 38, 39, 40],
    2: [35, 36, 37, 38],
    3: [35, 36, 37, 38],
    4: [35, 36, 37, 38],
}


def download() -> None:
    WORK.mkdir(parents=True, exist_ok=True)
    last_error: Exception | None = None
    for url in SOURCES:
        try:
            req = urllib.request.Request(
                url,
                headers={"User-Agent": "NeoBiliChecker offline-data generator; AAP 2022 guideline validation"},
            )
            with urllib.request.urlopen(req, timeout=45) as response:
                payload = response.read()
            if not payload.startswith(b"%PDF"):
                raise RuntimeError("downloaded content is not a PDF")
            PDF.write_bytes(payload)
            return
        except Exception as exc:
            last_error = exc
    raise RuntimeError(f"Unable to download AAP supplemental PDF: {last_error}")


def extract_text() -> None:
    try:
        subprocess.run(
            ["pdftotext", "-layout", str(PDF), str(TXT)],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
    except FileNotFoundError as exc:
        raise RuntimeError("pdftotext is required (install poppler-utils)") from exc


def parse_tables() -> dict[tuple[int, int], dict[int, float]]:
    text = TXT.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    curves: dict[tuple[int, int], dict[int, float]] = {}
    current_table: int | None = None
    current_ga: int | None = None

    for line in lines:
        table_match = re.search(r"SUPPLEMENTAL TABLE\s+([1-4])\b", line)
        if table_match:
            current_table = int(table_match.group(1))
            current_ga = None
            continue
        if current_table is None:
            continue

        ga_match = re.search(r"Gestational age of\s+(35|36|37|38|39|40)\b", line)
        if ga_match:
            current_ga = int(ga_match.group(1))
            curves.setdefault((current_table, current_ga), {})
            continue
        if current_ga is None:
            continue

        stripped = line.strip()
        day_match = re.match(r"^(\d{1,2})\s+", stripped)
        if not day_match:
            continue
        day = int(day_match.group(1))
        if day < 0 or day > 14:
            continue

        remainder = stripped[day_match.end():]
        values = [float(x) for x in re.findall(r"\d+\.\d+", remainder)]
        if len(values) not in (23, 24):
            continue

        # Day 0 omits age 0 h, so its 23 values correspond to ages 1..23.
        first_hour = 1 if day == 0 and len(values) == 23 else 0
        for offset, value in enumerate(values):
            age = day * 24 + first_hour + offset
            if 1 <= age <= 336:
                curves[(current_table, current_ga)][age] = value

    return curves


def normalize(curves: dict[tuple[int, int], dict[int, float]]) -> dict[tuple[int, int], list[float]]:
    normalized: dict[tuple[int, int], list[float]] = {}
    for table, gas in EXPECTED_GA.items():
        for ga in gas:
            key = (table, ga)
            if key not in curves:
                raise RuntimeError(f"Missing table {table}, GA {ga}")
            points = curves[key]
            if 1 not in points:
                raise RuntimeError(f"Table {table}, GA {ga} is missing age 1 h")
            max_age = max(points)
            missing_internal = [age for age in range(1, max_age + 1) if age not in points]
            if missing_internal:
                raise RuntimeError(f"Table {table}, GA {ga} has internal missing ages: {missing_internal[:12]}")
            last = points[max_age]
            normalized[key] = [points.get(age, last) for age in range(1, 337)]

    # Supplemental Tables 2-4 use a single >=38-week curve.
    for table in (2, 3, 4):
        for ga in (39, 40):
            normalized[(table, ga)] = list(normalized[(table, 38)])

    for ga in range(35, 41):
        for risk in (False, True):
            p_table = 2 if risk else 1
            e_table = 4 if risk else 3
            photo = normalized[(p_table, ga)]
            exchange = normalized[(e_table, ga)]
            if len(photo) != 336 or len(exchange) != 336:
                raise RuntimeError("Each curve must contain exactly 336 hourly values")
            if any(photo[i] > photo[i + 1] for i in range(335)):
                raise RuntimeError(f"Phototherapy curve is non-monotonic: GA {ga}, risk={risk}")
            if any(exchange[i] > exchange[i + 1] for i in range(335)):
                raise RuntimeError(f"Exchange curve is non-monotonic: GA {ga}, risk={risk}")
            if any(p >= e for p, e in zip(photo, exchange)):
                raise RuntimeError(f"Phototherapy must remain below exchange: GA {ga}, risk={risk}")
    return normalized


def curve_string(values: list[float]) -> str:
    return ",".join(f"{value:.1f}" for value in values)


def write_kotlin(data: dict[tuple[int, int], list[float]]) -> None:
    rows: list[str] = []
    for ga in range(35, 41):
        rows.append(f"P:N:{ga}={curve_string(data[(1, ga)])}")
        rows.append(f"P:R:{ga}={curve_string(data[(2, ga)])}")
        rows.append(f"E:N:{ga}={curve_string(data[(3, ga)])}")
        rows.append(f"E:R:{ga}={curve_string(data[(4, ga)])}")
    plain = ("\n".join(rows) + "\n").encode("utf-8")
    payload = base64.b64encode(gzip.compress(plain, compresslevel=9, mtime=0)).decode("ascii")

    kotlin = f'''package com.neobili.checker

import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.zip.GZIPInputStream

/**
 * Exact hour-specific AAP 2022 hyperbilirubinemia thresholds (ages 1..336 h).
 * The validated table is compressed only to keep source compact; all values are
 * bundled inside the APK and are decoded locally. No network access is used.
 */
internal object OfflineThresholdData {{
    private const val PAYLOAD = "{payload}"

    private val parsed: Map<String, DoubleArray> by lazy {{
        val compressed = Base64.getDecoder().decode(PAYLOAD)
        val text = GZIPInputStream(ByteArrayInputStream(compressed)).bufferedReader().use {{ it.readText() }}
        text.lineSequence()
            .filter {{ it.isNotBlank() }}
            .associate {{ line ->
                val split = line.indexOf('=')
                require(split > 0) {{ "Invalid bundled threshold row" }}
                val key = line.substring(0, split)
                val values = line.substring(split + 1).split(',').map(String::toDouble).toDoubleArray()
                require(values.size == 336) {{ "Invalid bundled threshold curve: $key" }}
                key to values
            }}
    }}

    fun value(kind: Char, ga: Int, ageHours: Int, hasAdditionalRisk: Boolean): Double {{
        require(kind == 'P' || kind == 'E') {{ "Threshold kind must be P or E" }}
        require(ga in 35..40) {{ "GA must be 35..40 weeks" }}
        require(ageHours in 1..336) {{ "Age must be 1..336 hours" }}
        val risk = if (hasAdditionalRisk) "R" else "N"
        return parsed.getValue("$kind:$risk:$ga")[ageHours - 1]
    }}
}}
'''
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(kotlin, encoding="utf-8")


def main() -> int:
    download()
    extract_text()
    normalized = normalize(parse_tables())
    write_kotlin(normalized)
    print(f"Generated {OUT}")
    for sample in [(1, 40, 24), (3, 40, 24), (2, 40, 48), (4, 40, 48), (1, 35, 72)]:
        table, ga, age = sample
        print(f"table={table} ga={ga} age={age}: {normalized[(table, ga)][age - 1]:.1f}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
