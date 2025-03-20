import json
import csv
import os
import configparser
import yaml
import portalocker
from abc import ABC, abstractmethod
from enum import Enum

class FileExtensions(Enum):
    TXT = "txt"
    CSV = "csv"
    JSON = "json"
    INI = "ini"
    YAML = "yaml"
    YML = "yml"

class FileReader(ABC):
    """Abstract base class for file readers."""
    def __init__(self, file_path):
        self.file_path = file_path
    
    @abstractmethod
    def read(self):
        pass

    def _lock_and_read(self, file, read_function):
        """Acquire lock on the file to avoid race conditions during reading."""
        with portalocker.Lock(file, 'r', timeout=10):  # Shared lock for reading
            return read_function(file)

class TxtFileReader(FileReader):
    def read(self):
        def read_function(file):
            return file.read()
        
        with open(self.file_path, 'r', encoding='utf-8') as file:
            return self._lock_and_read(file, read_function)

class CsvFileReader(FileReader):
    def read(self):
        def read_function(file):
            reader = csv.reader(file)
            return [row for row in reader]
        
        with open(self.file_path, 'r', encoding='utf-8') as file:
            return self._lock_and_read(file, read_function)

class JsonFileReader(FileReader):
    def read(self):
        def read_function(file):
            return json.load(file)
        
        with open(self.file_path, 'r', encoding='utf-8') as file:
            return self._lock_and_read(file, read_function)

class IniFileReader(FileReader):
    def read(self):
        def read_function(file):
            config = configparser.ConfigParser()
            config.read_file(file)
            return {section: dict(config[section]) for section in config.sections()}
        
        with open(self.file_path, 'r', encoding='utf-8') as file:
            return self._lock_and_read(file, read_function)

class YamlFileReader(FileReader):
    def read(self):
        def read_function(file):
            return yaml.safe_load(file)
        
        with open(self.file_path, 'r', encoding='utf-8') as file:
            return self._lock_and_read(file, read_function)

class FileReaderFactory:
    """Factory class to create file readers based on file extension."""
    _readers = {
        FileExtensions.TXT.value: TxtFileReader,
        FileExtensions.CSV.value: CsvFileReader,
        FileExtensions.JSON.value: JsonFileReader,
        FileExtensions.INI.value: IniFileReader,
        FileExtensions.YAML.value: YamlFileReader,
        FileExtensions.YML.value: YamlFileReader
    }
    
    @staticmethod
    def get_reader(file_path, extension=None):
        if extension:
            extension = extension.lower()
        else:
            extension = os.path.splitext(file_path)[1][1:].lower()
        
        reader_class = FileReaderFactory._readers.get(extension)
        if not reader_class:
            raise ValueError(f"Unsupported file extension: {extension}")
        return reader_class(file_path)

class SimpleFileReader:
    """Wrapper class to simplify file reading usage with locking."""
    def __init__(self, file_path):
        # Automatically determine the file extension
        self.file_path = file_path
        self.extension = os.path.splitext(file_path)[1][1:].lower()
        self.reader = FileReaderFactory.get_reader(file_path, extension=self.extension)
    
    def read(self):
        """Read the data from the file using the appropriate reader."""
        return self.reader.read()
