import threading

class SingletonMeta(type):
    _instances = {}
    _lock = threading.Lock()

    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            with cls._lock:
                if cls not in cls._instances:
                    instance = super().__call__(*args, **kwargs)
                    cls._instances[cls] = instance
        return cls._instances[cls]

    def reset_instance(cls):
        """Reset the singleton instance (for testing or reinitialization)"""
        with cls._lock:
            if cls in cls._instances:
                del cls._instances[cls]

    
class Singleton(metaclass=SingletonMeta):
    def __init__(self, value=None):
        self.value = value

    def __str__(self):
        return f"Singleton with value: {self.value}"

# a = Singleton("first")
# b = Singleton("second")

# print(a)  # Singleton with value: first
# print(b)  # Singleton with value: first
# print(a is b)  # True: same instance


s1 = Singleton("first")
print(s1)  # Singleton with value: first

Singleton.reset_instance()

s2 = Singleton("second")
print(s2)  # Singleton with value: second
print(s1 is s2)  # False: after reset, new instance


class Singleton:
    _instance = None
    _lock = threading.Lock()  # Thread-safe instantiation

    def __new__(cls, *args, **kwargs):
        if cls._instance is None:
            with cls._lock:  # Lock for thread safety
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self, value=None):
        # This will be called every time, so guard if needed
        if not hasattr(self, "_initialized"):
            self.value = value
            self._initialized = True

    def __str__(self):
        return f"Singleton with value: {self.value}"
    
