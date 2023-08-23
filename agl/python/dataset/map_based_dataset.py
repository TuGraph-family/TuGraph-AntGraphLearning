#!/usr/bin/python
# coding: utf-8

from typing import List
from torch.utils.data import Dataset
from torch.utils.data import get_worker_info

from agl.python.dataset.reader_util import (
    get_meta_info_from_file,
    read_file_with_handler_and_position,
)


class AGLTorchMapBasedDataset(Dataset):
    """AGL map-style dataset

    To use this dataset, assume you have a csv file with schema ["id", "graph_feature", "label"]:

    >>> file = "a.csv"
    >>> map_dataset = AGLTorchMapBasedDataset(file=file, column_sep=",")
    >>> print(map_dataset[0])
    >>> {"id": xxx, "graph_feature": xxx, "label": xxx}

    if you have a plain text format file with schema ["id", "graph_feature", "label"], and column_sep is "\\t":

    >>> file = "a.txt"
    >>> map_dataset = AGLTorchMapBasedDataset(file=file, column_sep="\\t")
    >>> print(map_dataset[0])
    >>> {"id": xxx, "graph_feature": xxx, "label": xxx}
    """

    def __init__(
        self,
        file: str,
        format: str = "csv",
        has_schema: bool = True,
        column_sep: str = ",",
        schema: List[str] = None,
    ):
        """AGL map-style dataset for local file (csv, or plain text)

        Args:
            file(str): The file path of dataset
            format(str): File format, only support csv or plain text now.
            has_schema(bool): Whether schema exists in the file; if none, you must set the schema filed
            column_sep(str): The separator for different columns in file
            schema(List(str)): File schema. if none, will use the schema in file.
                               The order should match the column order in file

        Returns:
            AGLTorchMapBasedDataset: a subclass of Pytorch Dataset
        """
        self._file_path = file
        self._file_format = format
        self._has_schema = has_schema
        self._column_sep = column_sep
        self._schema = schema
        self._position_meta = []
        self._file_opened_dict = dict()
        self._prepare_meta_info()
        self._prepare_reader()
        self._check_format()

    def __del__(self):
        for k, f in self._file_opened_dict.items():
            f.close()

    def __len__(self):
        return self.len()

    def __getitem__(self, index):
        return self.get_item(index)

    def _check_format(self):
        if self._file_format not in {"csv", "txt"}:
            raise NotImplementedError(
                "AGLTorchMapBasedDataset now only support txt or csv format data for now"
            )

    def _prepare_meta_info(self):
        """
        Store the meta information required for the map-based dataset in memory.
        """
        schema_line, position = get_meta_info_from_file(
            self._file_path, self._has_schema
        )
        if self._has_schema:
            if isinstance(schema_line, bytes):
                schema_line = schema_line.decode("utf-8")
            schema_in_file = schema_line.strip().split(self._column_sep)

            # Use the schema from the file as the schema or validate if the provided schema matches
            # the number of fields.
            if self._schema is None:
                self._schema = schema_in_file
            else:
                assert len(self._schema) == len(schema_in_file)

        self._position_meta = position

    def _prepare_reader(self, worker_id=0):
        # Create a reader.
        file_handler = open(self._file_path, "r")
        self._file_opened_dict.update({worker_id: file_handler})

    def _get_item_with_file(self, index, worker_id=0):
        if worker_id not in self._file_opened_dict.keys():
            # each process opens the file once.
            self._prepare_reader(worker_id)
        file_handler = self._file_opened_dict[worker_id]
        if index < self.len():
            pos = self._position_meta[index]
            line = read_file_with_handler_and_position(file_handler, pos)
            if isinstance(line, str):
                # The final output is always in the form of bytes, which makes it convenient
                # for passing data between Python and C++.
                line = line.encode("utf-8")
            line_list = line.strip().split(self._column_sep.encode("utf-8"))
            assert len(line_list) == len(self._schema)
            return dict(zip(self._schema, line_list))

    def len(self):
        """

        Returns: length of dataset

        """
        return len(self._position_meta)

    def get_item(self, index):
        """

        Args:
            index: index that you want to pick from the dataset

        Returns: data record referring to index

        """
        worker_info = get_worker_info()
        if worker_info is None:
            # single-process data loading
            return self._get_item_with_file(index)
        else:
            # multi-process data loading
            worker_id = worker_info.id
            return self._get_item_with_file(index, worker_id=worker_id)
