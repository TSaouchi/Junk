import pytest
import asyncio
import time
from retry_decorator import Retry  # Ensure your Retry class is in a module named retry_decorator

# Sample synchronous function that fails a few times before succeeding
def flaky_function(counter=[0]):
    if counter[0] < 2:
        counter[0] += 1
        raise ValueError("Temporary failure")
    return "Success"

# Sample synchronous function that returns an invalid value
def invalid_return_function():
    return None

# Sample asynchronous function that fails a few times before succeeding
async def async_flaky_function(counter=[0]):
    if counter[0] < 2:
        counter[0] += 1
        raise ValueError("Temporary failure")
    return "Success"

# Sample asynchronous function that returns an invalid value
async def async_invalid_return_function():
    return None

# Test case for retrying on exceptions
def test_retry_on_exception():
    retry_decorator = Retry(retries=3, delay=0.1, exceptions=(ValueError,))
    wrapped_function = retry_decorator(flaky_function)
    result = wrapped_function()
    assert result == "Success"

# Test case for retrying on invalid return values
def test_retry_on_invalid_return():
    retry_decorator = Retry(retries=3, delay=0.1, invalid_results=[None])
    wrapped_function = retry_decorator(invalid_return_function)
    with pytest.raises(RuntimeError):
        wrapped_function()

# Test case for retrying an async function on exceptions
@pytest.mark.asyncio
async def test_async_retry_on_exception():
    retry_decorator = Retry(retries=3, delay=0.1, exceptions=(ValueError,))
    wrapped_function = retry_decorator(async_flaky_function)
    result = await wrapped_function()
    assert result == "Success"

# Test case for retrying an async function on invalid return values
@pytest.mark.asyncio
async def test_async_retry_on_invalid_return():
    retry_decorator = Retry(retries=3, delay=0.1, invalid_results=[None])
    wrapped_function = retry_decorator(async_invalid_return_function)
    with pytest.raises(RuntimeError):
        await wrapped_function()

# Test case to ensure it fails after max retries
def test_max_retries():
    retry_decorator = Retry(retries=2, delay=0.1, exceptions=(ValueError,))
    wrapped_function = retry_decorator(lambda: (_ for _ in ()).throw(ValueError("Always failing")))
    with pytest.raises(RuntimeError):
        wrapped_function()

# Test case for max retries on async function
@pytest.mark.asyncio
async def test_async_max_retries():
    retry_decorator = Retry(retries=2, delay=0.1, exceptions=(ValueError,))
    async def always_failing():
        raise ValueError("Always failing")
    wrapped_function = retry_decorator(always_failing)
    with pytest.raises(RuntimeError):
        await wrapped_function()
