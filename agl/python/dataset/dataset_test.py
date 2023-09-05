#!/usr/bin/python
# coding: utf-8

import unittest
import os

from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.dataset.iterable_dataset import AGLIterableDataset


class AGLDatasetTest(unittest.TestCase):
    schema = ["id", "graph_feature", "label"]
    record = [
        ["1i", "aaappooppm", "0i"],
        ["2i", "sdsadfas", "1i"],
        ["3i", "sdafafre", "0i"],
    ]
    schema_sep = ","
    script_dir = os.path.dirname(os.path.abspath(__file__))
    file_name = os.path.join(script_dir, "../data/mock_origin.txt")

    @staticmethod
    def mock_data(file_name):
        with open(file_name, "w") as f:
            schema_str = AGLDatasetTest.schema_sep.join(AGLDatasetTest.schema) + "\n"
            f.write(schema_str)
            for i in range(len(AGLDatasetTest.record)):
                r_str = AGLDatasetTest.schema_sep.join(AGLDatasetTest.record[i]) + "\n"
                f.write(r_str)
        f.close()

    @classmethod
    def setUpClass(cls) -> None:
        AGLDatasetTest.mock_data(cls.file_name)

    def test_agl_map_style_dataset(self):
        test_dataset = AGLTorchMapBasedDataset(self.file_name, column_sep=",")
        self.assertEqual(test_dataset.len(), len(self.record))
        for i in range(test_dataset.len()):
            res_dict = test_dataset[i]
            res_id, res_gf, res_label = (
                res_dict["id"].decode("utf-8"),
                res_dict["graph_feature"].decode("utf-8"),
                res_dict["label"].decode("utf-8"),
            )
            self.assertEqual(res_id, AGLDatasetTest.record[i][0])
            self.assertEqual(res_gf, AGLDatasetTest.record[i][1])
            self.assertEqual(res_label, AGLDatasetTest.record[i][2])

    def test_agl_iterable_style_dataset(self):
        test_dataset = AGLIterableDataset(self.file_name)
        i = 0
        for data in test_dataset:
            res_dict = data
            res_id, res_gf, res_label = (
                res_dict["id"][0].decode("utf-8"),
                res_dict["graph_feature"][0].decode("utf-8"),
                res_dict["label"][0].decode("utf-8"),
            )
            self.assertEqual(res_id, AGLDatasetTest.record[i][0])
            self.assertEqual(res_gf, AGLDatasetTest.record[i][1])
            self.assertEqual(res_label, AGLDatasetTest.record[i][2])
            i = i + 1

    def test_data_loader(self):
        from torch.utils.data import DataLoader

        script_dir = os.path.dirname(os.path.abspath(__file__))
        my_test_dataset = AGLTorchMapBasedDataset(self.file_name, "txt", column_sep=",")
        train_loader = DataLoader(dataset=my_test_dataset, batch_size=5, shuffle=False)
        sample_num = 0
        for i, data in enumerate(train_loader):
            sample_num = sample_num + len(data["id"])
        self.assertEqual(sample_num, len(my_test_dataset))
