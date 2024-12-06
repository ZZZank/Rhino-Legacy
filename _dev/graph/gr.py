

import csv
from matplotlib import pyplot as plt

opens: list[tuple[str, str]] = [
    ['rhino.txt','r'],
    ['rhino_2nd_run.txt','orange'],
    # 'rhizo.txt',
    ['rhizo_compile.txt','g'],
    ['rhizo_compile_2nd_run.txt','blue'],
    ['rhizo3.4.txt','y']
]

legends = []

for name, fmt in opens:
    lines: list[str] = []
    with open(name) as f:
        lines = [line for line in f.readlines() if not line.startswith('#')]
    reader = csv.DictReader(lines)
    duras = [int(l['duration']) for l in reader]
    plt.plot(range(len(duras)), duras, fmt)
    fileName = '.'.join(name.split('.')[:-1])
    legends.append(f'{fileName}: {sum(duras) / len(duras)}ms')
    pass

plt.legend(legends)
plt.axis((-5, 165, 100, 600))
plt.xlabel('N-th test')
plt.ylabel('execution time (ms)')
plt.show()

legends.clear()
SLICE = [90, 160]
for name, fmt in opens:
    with open(name) as f:
        reader = csv.DictReader(f)
        duras = [int(l['duration']) for l in reader]
        duras = duras[SLICE[0]:SLICE[1]+1]
        plt.plot(range(len(duras)), duras, fmt)
        avgDuration = sum(duras) / len(duras)
        legends.append(f'{name.split(".", 1)[0]}: {avgDuration:.4}ms')
        pass

plt.legend(legends)
plt.axis((-5, SLICE[1]-SLICE[0]+5, 100, 400))
plt.xlabel('N-th test')
plt.ylabel('execution time (ms)')
plt.show()
