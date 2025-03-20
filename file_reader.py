# file_reader.py
import json
import csv
import os
import configparser
import yaml
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

class TxtFileReader(FileReader):
    def read(self):
        with open(self.file_path, 'r', encoding='utf-8') as file:
            return file.read()

class CsvFileReader(FileReader):
    def read(self):
        with open(self.file_path, 'r', encoding='utf-8') as file:
            reader = csv.reader(file)
            return [row for row in reader]

class JsonFileReader(FileReader):
    def read(self):
        with open(self.file_path, 'r', encoding='utf-8') as file:
            return json.load(file)

class IniFileReader(FileReader):
    def read(self):
        config = configparser.ConfigParser()
        config.read(self.file_path, encoding='utf-8')
        return {section: dict(config[section]) for section in config.sections()}

class YamlFileReader(FileReader):
    def read(self):
        with open(self.file_path, 'r', encoding='utf-8') as file:
            return yaml.safe_load(file)

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

# main.py
import argparse
from file_reader import FileReaderFactory

def main():
    parser = argparse.ArgumentParser(description="Read files based on extension.")
    parser.add_argument("file_path", help="Path to the file.")
    parser.add_argument("--extension", help="Specify file extension manually (optional).", required=False)
    args = parser.parse_args()
    
    try:
        reader = FileReaderFactory.get_reader(args.file_path, args.extension)
        content = reader.read()
        print("File Content:", content)
    except ValueError as e:
        print(e)

if __name__ == "__main__":
    main()

# requirements.txt
yaml
