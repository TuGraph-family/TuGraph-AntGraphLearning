import numpy as np
from typing import List

import torch
import pyarrow.dataset as ds
from torch.utils.data import IterableDataset
from torch.utils.data import get_worker_info


class AGLIterableDataset(IterableDataset):
    """AGLIterableDataset

    To use this dataset, assume you have a csv file with schema ["id", "graph_feature", "label"]

    >>> file = "a.csv"
    >>> iterable_dataset = AGLIterableDataset(file=file)
    >>> for data in iterable_dataset:
    >>>    print(f"data : {data}")
    >>>    break
    >>> data : {"id": [xxx], "graph_feature": [xxx], "label": [xxx]}
    """

    def __init__(
        self,
        file: str,
        format: str = "csv",
        schema: List[str] = None,
        batch_size: int = 1,
    ):
        """AGLIterableDataset for local file (csv).

        Args:
            file(str):  The file path of dataset
            format(str): file format, for now, only support csv
            schema(List(str)): file schema to read, if none, will return all columns
            batch_size(int): for every iterator, return batch_size records

        Returns:
            AGLIterableDataset a subclass of Pytorch IterableDataset

        Note
        ----
        Different from AGLTorchMapBasedDataset, batch_size should be set in AGLIterableDataset,
        and thus should not be set in Dataloader

        """
        self._file = file
        self._schema = schema
        self._batch_size = batch_size
        self._dataset = ds.dataset(self._file, format=format)
        self._total_rows = self._dataset.count_rows()
        self._valid_columns()
        self._workload_dict = {}

    def _valid_columns(self):
        if self._schema is None:
            pass
        else:
            fields_names = set(self._dataset.schema.names)
            for name in self._schema:
                if name not in fields_names:
                    raise ValueError(
                        f"{name} in schema not in file schema: {fields_names}"
                    )

    def _get_batch_workload(
        self, work_load_num, total_batch_num, batch_index_array, worker_id=0
    ):
        if worker_id in self._workload_dict.keys():
            return self._workload_dict[worker_id]
        start = worker_id * work_load_num
        end = min(start + work_load_num, total_batch_num)
        load_for_worker = batch_index_array[start:end]
        self._workload_dict.update({worker_id: load_for_worker})
        return load_for_worker

    def __iter__(self):
        worker_info = get_worker_info()
        batch_num = self._total_rows // self._batch_size
        batch_index = np.arange(0, batch_num)
        batch_workload = []
        if worker_info is not None:
            # note 没有使用 pyarrow to_batches 接口，产生的batch 的 batch size 似乎会变动
            work_load_num = batch_num // worker_info.num_workers
            if work_load_num == 0:
                # 如果进程数 > batch 数目
                if worker_info.id < batch_num:
                    batch_workload = [batch_index[worker_info.id]]
                else:
                    return
            batch_workload = self._get_batch_workload(
                work_load_num, batch_num, batch_index, worker_info.id
            )
        else:
            batch_workload = self._get_batch_workload(batch_num, batch_num, batch_index)

        def string_to_bytes(batch_dict):
            for k, v in batch_dict.items():
                if isinstance(v[0], str):
                    v = [v_i.encode("utf-8") for v_i in v]
                    batch_dict.update({k: v})
            return batch_dict

        for batch_index in batch_workload:
            start = batch_index * self._batch_size
            end = min(start + self._batch_size, self._total_rows)
            indices = np.arange(start, end)
            batch = self._dataset.take(indices).to_pydict()
            batch = string_to_bytes(batch)
            yield batch
