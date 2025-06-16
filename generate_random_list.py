import random
from typing import Iterator, Tuple, List, Optional
from itertools import combinations, chain

class RandomSubsetGenerator:
    def __init__(self, data: Iterator[Tuple], seed: Optional[int] = None):
        self.records = list(data)
        self.size = len(self.records)
        self.random = random.Random(seed)

    def _all_non_empty_subsets(self) -> List[List[Tuple]]:
        """
        Return all non-empty subsets of self.records.
        """
        return list(
            chain.from_iterable(combinations(self.records, r) for r in range(1, self.size + 1))
        )

    def generate_subsets(self, n: Optional[int] = None, alpha: float = 0.5, beta: float = 0.5) -> List[List[Tuple]]:
        if n is None:
            all_subsets = self._all_non_empty_subsets()
            self.random.shuffle(all_subsets)
            return [list(sub) for sub in all_subsets]

        # Else, generate `n` random subsets with beta-distributed size
        subsets = []
        for _ in range(n):
            subset_fraction = self.random.betavariate(alpha, beta)
            subset_size = max(1, min(self.size, int(subset_fraction * self.size)))
            subset = self.random.sample(self.records, k=subset_size)
            subsets.append(subset)

        return subsets
list1 = ['A1', 'A2', 'A3', 'A4', 'A5']
list2 = ['B1', 'B2', 'B3', 'B4', 'B5']
list3 = ['C1', 'C2', 'C3', 'C4', 'C5']

data = zip(list1, list2, list3)
generator = RandomSubsetGenerator(data, seed=123)

# Without N → all possible non-empty subsets
subsets = generator.generate_subsets(alpha=0.89, beta=0.88)

print(f"Generated {len(subsets)} total subsets.")
for i, subset in enumerate(subsets):
    print(f"\nSubset {i+1} ({len(subset)} items): {subset}")

list1, list2, list3 = zip(*subsets[0])
list1 = list(list1)
list2 = list(list2)
list3 = list(list3)
