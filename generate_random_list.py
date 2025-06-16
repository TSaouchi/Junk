import random
from typing import List, Tuple, Iterator

class RandomSubsetGenerator:
    def __init__(self, data: Iterator[Tuple], seed: int = None):
        self.data = list(data)
        self.total_size = len(self.data)
        self.random = random.Random(seed)  # Reproducibility

    def generate_subsets(self, n: int) -> List[List[Tuple]]:
        available_indices = list(range(self.total_size))
        self.random.shuffle(available_indices)

        subsets = [[] for _ in range(n)]
        current = 0

        # Randomly assign each item to one of the n subsets
        for idx in available_indices:
            subsets[self.random.randint(0, n - 1)].append(self.data[idx])

        return subsets

list1 = ['A1', 'A2', 'A3', 'A4', 'A5']
list2 = ['B1', 'B2', 'B3', 'B4', 'B5']
list3 = ['C1', 'C2', 'C3', 'C4', 'C5']

data = zip(list1, list2, list3)
generator = RandomSubsetGenerator(data, seed=42)

subsets = generator.generate_subsets(3)

for i, subset in enumerate(subsets):
    print(f"\nSubset {i+1}:")
    for item in subset:
        print(item)
