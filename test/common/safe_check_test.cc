#include <string>

#include "common/safe_check.h"
#include "gtest/gtest.h"
using namespace agl;
using namespace std;

TEST(SAFE_CHECK_TEST, TEST_USAGE) {
  AGL_CHECK(true);
  AGL_CHECK_EQUAL(true, true);
  AGL_CHECK_NOT_EQUAL(true, false);
  AGL_CHECK_LESS_THAN(1, 2);
  AGL_CHECK_LESS_EQUAL(1, 1);
  AGL_CHECK_LESS_EQUAL(1, 2);
  AGL_CHECK_GREAT_EQUAL(2, 2);
  AGL_CHECK_GREAT_EQUAL(4, 2);
  AGL_CHECK_GREAT_THAN(4, 2);

  EXPECT_THROW(AGL_CHECK(false), string);
  EXPECT_THROW(AGL_CHECK_EQUAL(1.0, 2.0), string);
  EXPECT_THROW(AGL_CHECK_NOT_EQUAL(1.0, 1.0), string);
  EXPECT_THROW(AGL_CHECK_GREAT_THAN(1.0, 2.0), string);
  EXPECT_THROW(AGL_CHECK_GREAT_EQUAL(1.0, 2.0), string);
  EXPECT_THROW(AGL_CHECK_LESS_EQUAL(3.0, 2.0), string);
  EXPECT_THROW(AGL_CHECK_LESS_THAN(3.0, 2.0), string);
}