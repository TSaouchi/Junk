import asyncio
import threading
from collections.abc import Coroutine

class AsyncRunner:
    def __init__(self):
        self.loop = asyncio.new_event_loop()
        self.thread = threading.Thread(target=self.loop.run_forever, daemon=True)
        self.thread.start()

    def run(self, coro_or_list):
        # If a list/tuple of coroutines, wrap with gather
        if isinstance(coro_or_list, (list, tuple)) and all(isinstance(c, Coroutine) for c in coro_or_list):
            async def gather_wrapper():
                return await asyncio.gather(*coro_or_list)
            coro = gather_wrapper()
        elif isinstance(coro_or_list, Coroutine):
            coro = coro_or_list
        else:
            raise TypeError("run() accepts a coroutine or a list/tuple of coroutines")

        return asyncio.run_coroutine_threadsafe(coro, self.loop).result()

    def stop(self):
        self.loop.call_soon_threadsafe(self.loop.stop)
        self.thread.join()
