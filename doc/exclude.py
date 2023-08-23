excludes = """
examples/*
lib/*
data/subgraph/test_data/*
**/*_test.py
"""

modules = excludes.strip().split("\n")
modules = ["../agl/python/" + line for line in modules]
print(" ".join(modules))
