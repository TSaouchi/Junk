import random
from typing import Iterator, Tuple, List

class RandomSubsetGenerator:
    def __init__(self, data: Iterator[Tuple], seed: int = None):
        self.records = list(data)
        self.size = len(self.records)
        self.random = random.Random(seed)

    def generate_subsets(self, n: int) -> List[List[Tuple]]:
        subsets = []

        for _ in range(n):
            subset_size = self.random.randint(0, self.size)
            # Sample without replacement inside the same subset
            subset = self.random.sample(self.records, k=subset_size)
            subsets.append(subset)

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
