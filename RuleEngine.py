from abc import ABC, abstractmethod

# Rule Interface
class Rule(ABC):
    @abstractmethod
    def apply(self, data: dict) -> dict:
        """
        Apply the rule to the dictionary and return the modified dictionary.
        """
        pass

# Specific Rules
class BlacklistRule(Rule):
    def __init__(self, blacklist_keys: list):
        self.blacklist_keys = blacklist_keys

    def apply(self, data: dict) -> dict:
        return {k: v for k, v in data.items() if k not in self.blacklist_keys}

class KeyOverrideRule(Rule):
    def __init__(self, key_mapping: dict):
        self.key_mapping = key_mapping

    def apply(self, data: dict) -> dict:
        return {self.key_mapping.get(k, k): v for k, v in data.items()}

class WhitelistRule(Rule):
    def __init__(self, whitelist_keys: list):
        self.whitelist_keys = whitelist_keys

    def apply(self, data: dict) -> dict:
        return {k: v for k, v in data.items() if k in self.whitelist_keys}

class TransformationRule(Rule):
    def __init__(self, transformation: callable):
        self.transformation = transformation

    def apply(self, data: dict) -> dict:
        return self.transformation(data)

# Rule Engine
class RuleEngine:
    def __init__(self):
        self.rules = []

    def add_rule(self, rule: Rule):
        """
        Add a rule to the engine.
        """
        self.rules.append(rule)

    def apply_rules(self, data: dict) -> dict:
        """
        Apply all rules in sequence to the given dictionary.
        """
        for rule in self.rules:
            data = rule.apply(data)
        return data

# Example Usage
if __name__ == "__main__":
    # Define rules
    blacklist_rule = BlacklistRule(blacklist_keys=["password", "secret"])
    key_override_rule = KeyOverrideRule(key_mapping={"userId": "user_id", "amount": "transaction_amount"})
    whitelist_rule = WhitelistRule(whitelist_keys=["user_id", "transaction_amount", "note"])
    custom_rule = TransformationRule(lambda d: {k.upper(): v for k, v in d.items()})

    # Create the rule engine and add rules
    engine = RuleEngine()
    engine.add_rule(blacklist_rule)
    engine.add_rule(key_override_rule)
    engine.add_rule(whitelist_rule)
    engine.add_rule(custom_rule)

    # Apply rules to a sample dictionary
    request_data = {
        "userId": 123,
        "password": "hidden",
        "amount": 100.50,
        "secret": "xyz",
        "note": "Sample transaction"
    }

    result = engine.apply_rules(request_data)
    print(result)
    # Output: {'USER_ID': 123, 'TRANSACTION_AMOUNT': 100.5, 'NOTE': 'Sample transaction'}
