from abc import ABC, abstractmethod

# 1️⃣ Interface: Defines the contract for all strategies
class Strategy(ABC):
    @staticmethod
    @abstractmethod
    def execute(data):
        """Abstract method that all concrete strategies must implement"""
        pass

# 2️⃣ Concrete Strategies: Implementing different behaviors
class StrategyA(Strategy):
    @staticmethod
    def execute(data):
        return f"Processed {data} using Strategy A"

class StrategyB(Strategy):
    @staticmethod
    def execute(data):
        return f"Processed {data} using Strategy B"

# 3️⃣ Manager: Uses strategies dynamically
class StrategyManager:
    # Hardcoded strategies - no registration required
    strategies = {
        "A": StrategyA,
        "B": StrategyB
    }

    def __init__(self, strategy_name):
        if strategy_name not in self.strategies:
            raise ValueError(f"Strategy '{strategy_name}' not found")
        self.strategy = self.strategies[strategy_name]()

    def process(self, data):
        return self.strategy.execute(data)

# 🎯 Example Usage
manager = StrategyManager("A")
print(manager.process("Data"))  # Output: Processed Data using Strategy A

manager = StrategyManager("B")
print(manager.process("Data"))  # Output: Processed Data using Strategy B
