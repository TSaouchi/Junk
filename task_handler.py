import threading
import queue
import asyncio
import time

class OrderedTaskWorker:
    def __init__(self):
        self.task_queue = queue.Queue()  # FIFO Queue
        self.running = True
        self.loop = asyncio.new_event_loop()
        self.loop_thread = threading.Thread(target=self._start_event_loop, daemon=True)
        self.loop_thread.start()
        self.worker_thread = threading.Thread(target=self._worker, daemon=True)
        self.worker_thread.start()

    def _start_event_loop(self):
        asyncio.set_event_loop(self.loop)
        try:
            self.loop.run_forever()
        except Exception as e:
            print(f"Event loop error: {e}")
        finally:
            self.loop.close()

    def _worker(self):
        while self.running or not self.task_queue.empty():
            try:
                task, args, kwargs = self.task_queue.get(timeout=1)  # Get task in FIFO order
                
                if asyncio.iscoroutinefunction(task):
                    future = asyncio.run_coroutine_threadsafe(task(*args, **kwargs), self.loop)
                    future.result()  # Ensures sequential execution
                else:
                    task(*args, **kwargs)  # Run sync task directly

                self.task_queue.task_done()  # Mark task as completed

            except queue.Empty:
                continue

    def submit_task(self, task, *args, **kwargs):
        """Submit a sync or async task to be executed in order."""
        self.task_queue.put((task, args, kwargs))

    def stop(self):
        """Gracefully stop the worker after completing all pending tasks."""
        print("\nWaiting for all tasks to finish...")
        self.task_queue.join()  # Wait until all tasks are completed
        self.running = False
        self.loop.call_soon_threadsafe(self.loop.stop)  # Stop event loop
        self.worker_thread.join()
        self.loop_thread.join()
        print("All tasks completed. Shutdown successful.")

# === Example Tasks ===
def sync_task(name, delay=2):
    print(f"[{time.strftime('%X')}] Sync Task {name} started")
    time.sleep(delay)
    print(f"[{time.strftime('%X')}] Sync Task {name} completed")
def sync_task2(name, delay=2):
    print(f"[{time.strftime('%X')}] Sync Task2 {name} started")
    time.sleep(delay)
    print(f"[{time.strftime('%X')}] Sync Task2 {name} completed")

async def async_task(name, delay=2):
    print(f"[{time.strftime('%X')}] Async Task {name} started")
    await asyncio.sleep(delay)
    print(f"[{time.strftime('%X')}] Async Task {name} completed")

# === Usage ===
task_worker = OrderedTaskWorker()

# Submitting tasks in order
task_worker.submit_task(sync_task, "Sync1", delay=3)
task_worker.submit_task(async_task, "Async1", delay=2)
task_worker.submit_task(sync_task, "Sync2", delay=1)
task_worker.submit_task(async_task, "Async2", delay=4)

print("Microservice is running...")
start = time.perf_counter()
for i in range(5):
    sync_task2(0)
end = time.perf_counter()
print(f"Time taken: {end - start:.2f} seconds")
while True:
    time.sleep(10)

