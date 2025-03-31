import time
import asyncio
import logging
from functools import wraps

# Configure logging
logging.basicConfig(level=logging.INFO)

class Retry:
    def __init__(self, retries=3, delay=1, backoff=2, max_wait=10, exceptions=(Exception,), invalid_results=None):
        """
        A decorator to retry a function execution on failure.

        :param retries: Max number of retries before failing.
        :param delay: Initial delay before retrying.
        :param backoff: Multiplier for delay (exponential backoff).
        :param max_wait: Maximum delay before retrying (prevents excessive backoff).
        :param exceptions: Tuple of exception types to catch and retry.
        :param invalid_results: List of specific return values that trigger a retry.
        """
        self.retries = retries
        self.delay = delay
        self.backoff = backoff
        self.max_wait = max_wait
        self.exceptions = exceptions
        self.invalid_results = invalid_results if invalid_results is not None else []

    def __call__(self, func):
        if asyncio.iscoroutinefunction(func):
            return self._async_wrapper(func)
        else:
            return self._sync_wrapper(func)

    def _sync_wrapper(self, func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            attempt = 0
            wait_time = self.delay

            while attempt < self.retries:
                try:
                    result = func(*args, **kwargs)

                    if result in self.invalid_results:
                        raise ValueError(f"Invalid return value: {result}")

                    return result  # Success case

                except self.exceptions as e:
                    logging.warning(f"Attempt {attempt + 1} failed due to: {e}. Retrying in {wait_time} sec...")
                    time.sleep(min(wait_time, self.max_wait))  # Ensure max wait time is respected
                    wait_time = min(wait_time * self.backoff, self.max_wait)  # Increase exponentially
                    attempt += 1

            logging.error(f"Function {func.__name__} failed after {self.retries} retries.")
            raise RuntimeError(f"Function {func.__name__} failed after {self.retries} retries.")

        return wrapper

    def _async_wrapper(self, func):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            attempt = 0
            wait_time = self.delay

            while attempt < self.retries:
                try:
                    result = await func(*args, **kwargs)

                    if result in self.invalid_results:
                        raise ValueError(f"Invalid return value: {result}")

                    return result  # Success case

                except self.exceptions as e:
                    logging.warning(f"Attempt {attempt + 1} failed due to: {e}. Retrying in {wait_time} sec...")
                    await asyncio.sleep(min(wait_time, self.max_wait))  # Ensure max wait time is respected
                    wait_time = min(wait_time * self.backoff, self.max_wait)  # Increase exponentially
                    attempt += 1

            logging.error(f"Async function {func.__name__} failed after {self.retries} retries.")
            raise RuntimeError(f"Async function {func.__name__} failed after {self.retries} retries.")

        return wrapper
