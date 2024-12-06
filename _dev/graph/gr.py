

import csv
from matplotlib import pyplot as plt

opens: list[tuple[str, str]] = [
    ['rhino.txt','r'],
    ['rhino_2nd_run.txt','orange'],

    # 'rhizo.txt',

    # ['rhizo_compile.txt','g'],
    # ['rhizo_compile_2nd_run.txt','blue']
    ['rhizo3.4.txt','g'],
    ['rhizo3.4 2nd run.txt','blue']
]

legends = []

def collect(fineName: str):
    lines: list[str] = []
    with open(name) as f:
        lines = f.readlines()
    lines = [line.strip() for line in lines]
    lines = [line for line in lines if len(line) != 0 and not line.startswith('#')]
    return lines

def removeSuffix(fileName: str):
    try:
        i = fileName.rindex('.')
        return fileName[:i]
    except:
        return fileName

for name, fmt in opens:
    lines = collect(name)
    reader = csv.DictReader(lines)
    duras = [int(l['duration']) for l in reader]
    plt.plot(range(len(duras)), duras, fmt)
    legends.append(f'{removeSuffix(name)}: {sum(duras) / len(duras)}ms')
    pass

plt.legend(legends)
plt.axis((-5, 165, 100, 600))
plt.xlabel('N-th test')
plt.ylabel('execution time (ms)')
plt.show()

legends.clear()
SLICE = [90, 160]
for name, fmt in opens:
    lines = collect(name)
    reader = csv.DictReader(lines)
    duras = [int(l['duration']) for l in reader]
    duras = duras[SLICE[0]:SLICE[1]+1]
    plt.plot(range(len(duras)), duras, fmt)
    avgDuration = sum(duras) / len(duras)
    legends.append(f'{removeSuffix(name)}: {avgDuration:.4}ms')


plt.legend(legends)
plt.axis((-5, SLICE[1]-SLICE[0]+5, 100, 400))
plt.xlabel('N-th test')
plt.ylabel('execution time (ms)')
plt.show()
