import json
import csv
import configparser
import yaml
import os
import threading
from abc import ABC, abstractmethod
from file_reader import FileExtensions  # Make sure FileExtensions is imported correctly

class FileWriter(ABC):
    """Abstract base class for file writers."""
    def __init__(self, file_path):
        self.file_path = file_path
        self.lock = threading.Lock()  # Create a lock for each file writer
    
    @abstractmethod
    def write(self, data):
        pass

    def _lock_and_write(self, write_function):
        """Acquire lock on the file to avoid race conditions during writing."""
        with self.lock:  # Acquire the lock before writing
            with open(self.file_path, 'w', encoding='utf-8') as file:
                write_function(file)

class TxtFileWriter(FileWriter):
    def write(self, data):
        def write_function(file):
            file.write(data)
        
        self._lock_and_write(write_function)

class CsvFileWriter(FileWriter):
    def write(self, data):
        def write_function(file):
            writer = csv.writer(file)
            writer.writerows(data)
        
        self._lock_and_write(write_function)

class JsonFileWriter(FileWriter):
    def write(self, data):
        def write_function(file):
            json.dump(data, file, indent=4)
        
        self._lock_and_write(write_function)

class IniFileWriter(FileWriter):
    def write(self, data):
        def write_function(file):
            config = configparser.ConfigParser()
            for section, values in data.items():
                config[section] = values
            config.write(file)
        
        self._lock_and_write(write_function)

class YamlFileWriter(FileWriter):
    def write(self, data):
        def write_function(file):
            yaml.safe_dump(data, file)
        
        self._lock_and_write(write_function)

class FileWriterFactory:
    """Factory class to create file writers based on file extension."""
    _writers = {
        FileExtensions.TXT.value: TxtFileWriter,
        FileExtensions.CSV.value: CsvFileWriter,
        FileExtensions.JSON.value: JsonFileWriter,
        FileExtensions.INI.value: IniFileWriter,
        FileExtensions.YAML.value: YamlFileWriter,
        FileExtensions.YML.value: YamlFileWriter
    }
    
    @staticmethod
    def get_writer(file_path, extension=None):
        if extension:
            extension = extension.lower()
        else:
            extension = os.path.splitext(file_path)[1][1:].lower()
        
        writer_class = FileWriterFactory._writers.get(extension)
        if not writer_class:
            raise ValueError(f"Unsupported file extension: {extension}")
        return writer_class(file_path)

class SimpleFileWriter:
    """Wrapper class to simplify file writing usage with locking."""
    def __init__(self, file_path):
        # Automatically determine the file extension
        self.file_path = file_path
        self.extension = os.path.splitext(file_path)[1][1:].lower()
        self.writer = FileWriterFactory.get_writer(file_path, extension=self.extension)
    
    def write(self, data):
        """Write the data to the file using the appropriate writer."""
        self.writer.write(data)

# Example data to write to files
txt_data = "This is a text file."
csv_data = [["name", "age"], ["Alice", 30], ["Bob", 25]]
json_data = {"name": "Alice", "age": 30}
ini_data = {"section1": {"key1": "value1", "key2": "value2"}}
yaml_data = {"name": "Alice", "age": 30}

# Example file paths
txt_file_path = "example.txt"
csv_file_path = "example.csv"
json_file_path = "example.json"
ini_file_path = "example.ini"
yaml_file_path = "example.yaml"

# Write data to files using SimpleFileWriter
SimpleFileWriter(txt_file_path).write(txt_data)
SimpleFileWriter(csv_file_path).write(csv_data)
SimpleFileWriter(json_file_path).write(json_data)
SimpleFileWriter(ini_file_path).write(ini_data)
SimpleFileWriter(yaml_file_path).write(yaml_data)
