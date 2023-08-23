#!/usr/bin/python
# coding: utf-8

import unittest
import numpy as np

from agl.python.data.column import (
    AGLDenseColumn,
    AGLRowColumn,
    AGLMultiDenseColumn,
)


class ColumnTest(unittest.TestCase):
    def test_dense_column(self):
        import torch

        mock_dense = [b"1 2 3", b"3 5 6"]
        dense = AGLDenseColumn(name="df", dim=3, dtype=np.int64, sep=" ")
        result = dense(mock_dense)
        self.assertEqual(result.dtype, torch.int64)
        print(result)
        res_np = result.numpy()
        truth = [[1, 2, 3], [3, 5, 6]]
        self.assertListEqual(truth, res_np.tolist())

    def test_raw_column(self):
        mock_raw = [b"1 2 3", b"3 5 6"]
        raw = AGLRowColumn("raw")
        result = raw(mock_raw)
        print(result)
        self.assertListEqual(result, mock_raw)

    def test_mutidense_column(self):
        import torch

        mock_multi_dense_int = [b"1,2,3 4,5,6", b"7,8,9"]
        multi_dense = AGLMultiDenseColumn(
            name="mdf", dim=3, dtype=np.int64, out_sep=" ", in_sep=",", concat=True
        )
        result = multi_dense(mock_multi_dense_int)
        self.assertEqual(result.dtype, torch.int64)
        truth = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
        self.assertListEqual(truth, result.numpy().tolist())

        multi_dense_int32 = AGLMultiDenseColumn(
            name="mdf", dim=3, dtype=np.int32, out_sep=" ", in_sep=",", concat=True
        )
        result_int32 = multi_dense_int32(mock_multi_dense_int)
        self.assertEqual(result_int32.dtype, torch.int32)
        truth = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
        self.assertListEqual(truth, result_int32.numpy().tolist())

        multi_dense_float = AGLMultiDenseColumn(
            name="mdf", dim=3, dtype=np.float32, out_sep=" ", in_sep=",", concat=True
        )
        result_float = multi_dense_float(mock_multi_dense_int)
        self.assertEqual(result_float.dtype, torch.float32)
        truth = [[1.0, 2.0, 3.0], [4.0, 5.0, 6.0], [7.0, 8.0, 9.0]]
        self.assertAlmostEqual(np.array(truth).all(), result_float.numpy().all())

        multi_dense_double = AGLMultiDenseColumn(
            name="mdf", dim=3, dtype=np.float64, out_sep=" ", in_sep=",", concat=True
        )
        result_double = multi_dense_double(mock_multi_dense_int)
        self.assertEqual(result_double.dtype, torch.float64)
        self.assertAlmostEqual(np.array(truth).all(), result_double.numpy().all())
