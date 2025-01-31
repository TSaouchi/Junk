from abc import ABC, abstractmethod

# 1️⃣ Interface: Defines the contract for all strategies
class Strategy(ABC):
    @staticmethod
    @abstractmethod
    def execute(data):
        """Abstract method that all concrete strategies must implement"""
        pass

# 2️⃣ Registry: Manages available strategies
class StrategyRegistry:
    _registry = {}
    _instance = None

    def __new__(cls, *args, **kwargs):
        if cls._instance is None:
            cls._instance = super().__new__(cls, *args, **kwargs)
        return cls._instance

    @classmethod
    def register(cls, name):
        """Decorator to register a strategy"""
        def decorator(strategy_cls):
            if not issubclass(strategy_cls, Strategy):
                raise TypeError(f"{strategy_cls} must be a subclass of Strategy")
            cls._registry[name] = strategy_cls
            return strategy_cls  # Return the class unchanged
        return decorator

    @classmethod
    def create(cls, name):
        """Factory method to retrieve and instantiate a strategy"""
        if name not in cls._registry:
            raise ValueError(f"Strategy '{name}' not found")
        return cls._registry[name]()  # Instantiate and return

# 3️⃣ Concrete Strategies: Different implementations, auto-registered
@StrategyRegistry.register("A")
class StrategyA(Strategy):
    @staticmethod
    def execute(data):
        return f"Processed {data} using Strategy A"

@StrategyRegistry.register("B")
class StrategyB(Strategy):
    @staticmethod
    def execute(data):
        return f"Processed {data} using Strategy B"

# 4️⃣ Manager: Uses strategies dynamically
class StrategyManager:
    def __init__(self, strategy_name):
        self.strategy = StrategyRegistry.create(strategy_name)

    def process(self, data):
        return self.strategy.execute(data)

# 🎯 Example Usage
manager = StrategyManager("A")
print(manager.process("Data"))  # Output: Processed Data using Strategy A

manager = StrategyManager("B")
print(manager.process("Data"))  # Output: Processed Data using Strategy B
