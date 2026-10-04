import csv
import matplotlib
import os
matplotlib.use("Agg")
import matplotlib.pyplot as plt

script_dir = os.path.dirname(os.path.abspath(__file__))
agg_csv = os.path.join(script_dir, "measurements_agg.csv")
out_img = os.path.join(script_dir, "speedup.png")

rows = []
with open(agg_csv, encoding='utf-8-sig') as f:
    for r in csv.DictReader(f):
        rows.append(r)

sizes = sorted({int(r["size"]) for r in rows})
platform = {s: {} for s in sizes}
speedup_map = {s: {} for s in sizes}

for r in rows:
    if r["mode"] == "platform":
        s = int(r["size"])
        t = int(r["threads"])
        platform[s][t] = float(r["avgResponseTimeMs"])
        speedup_map[s][t] = float(r["speedup"])

fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 5))

for s in sizes:
    threads = sorted(platform[s].keys())
    times = [platform[s][t] for t in threads]
    ax1.plot(threads, times, marker="o", label=f"n={s}")

ax1.set_xlabel("Threads")
ax1.set_ylabel("Tempo médio de resposta HTTP (ms)")
ax1.set_title("Tempo vs threads")
ax1.set_xticks([1, 2, 4, 8])
ax1.legend()
ax1.grid(True, alpha=0.3)

for s in sizes:
    threads = sorted(speedup_map[s].keys())
    sp = [speedup_map[s][t] for t in threads]
    ax2.plot(threads, sp, marker="o", label=f"n={s}")

ax2.plot([1, 2, 4, 8], [1, 2, 4, 8], linestyle="--", color="gray", label="ideal (linear)")
ax2.set_xlabel("Threads")
ax2.set_ylabel("Speedup (T1 / Tn)")
ax2.set_title("Speedup vs threads")
ax2.set_xticks([1, 2, 4, 8])
ax2.legend()
ax2.grid(True, alpha=0.3)

fig.tight_layout()
fig.savefig(out_img, dpi=150)
print(f"Gráfico salvo em {out_img}")

print("\n| Tamanho (n) | Threads | Tempo Médio (ms) | Speedup |")
print("|---|---|---|---|")
for s in sizes:
    for t in sorted(platform[s].keys()):
        tm = platform[s][t]
        sp = speedup_map[s][t]
        print(f"| {s} | {t} | {tm:.2f} | {sp:.2f}x |")
