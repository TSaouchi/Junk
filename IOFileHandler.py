import os
import threading

class IOFileHandler:
    """Manages multiple files, reading and writing only when each file changes, with thread-safe operations."""

    def __init__(self):
        self.files = {}  # Store {name: {"path": str, "mtime": float, "content": str}}
        self.lock = threading.Lock()  # Lock for thread-safe writes

    def register_file(self, name, path):
        """Registers a new file with a unique name."""
        if name in self.files:
            raise ValueError(f"File name '{name}' is already registered.")
        self.files[name] = {"path": path, "mtime": None, "content": ""}
        print(f"Registered file '{name}' at '{path}'.")

    def read_if_changed(self, name):
        """Reads the file only if it has changed since the last read."""
        if name not in self.files:
            raise ValueError(f"File '{name}' is not registered.")

        file_info = self.files[name]
        file_path = file_info["path"]

        try:
            current_mtime = os.stat(file_path).st_mtime  # Get modification time
            if file_info["mtime"] is None or current_mtime > file_info["mtime"]:
                file_info["mtime"] = current_mtime  # Update last modification time
                with open(file_path, "r") as file:
                    file_info["content"] = file.read()
                print(f"File '{name}' changed! New content read.")
            else:
                print(f"No changes detected in '{name}'. Skipping read.")
        except FileNotFoundError:
            print(f"File '{name}' not found.")

    def get_content(self, name):
        """Returns the last-read content of a file."""
        if name not in self.files:
            raise ValueError(f"File '{name}' is not registered.")
        return self.files[name]["content"]

    def write(self, name, new_content):
        """Writes content to the file in a thread-safe manner."""
        if name not in self.files:
            raise ValueError(f"File '{name}' is not registered.")

        file_info = self.files[name]
        file_path = file_info["path"]

        # Use the lock to ensure thread-safe writing
        with self.lock:
            try:
                with open(file_path, "w") as file:
                    file.write(new_content)
                # Update the content and mtime after a successful write
                file_info["content"] = new_content
                file_info["mtime"] = os.stat(file_path).st_mtime
                print(f"File '{name}' successfully written.")
            except FileNotFoundError:
                print(f"File '{name}' not found.")

    def purge_file(self, name):
        """Removes a specific file from tracking."""
        if name in self.files:
            del self.files[name]
            print(f"Purged file '{name}'.")
        else:
            print(f"File '{name}' not found in registry.")

    def purge_all(self):
        """Removes all tracked files."""
        self.files.clear()
        print("Purged all files.")
