#    Copyright 2023 AntGroup CO., Ltd.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import argparse
import os
import re
import subprocess
import sys



def remove_empty_params(str):
    """Removes lines with an empty parameter from the given string."""

    lines = str.split("\n")
    new_lines = []
    for line in lines:
        if line and line.find('""') == -1:
            new_lines.append(line)

    return "\n".join(new_lines)


def _parse_args():
    parser = argparse.ArgumentParser(description="**Spark GNN Sampling**")

    # input output args
    parser.add_argument(
        "--input_node_table_name",
        dest="input_node_table_name",
        required=True,
        type=str,
        help="input node table name",
    )
    parser.add_argument(
        "--input_edge_table_name",
        dest="input_edge_table_name",
        required=True,
        type=str,
        help="input edge table name",
    )
    parser.add_argument(
        "--input_label_table_name",
        dest="input_label_table_name",
        default="",
        type=str,
        help="input label table name",
    )
    parser.add_argument(
        "--output_table_name_prefix",
        dest="output_table_name_prefix",
        required=True,
        type=str,
        help="output table name prefix",
    )
    parser.add_argument(
        "--mode",
        dest="mode",
        default="local",
        type=str,
        help="spark running mode",
    )
    parser.add_argument(
        "--jar_resource_path",
        dest="jar_resource_path",
        required=True,
        type=str,
        help="jar_resource_path",
    )

    parser.add_argument(
        "--neighbor_distance",
        dest="neighbor_distance",
        type=int,
        default=1,
        help="neighbor_distance",
    )
    parser.add_argument(
        "--index_metas", dest="index_metas", default="", type=str, help="index_metas"
    )
    parser.add_argument(
        "--filter_condition",
        dest="filter_condition",
        default="",
        type=str,
        help="filter_condition",
    )
    parser.add_argument(
        "--sample_condition",
        dest="sample_condition",
        required=True,
        type=str,
        help="sample_condition",
    )
    parser.add_argument(
        "--subgraph_spec",
        dest="subgraph_spec",
        required=True,
        type=str,
        help="subgraph_spec",
    )
    parser.add_argument(
        "--train_flag", dest="train_flag", default="", type=str, help="train_flag"
    )
    parser.add_argument(
        "--remove_edge_among_roots",
        dest="remove_edge_among_roots",
        default="",
        type=str,
        help="whether or not to remove edges among roots [true, false]",
    )
    parser.add_argument(
        "--store_id_in_subgraph",
        dest="store_id_in_subgraph",
        default="",
        type=str,
        help="whether or not to store id in subgraph [true, false]",
    )
    parser.add_argument(
        "--hegnn_mode", dest="hegnn_mode", default="", type=str, help="[two_end, path]"
    )
    parser.add_argument(
        "--label_level", dest="label_level", default="", type=str, help="label_level"
    )
    parser.add_argument(
        "--algorithm",
        default="",
        type=str,
        help="[node_level, link_level, graph_level, hegnn, pagnn, sgc]",
    )
    return parser.parse_known_args()


def get_abslute_path(filepath):
    if not filepath.startswith("/"):
        return os.getcwd() + "/" + filepath
    return filepath


def run(args):
    script_directory = os.path.dirname(os.path.abspath(sys.argv[0]))

    if not os.path.exists(args.jar_resource_path):
        print("please build java project first")
        subprocess.check_call(
            f"cd {script_directory}/../../java && pwd && mvn clean package -Dmaven.test.skip", shell=True
        )

    algorithm = args.algorithm
    if algorithm == "node_level":
        algorithm_main = "com.alipay.alps.flatv3.spark.NodeLevelSampling"
    elif algorithm == "hegnn":
        algorithm_main = "com.alipay.alps.flatv3.spark.HEGNN"
    elif algorithm == "sgc":
        algorithm_main = "com.alipay.alps.flatv3.spark.SGC"
    elif algorithm == "link_level":
        algorithm_main = "com.alipay.alps.flatv3.spark.LinkLevelSampling"
    elif algorithm == "link_merge" or algorithm == "pagnn" or algorithm == "kcan":
        algorithm_main = "com.alipay.alps.flatv3.spark.LinkLevelSamplingMerge"
    elif algorithm == "graph_level":
        algorithm_main = "com.alipay.alps.flatv3.spark.GraphLevelSampling"
    else:
        raise AttributeError("Not Support algorithm:" + algorithm)

    # create spark script from template.
    from string import Template
    with open(f"{script_directory}/run_spark_template.sh", "r") as reader:
        template = Template(reader.read())

    shell_script = template.substitute(
        algorithm=algorithm_main,
        jar_resource_path=args.jar_resource_path,
        mode=args.mode,
        neighbor_distance=args.neighbor_distance,
        edge_table=get_abslute_path(args.input_edge_table_name),
        label_table=get_abslute_path(args.input_label_table_name),
        node_table=get_abslute_path(args.input_node_table_name),
        output_table_name_prefix=get_abslute_path(args.output_table_name_prefix),
        subgraph_spec=args.subgraph_spec,
        index_metas=args.index_metas,
        filter_condition=args.filter_condition,
        sample_condition=args.sample_condition,
        remove_edge_among_roots=args.remove_edge_among_roots,
        store_id_in_subgraph=args.store_id_in_subgraph,
        train_flag=args.train_flag,
        hegnn_mode=args.hegnn_mode,
        label_level=args.label_level,
    )
    shell_script = remove_empty_params(shell_script)

    script_file = "./run_spark.sh"
    with open(script_file, "w") as f:
        f.write(shell_script)

    # start to run spark script
    _run_shell_template(script_file)

    print("Finish.")


def _run_shell_template(script_file):
    try:
        print("Running spark:\n%s" % script_file)
        subprocess.check_call(["sh", script_file], shell=False)
    except:
        print("spark fail. spark:\n%s" % script_file)
        raise


if __name__ == "__main__":
    args, _ = _parse_args()
    run(args)
