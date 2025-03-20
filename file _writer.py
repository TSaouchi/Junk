import json
import csv
import configparser
import yaml
import os
import portalocker
from abc import ABC, abstractmethod
from file_reader import FileExtensions  # Make sure to import FileExtensions

class FileWriter(ABC):
    """Abstract base class for file writers."""
    def __init__(self, file_path):
        self.file_path = file_path
    
    @abstractmethod
    def write(self, data):
        pass

    def _lock_and_write(self, file, write_function):
        # Acquire lock on the file to avoid race conditions
        with portalocker.Lock(file, 'w', timeout=10):  # Timeout to avoid hanging indefinitely
            write_function(file)

class TxtFileWriter(FileWriter):
    def write(self, data):
        def write_function(file):
            file.write(data)
        
        with open(self.file_path, 'w', encoding='utf-8') as file:
            self._lock_and_write(file, write_function)

class CsvFileWriter(FileWriter):
    def write(self, data):
        def write_function(file):
            writer = csv.writer(file)
            writer.writerows(data)
        
        with open(self.file_path, 'w', encoding='utf-8', newline='') as file:
            self._lock_and_write(file, write_function)

class JsonFileWriter(FileWriter):
    def write(self, data):
        def write_function(file):
            json.dump(data, file, indent=4)
        
        with open(self.file_path, 'w', encoding='utf-8') as file:
            self._lock_and_write(file, write_function)

class IniFileWriter(FileWriter):
    def write(self, data):
        def write_function(file):
            config = configparser.ConfigParser()
            for section, values in data.items():
                config[section] = values
            config.write(file)
        
        with open(self.file_path, 'w', encoding='utf-8') as file:
            self._lock_and_write(file, write_function)

class YamlFileWriter(FileWriter):
    def write(self, data):
        def write_function(file):
            yaml.safe_dump(data, file)
        
        with open(self.file_path, 'w', encoding='utf-8') as file:
            self._lock_and_write(file, write_function)

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
    """Wrapper class to simplify file writing usage."""
    def __init__(self, file_path):
        # Automatically determine the file extension
        self.file_path = file_path
        self.extension = os.path.splitext(file_path)[1][1:].lower()
        self.writer = FileWriterFactory.get_writer(file_path, extension=self.extension)
    
    def write(self, data):
        """Write the data to the file using the appropriate writer."""
        self.writer.write(data)

