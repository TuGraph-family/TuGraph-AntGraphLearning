#!/usr/bin/python
# coding: utf-8

import numpy as np
from enum import Enum
from collections import namedtuple
from typing import *

from pyagl.pyagl import AGLDType

DTypeValue = namedtuple("DTypeValue", ["name", "np_dtype", "c_dtype"])


class AGLDTypeHelper(Enum):
    int8 = DTypeValue("int8", np.int8, AGLDType.INT8)
    int16 = DTypeValue("int16", np.int16, AGLDType.INT16)
    int32 = DTypeValue("int32", np.int32, AGLDType.INT32)
    int64 = DTypeValue("int64", np.int64, AGLDType.INT64)
    uint8 = DTypeValue("uint8", np.uint8, AGLDType.UINT8)
    uint16 = DTypeValue("uint16", np.uint16, AGLDType.UINT16)
    uint32 = DTypeValue("uint32", np.uint32, AGLDType.UINT32)
    uint64 = DTypeValue("uint64", np.uint64, AGLDType.UINT64)
    float32 = DTypeValue("float32", np.float32, AGLDType.FLOAT)
    float64 = DTypeValue("float64", np.float64, AGLDType.DOUBLE)

    @property
    def name(self):
        return self.value.name

    @property
    def np_dtype(self):
        return self.value.np_dtype

    @property
    def c_dtype(self):
        return self.value.c_dtype


np_to_agl_dtype: Dict[type, AGLDType] = {item.np_dtype: item.c_dtype for item in AGLDTypeHelper}
agl_dtype_to_np: Dict[AGLDType, type] = {item.c_dtype: item.np_dtype for item in AGLDTypeHelper}



