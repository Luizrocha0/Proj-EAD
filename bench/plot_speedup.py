import csv
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

rows = []
with open("bench/measurements.csv") as f:
    for r in csv.DictReader(f):
        rows.append(r)

sizes = sorted({int(r["size"]) for r in rows})
platform = {s: {} for s in sizes}
for r in rows:
    if r["mode"] == "platform":
        platform[int(r["size"])][int(r["threads"])] = int(r["processingTimeMs"])

fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 5))

for s in sizes:
    threads = sorted(platform[s].keys())
    times = [platform[s][t] for t in threads]
    ax1.plot(threads, times, marker="o", label=f"n={s}")

ax1.set_xlabel("Threads")
ax1.set_ylabel("Tempo de processamento (ms)")
ax1.set_title("Tempo vs threads (por tamanho de entrada)")
ax1.set_xticks([1, 2, 4, 8])
ax1.legend()
ax1.grid(True, alpha=0.3)

for s in sizes:
    threads = sorted(platform[s].keys())
    base = platform[s][1]
    speedup = [base / platform[s][t] for t in threads]
    ax2.plot(threads, speedup, marker="o", label=f"n={s}")

ax2.plot([1, 2, 4, 8], [1, 2, 4, 8], linestyle="--", color="gray", label="speedup ideal (linear)")
ax2.set_xlabel("Threads")
ax2.set_ylabel("Speedup (T1 / Tn)")
ax2.set_title("Speedup vs threads")
ax2.set_xticks([1, 2, 4, 8])
ax2.legend()
ax2.grid(True, alpha=0.3)

fig.tight_layout()
fig.savefig("bench/speedup.png", dpi=150)
print("saved bench/speedup.png")

print("\n| n | threads | tempo(ms) | speedup |")
print("|---|---------|-----------|---------|")
for s in sizes:
    base = platform[s][1]
    for t in sorted(platform[s].keys()):
        tm = platform[s][t]
        print(f"| {s} | {t} | {tm} | {base/tm:.2f}x |")
