## IO and Dataset

Assuming you have completed the tutorial on generating graph samples, we will introduce how to read those samples.

Note those graph samples are stored in CSV format now.
Each sample consists of several columns, with one column storing the serialized GraphFeature. Other columns may include
sample IDs, labels, or sample-level features.

Based on PyTorch, AGL provides two simple datasets for reading these CSV files and constructing the required
training/validation/testing datasets for the model.

Assuming you have built a PPI training dataset named ppi_train.csv, you can read this file as follows:

* [AGLTorchMapBasedDataset](../../../agl/python/dataset/map_based_dataset.py) （map-style dataset）

   ```python
    from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
    train_data_set = AGLTorchMapBasedDataset("/your_path_to/ppi_train.csv")
    print(train_data_set[0]) # the fist sample
   ```

* [AGLIterableDataset](../../../agl/python/dataset/iterable_dataset.py) （iterable-stype dataset）

  Similar to AGLTorchMapBasedDataset mentioned above, but batch_size needs to be specified (do not set batch_size in
  dataloader)

    ```python
    from agl.python.dataset.iterable_dataset import AGLIterableDataset

    train_data_set = AGLIterableDataset(file="/your_path_to/ppi_train_.csv")
    for data in iterable_dataset:
        print(f"data : {data}")
        break
    ```

Without loss of generality, in the following examples, we will use AGLTorchMapBasedDataset as the data reading method.

