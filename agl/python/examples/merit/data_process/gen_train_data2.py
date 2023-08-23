import sys
import numpy as np

# read node graph feature
i = 0
train_output = ""
test_output = ""
is_first_line = True
for line in open("wiki_data.csv"):
    if is_first_line:
        is_first_line = False
        continue
    parts = line.rstrip("\n").split(",")

    graph_feature = parts[1]

    if i <= 100:
        train_output += "{},{},{},{},{}\n".format(1, 2, graph_feature, 3, i % 2)
    elif i <= 200:
        test_output += "{},{},{},{},{}\n".format(1, 2, graph_feature, 3, i % 2)
    else:
        break

    i += 1

with open("wiki_train.csv", "w") as f:
    f.write(train_output)

with open("wiki_test.csv", "w") as f:
    f.write(test_output)
