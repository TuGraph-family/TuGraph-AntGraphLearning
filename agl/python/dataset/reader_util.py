#!/usr/bin/python
# coding: utf-8


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
