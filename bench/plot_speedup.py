"""Recalcula as médias HTTP e gera o gráfico a partir das medições brutas."""

import argparse
import csv
import shutil
from collections import defaultdict
from decimal import Decimal, InvalidOperation
from pathlib import Path


FIELDS = ["size", "mode", "threads", "avgResponseTimeMs", "speedup"]
SCRIPT_DIR = Path(__file__).resolve().parent


def aggregate_measurements(rows: list[dict[str, str]]) -> list[dict[str, str]]:
    """Valida a coleta e calcula T1/Tt usando a média de responseTimeMs."""
    if not rows:
        raise ValueError("O CSV bruto não contém medições.")

    times = defaultdict(list)
    runs = defaultdict(set)
    counts = {}
    for line, row in enumerate(rows, start=2):
        try:
            size = int(row["size"])
            threads = int(row["threads"])
            run = int(row["run"])
            count = int(row["duplicatesFound"])
            response_time = Decimal(row["responseTimeMs"])
            mode = row["mode"]
            status = row["status"]
        except (KeyError, TypeError, ValueError, InvalidOperation) as error:
            raise ValueError(f"Linha {line}: campos obrigatórios inválidos.") from error
        if (
            min(size, threads, run) <= 0
            or count < 0
            or mode not in {"platform", "virtual"}
            or status != "SUCCESS"
            or not response_time.is_finite()
            or response_time <= 0
        ):
            raise ValueError(f"Linha {line}: medição inválida ou sem SUCCESS.")
        if size in counts and counts[size] != count:
            raise ValueError(f"n={size}: contagens de duplicatas divergentes.")
        counts[size] = count
        key = (size, mode, threads)
        if run in runs[key]:
            raise ValueError(f"Linha {line}: repetição duplicada para {key}.")
        runs[key].add(run)
        times[key].append(response_time)

    means = {}
    for key, values in times.items():
        if runs[key] != set(range(1, len(values) + 1)):
            raise ValueError(f"{key}: sequência de repetições incompleta.")
        means[key] = sum(values) / len(values)

    result = []
    for (size, mode, threads), mean in sorted(means.items()):
        baseline_key = (size, "platform", 1)
        if baseline_key not in means:
            raise ValueError(f"n={size}: falta a medição sequencial (platform, 1).")
        if len(times[(size, mode, threads)]) != len(times[baseline_key]):
            raise ValueError(f"n={size}: número de repetições diferente da base.")
        result.append({
            "size": str(size),
            "mode": mode,
            "threads": str(threads),
            "avgResponseTimeMs": f"{mean:.2f}",
            "speedup": f"{means[baseline_key] / mean:.6f}",
        })
    return result


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Confere o agregado sem alterar arquivos.")
    args = parser.parse_args()
    try:
        rows = aggregate_measurements(read_csv(SCRIPT_DIR / "measurements.csv"))
        agg_csv = SCRIPT_DIR / "measurements_agg.csv"
        if args.check:
            if read_csv(agg_csv) != rows:
                raise ValueError("O agregado diverge dos dados brutos. Execute sem --check para regerá-lo.")
            print(f"Agregado correto: {len(rows)} configurações.")
            return
    except (OSError, ValueError) as error:
        parser.exit(1, f"Erro: {error}\n")

    import matplotlib

    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    with agg_csv.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=FIELDS, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)

    platform = defaultdict(dict)
    for row in rows:
        if row["mode"] == "platform":
            platform[int(row["size"])][int(row["threads"])] = row

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 5))
    for size, configurations in sorted(platform.items()):
        threads = sorted(configurations)
        ax1.plot(threads, [float(configurations[t]["avgResponseTimeMs"]) for t in threads], marker="o", label=f"n={size}")
        ax2.plot(threads, [float(configurations[t]["speedup"]) for t in threads], marker="o", label=f"n={size}")
    ax1.set_ylabel("Tempo médio de resposta HTTP (ms)")
    ax1.set_title("Tempo vs threads")
    ax2.plot([1, 2, 4, 8], [1, 2, 4, 8], linestyle="--", color="gray", label="ideal (linear)")
    ax2.set_ylabel("Speedup (T1 / Tt)")
    ax2.set_title("Speedup vs threads")
    for axis in (ax1, ax2):
        axis.set_xlabel("Threads")
        axis.set_xticks([1, 2, 4, 8])
        axis.legend()
        axis.grid(True, alpha=0.3)
    fig.tight_layout()
    image_path = SCRIPT_DIR / "speedup.png"
    fig.savefig(image_path, dpi=150)
    plt.close(fig)
    docs_image = SCRIPT_DIR.parent / "docs" / "speedup.png"
    shutil.copyfile(image_path, docs_image)
    print(f"Agregado salvo em {agg_csv}\nGráfico salvo em {image_path}\nCópia salva em {docs_image}")
    print("\n| Tamanho (n) | Threads | Tempo Médio (ms) | Speedup |")
    print("|---|---|---|---|")
    for size, configurations in sorted(platform.items()):
        for threads, row in sorted(configurations.items()):
            print(f"| {size} | {threads} | {row['avgResponseTimeMs']} | {float(row['speedup']):.2f}x |")


if __name__ == "__main__":
    main()
