#!/usr/bin/python
# coding: utf-8

import struct


def get_meta_info_from_file(file: str, has_schema: bool = True):
    """scan file and record file meta info into memory

    assume you have file with schema ["id", "graph_feature", "label"], and column_spe is ",":

    >>> file = "a.csv"
    >>> schema_line, positions = get_meta_info_from_file(file)
    >>> print(f"schema_line: {schema_line}, positions: {positions}")
    >>> "schema_line: id,graph_feature,graph_feature, positions: [xxx, xxx]"

    Args:
        file(str): file path (support csv and plain text )
        has_schema(bool): weather have schema in first line of the file

    Returns:
        (schema, positions)
        schema: schema line
        positions: position for each line

    """
    positions = []
    i = 0
    schema = None
    with open(file, "r") as ori_src:
        while True:
            if has_schema and i == 0:
                # skip first line if its schema
                schema = ori_src.readline()
            pos = ori_src.tell()
            line = ori_src.readline()
            i = i + 1
            if not line:
                break
            positions.append(pos)
    ori_src.close()
    return schema, positions


def read_file_with_handler_and_position(file_handler, position: int):
    """read out a line from file with specific position

    usually used with get_meta_info_from_file() function

    >>> file = "a.csv"
    >>> schema_line, positions = get_meta_info_from_file(file)
    >>> file_handler = open(file, "r")
    >>> res = read_file_with_handler_and_position(file_handler, positions[0])
    >>> print(res)
    >>> "XXXX" (line at positions[0])

    Args:
        file_handler(file): python file object
        position(int):     position to seek the file handler

    Returns: line wrt. position in this file handler

    """
    file_handler.seek(position)
    return file_handler.readline()


# todo will delete later
def write_format_file(
    src_file, dst_file, src_as_dst: bool = False, has_schema: bool = True
):
    positions = []
    i = 0
    meta_pos = 0
    with open(src_file, "r") as f_src:
        with open(dst_file, "wb") as f_dst:
            while True:
                line = f_src.readline()
                if not line:
                    break
                if has_schema and i == 0:
                    f_dst.write(line.encode("utf-8"))
                    i += 1
                else:
                    positions.append(f_dst.tell())
                    f_dst.write(line.encode("utf-8"))
                    i += 1
                if i % 1000 == 0:
                    print(f"processed {i} lines\n")
            f_dst.write("\n".encode("utf-8"))  # 防止最后一行没有换行符
            meta_pos = f_dst.tell()
            pos_str = " ".join(str(pos) for pos in positions)

            f_dst.write(pos_str.encode("utf-8"))
            f_dst.write("\n".encode("utf-8"))
    f_src.close()
    f_dst.close()

    with open(dst_file, "ab") as f:
        # 使用Q表示 64位 无符号长整型 ，0 到 18,446,744,073,709,551,615
        # print(f">>>> meta_pos:{meta_pos}")
        binary_num = struct.pack("Q", meta_pos)
        f.write(binary_num)
    f.close()


# todo will delete later
def read_schema(file_path, read_format: str = "rb"):
    with open(file_path, read_format) as f:
        # 第一行数据为schema
        schema = f.readline()
    f.close()
    return schema


def read_meta(file_path, read_format: str = "rb"):
    meta_pos = 0
    with open(file_path, read_format) as f:
        # 调整文件指针的位置，从文件末尾读取8个字节（uint64）, 找到meta信息所在的文件位置
        f.seek(-8, 2)
        binary_num = f.read(8)
        meta_pos = struct.unpack("Q", binary_num)[0]
        print(f">>>> read meta, meta_pos:{meta_pos}")
        f.seek(meta_pos)
        line = f.readline()
        # print(f"position line:{line}")
        positions = [int(pos) for pos in line.split()]
    f.close()
    return positions


# todo will delete later
def read_file_with_position(file_path, position: int, read_format: str = "rb"):
    with open(file_path, read_format) as f:
        f.seek(position)
        if f is not None:
            line = f.readline()
    f.close()
    return line
