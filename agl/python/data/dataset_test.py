#!/usr/bin/python
# coding: utf-8

import unittest
import os

from agl.python.data.dataset import PPITorchDataset


class AGLDatasetTest(unittest.TestCase):
    schema = ["id", "graph_feature", "label"]
    record = [
        ["1", "aaappooppm", "0"],
        ["2", "sdsadfas", "1"],
        ["3", "sdafafre", "0"]
    ]
    schema_sep = "\t"

    @staticmethod
    def mock_data():

        script_dir = os.path.dirname(os.path.abspath(__file__))
        file_name = os.path.join(script_dir, "mock_origin.txt")
        with open(file_name, "w") as f:
            schema_str = AGLDatasetTest.schema_sep.join(AGLDatasetTest.schema) + "\n"
            f.write(schema_str)
            for i in range(len(AGLDatasetTest.record)):
                r_str = AGLDatasetTest.schema_sep.join(AGLDatasetTest.record[i]) + "\n"
                f.write(r_str)
        f.close()
        return file_name

    def test_agl_ppi_dataset(self):
        file_name = AGLDatasetTest.mock_data()
        script_dir = os.path.dirname(os.path.abspath(__file__))
        my_test_dataset = PPITorchDataset(file_name, False, script_dir)
        self.assertEqual(my_test_dataset.len(), 3)
        for i in range(my_test_dataset.len()):
            r_dict = my_test_dataset[i]
            r_id, r_gf, r_label = r_dict["id"].decode('utf-8'), r_dict["graph_feature"].decode('utf-8'), r_dict["label"].decode('utf-8')
            self.assertEqual(r_id, AGLDatasetTest.record[i][0])
            self.assertEqual(r_gf, AGLDatasetTest.record[i][1])
            self.assertEqual(r_label, AGLDatasetTest.record[i][2])

    def test_data_loader(self):
        from torch.utils.data import DataLoader
        file_name = AGLDatasetTest.mock_data()
        script_dir = os.path.dirname(os.path.abspath(__file__))
        my_test_dataset = PPITorchDataset(file_name, False, script_dir)
        train_loader = DataLoader(dataset=my_test_dataset,
                                  batch_size=5,
                                  shuffle=False)
        for i, data in enumerate(train_loader):
            print(f"i:{i}, data:{data} \n")
