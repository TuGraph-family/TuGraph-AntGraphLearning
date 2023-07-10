#!/usr/bin/python
# coding: utf-8

import struct


def write_format_file(src_file, dst_file, src_as_dst: bool = False, has_schema: bool = True):
    positions = []
    i = 0
    meta_pos = 0
    with open(src_file, 'r') as f_src:
        with open(dst_file, 'wb') as f_dst:
            while True:
                line = f_src.readline()
                if not line:
                    break
                if has_schema and i == 0:
                    f_dst.write(line.encode('utf-8'))
                    i += 1
                else:
                    positions.append(f_dst.tell())
                    f_dst.write(line.encode('utf-8'))
                    i += 1
                if i % 1000 == 0:
                    print(f"processed {i} lines\n")
            f_dst.write("\n".encode('utf-8'))   # 防止最后一行没有换行符
            meta_pos = f_dst.tell()
            pos_str = ' '.join(str(pos) for pos in positions)

            f_dst.write(pos_str.encode('utf-8'))
            f_dst.write("\n".encode('utf-8'))
    f_src.close()
    f_dst.close()

    with open(dst_file, 'ab') as f:
        # 使用Q表示 64位 无符号长整型 ，0 到 18,446,744,073,709,551,615
        # print(f">>>> meta_pos:{meta_pos}")
        binary_num = struct.pack('Q', meta_pos)
        f.write(binary_num)
    f.close()


def read_schema(file_path, read_format: str = 'rb'):
    with open(file_path, read_format) as f:
        # 第一行数据为schema
        schema = f.readline()
    f.close()
    return schema


def read_meta(file_path, read_format: str = 'rb'):
    meta_pos = 0
    with open(file_path, read_format) as f:
        # 调整文件指针的位置，从文件末尾读取8个字节（uint64）, 找到meta信息所在的文件位置
        f.seek(-8, 2)
        binary_num = f.read(8)
        meta_pos = struct.unpack('Q', binary_num)[0]
        print(f">>>> read meta, meta_pos:{meta_pos}")
        f.seek(meta_pos)
        line = f.readline()
        # print(f"position line:{line}")
        positions = [int(pos) for pos in line.split()]
    f.close()
    return positions


def read_file_with_handler_and_position(file_hanlder, position:int):
    file_hanlder.seek(position)
    return file_hanlder.readline()


def read_file_with_position(file_path, position: int, read_format: str = 'rb'):
    with open(file_path, read_format) as f:
        f.seek(position)
        if f is not None:
            line = f.readline()
    f.close()
    return line
