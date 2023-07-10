#!/usr/bin/python
# coding: utf-8

import os
from abc import ABC, abstractmethod
from typing import List

from torch.utils.data import Dataset

from agl.python.data.reader_util import write_format_file, read_schema, read_meta, read_file_with_position, \
    read_file_with_handler_and_position


class AGLTorchDataset(Dataset, ABC):
    def __init__(self, file_path, is_processed: bool):
        self.file_path = file_path
        self.is_processed = is_processed

    def __getitem__(self, index):
        return self.get_item(index)

    def __len__(self):
        return self.len()

    @abstractmethod
    def get_item(self, index):
        raise NotImplementedError

    @abstractmethod
    def len(self):
        raise NotImplementedError


class PPITorchDataset(AGLTorchDataset):
    def __init__(self, src_file_path, is_processed: bool,
                 processed_file_dir: str,
                 has_schema: bool = True, column_sep: str = '\t',
                 schema:List[str] = None,
                 processed_file_suffix: str = "train"):
        super().__init__(src_file_path, is_processed)
        self.src_has_schema = has_schema
        self.processed_file_dir = processed_file_dir
        self.column_sep = column_sep
        self.processed_file_suffix = processed_file_suffix
        self.meta: List[int] = []
        self._schema = schema  # 如果 src_has_schema, 则会替换成文件中实际的schema
        self._default_schema = ["id", "graph_feature", "label"]
        self.file_handle = None

        self._process()
        print(f"processed_file_name:{self.processed_file_name}, schema:{self.schema}")

    def __del__(self):
        if self.file_handle:
            self.file_handle.close()

    @property
    def schema(self):
        if self._schema:
            return self._schema
        else:
            return self._default_schema
        # if self.src_has_schema:
        #     return self._schema
        # else:
        #     raise NotImplementedError(f"self.src_has_schema is {self.src_has_schema}")

    @property
    def processed_file_name(self):
        if self.is_processed:
            return self.file_path
        else:
            return os.path.join(self.processed_file_dir, f"ppi_{self.processed_file_suffix}.txt")

    def _get_mata_from_processed_file(self):
        if self.src_has_schema:
            schema_line = read_schema(self.processed_file_name)
            if isinstance(schema_line, bytes):
                schema_line = schema_line.decode('utf-8')
                self._schema = schema_line.strip().split(self.column_sep)
            elif isinstance(schema_line, str):
                self._schema = schema_line.strip().split(self.column_sep)
            else:
                raise NotImplementedError
        self.meta = read_meta(self.processed_file_name)

    def _process(self):
        # 如果已经处理了，直接读取 schema 和 meta信息即可
        if not self.is_processed:
            print(f"self.processed_file_name: {self.processed_file_name}")
            write_format_file(self.file_path, self.processed_file_name, has_schema=self.src_has_schema)
        # self.is_processed = True
        self._get_mata_from_processed_file()
        # self.file_handle = open(self.processed_file_name, 'rb')

    def len(self):
        return len(self.meta)

    def get_item(self, index):
        if index < self.len():
            pos = self.meta[index]
            # # 下面方法会频繁打开和关闭文件
            line = read_file_with_position(self.processed_file_name, pos)
            # collate 多进程的时候下述方法有问题, 所以目前每次get item都需要打开和关闭文件
            # line = read_file_with_handler_and_position(self.file_handle, pos)
            if isinstance(line, bytes):
                line_list = line.strip().split(self.column_sep.encode('utf-8'))
                assert len(line_list) == len(self.schema)
                return dict(zip(self.schema, line_list))
            elif isinstance(line, str):
                line_list = line.strip().split(self.column_sep)
                assert len(line_list) == len(self.schema)
                return dict(zip(self.schema, line_list))
