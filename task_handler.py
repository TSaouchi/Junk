import asyncio
import time
import threading
import queue
import uuid
from concurrent.futures import ProcessPoolExecutor

# --- Define CPU and Async Tasks ---
async def async_task(data):
    """An async task that depends on the result of a CPU-bound task."""
    await asyncio.sleep(1)  # Simulating async work
    return f"Processed Async: {data}"

def cpu_bound_task(x):
    """A CPU-bound task (simulating heavy computation)."""
    time.sleep(2)  # Simulating CPU-intensive work
    return f"CPU Computed: {x**2}"

class OrderedTaskWorker:
    def __init__(self, max_workers=4, max_processes=2):
        self.task_queue = queue.PriorityQueue()
        self.running = True
        self.task_results = {}

        # Executor for CPU-bound tasks
        self.process_executor = ProcessPoolExecutor(max_workers=max_processes)

        # Async loop running in a separate thread
        self.loop = asyncio.new_event_loop()
        self.loop_thread = threading.Thread(target=self._start_event_loop, daemon=True)
        self.loop_thread.start()

        # Worker thread
        self.worker_thread = threading.Thread(target=self._worker, daemon=True)
        self.worker_thread.start()

    def _start_event_loop(self):
        """Runs the asyncio event loop in a separate thread."""
        asyncio.set_event_loop(self.loop)
        self.loop.run_forever()

    def _worker(self):
        """Worker thread that processes tasks from the queue."""
        while self.running or not self.task_queue.empty():
            try:
                priority, task_id, task, args, kwargs, prev_task_map, is_cpu_bound = self.task_queue.get(timeout=1)

                # Resolve dependencies (get results from previous tasks)
                for prev_task_id, arg_name in (prev_task_map or {}).items():
                    while prev_task_id not in self.task_results:
                        time.sleep(0.1)  # Wait until the required task is completed
                    kwargs[arg_name] = self.task_results.pop(prev_task_id)

                # Execute the task
                if asyncio.iscoroutinefunction(task):
                    future = asyncio.run_coroutine_threadsafe(task(*args, **kwargs), self.loop)
                    result = future.result()
                elif is_cpu_bound:
                    future = self.process_executor.submit(task, *args, **kwargs)
                    result = future.result()
                else:
                    result = task(*args, **kwargs)

                # Store the result
                if task_id is not None:
                    self.task_results[task_id] = result
                    print(f"Task {task_id} completed: {result}")  # Print when task is done

                self.task_queue.task_done()
            except queue.Empty:
                continue

    def submit_task(self, task, *args, task_id=None, prev_task_map=None, priority=10, is_cpu_bound=False, **kwargs):
        """Submit a task with a priority (lower number = higher priority)."""
        if task_id is None:
            task_id = str(uuid.uuid4())
        self.task_queue.put((priority, task_id, task, args, kwargs, prev_task_map, is_cpu_bound))
        return task_id

    def wait_for_completion(self):
        """Wait until all tasks in the queue are finished."""
        self.task_queue.join()

    def shutdown(self):
        """Shuts down the worker gracefully."""
        self.running = False
        self.worker_thread.join()
        self.process_executor.shutdown(wait=True)
        self.loop.call_soon_threadsafe(self.loop.stop)

# --- Ensure Compatibility on Windows ---
if __name__ == "__main__":
    worker = OrderedTaskWorker(max_workers=4, max_processes=2)

    # Submit CPU-bound task
    cpu_task_id = worker.submit_task(cpu_bound_task, 10, priority=1, is_cpu_bound=True)

    # Submit async task that depends on the CPU-bound task result
    async_task_id = worker.submit_task(
        async_task,
        task_id="async_1",
        prev_task_map={cpu_task_id: "data"},
        priority=2
    )

    # Wait for tasks to complete
    worker.wait_for_completion()
    worker.shutdown()
