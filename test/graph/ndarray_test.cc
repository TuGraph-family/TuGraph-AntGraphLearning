//
// Created by zdl on 2023/4/28.
//

#include <iostream>
#include <memory>
#include <string>
#include <vector>

#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"
#include "gtest/gtest.h"

using namespace agl;
using namespace std;

TEST(NDARRAY_TEST, TEST_USAGE) {
  {
    int rows = 2;
    int cols = 2;
    auto nd_ptr = unique_ptr<NDArray>(new NDArray(rows, cols, AGLDType::FLOAT));
    EXPECT_EQ(nd_ptr->GetRowNumber(), rows);
    EXPECT_EQ(nd_ptr->GetColNumber(), cols);
    EXPECT_EQ(nd_ptr->GetDType(), GetDTypeFromT<float>());

    auto* flat_p = nd_ptr->Flat<float>();
    vector<float> gt{1, 2, 3, 4};
    vector<vector<float>> gt_2 {
        {1,2},
        {3,4}
    };
    for (size_t i = 0; i < gt.size(); ++i) {
      flat_p[i] = gt[i];
    }
    for (size_t i = 0; i < rows; ++i) {
      for (size_t j = 0; j < cols; ++j) {
        EXPECT_EQ(flat_p[i * cols + j], gt_2[i][j]);
      }
    }
//    auto view_ptr = nd_ptr->ToView<float>();
//    for (size_t i = 0; i < rows; ++i) {
//      for (size_t j = 0; j < cols; ++j) {
//        cout << "view[" << i << "][" << j << "]:" << (*view_ptr)[i][j]
//             << ", ground truth:" << gt[i * cols + j] << "\n";
//        EXPECT_EQ((*view_ptr)[i][j], gt[i * cols + j]);
//      }
//    }
  }
}

TEST(NDARRAY_TEST, TEST_EXCEPTION) {
  {
    int rows = 2;
    int cols = 2;
    auto nd_ptr = unique_ptr<NDArray>(new NDArray(rows, cols, AGLDType::FLOAT));
    EXPECT_THROW(nd_ptr->Flat<double>(), string);
  }
}