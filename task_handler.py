import threading
import queue
import asyncio
import time
import uuid

class OrderedTaskWorker:
    def __init__(self):
        self.task_queue = queue.Queue()  # FIFO Queue
        self.running = True
        self.task_results = {}  # Shared dictionary to store task results
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
                task_id, task, args, kwargs, prev_task_map = self.task_queue.get(timeout=1)  # Get task in FIFO order
                
                # Inject previous task results into the specified arguments
                for prev_task_id, arg_name in (prev_task_map or {}).items():
                    if prev_task_id in self.task_results:
                        kwargs[arg_name] = self.task_results.pop(prev_task_id)
                
                # Execute task
                if asyncio.iscoroutinefunction(task):
                    future = asyncio.run_coroutine_threadsafe(task(*args, **kwargs), self.loop)
                    result = future.result()  # Ensures sequential execution
                else:
                    result = task(*args, **kwargs)  # Run sync task directly
                
                # Store the result if needed
                if task_id is not None:
                    self.task_results[task_id] = result
                
                self.task_queue.task_done()  # Mark task as completed

            except queue.Empty:
                continue

    def submit_task(self, task, *args, task_id=None, prev_task_map=None, **kwargs):
        """Submit a sync or async task to be executed in order."""
        if task_id is None:
            task_id = str(uuid.uuid4())  # Generate a unique task ID
        self.task_queue.put((task_id, task, args, kwargs, prev_task_map))
        return task_id  # Return the generated task ID for reference

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
    result = f"Result from {name}"  # Returning result
    print(f"[{time.strftime('%X')}] Sync Task {name} completed with result: {result}")
    return result  # Store result

async def async_task(name, delay=2, prev_result=None):
    print(f"[{time.strftime('%X')}] Async Task {name} started, received: {prev_result}")
    await asyncio.sleep(delay)
    print(f"[{time.strftime('%X')}] Async Task {name} completed")

# === Usage ===
task_worker = OrderedTaskWorker()

# Submitting tasks in order
task1_id = task_worker.submit_task(sync_task, "Sync1", delay=3)
task2_id = task_worker.submit_task(async_task, "Async1", delay=2, prev_task_map={task1_id: "prev_result"})  # Async1 receives Sync1's result
task3_id = task_worker.submit_task(sync_task, "Sync2", delay=1)
task4_id = task_worker.submit_task(async_task, "Async2", delay=4, prev_task_map={task3_id: "prev_result"})

print("Microservice is running...")
start = time.perf_counter()
for i in range(0):
    print("do this do this do this")
end = time.perf_counter()
print(f"Time taken: {end - start:.2f} seconds")
while True:
    time.sleep(10)
