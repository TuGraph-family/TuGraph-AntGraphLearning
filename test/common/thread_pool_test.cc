#include "common/thread_pool.h"
#include "gtest/gtest.h"
#include <functional>


using namespace std;
using namespace agl;
void MyAdd(atomic<int> *sum, int add) { sum->fetch_add(add) ; }
TEST(THREAD_POOL_TEST, TEST_USAGE) {
  ThreadPool pool(2);
  atomic<int> sum(0);
  pool.AddTask(std::bind(MyAdd, &sum, 1));
  pool.CloseAndJoin();
  EXPECT_EQ(sum.load(), 1);
}

TEST(SHARD_TEST, TEST_USAGE) {
  atomic<int> total(0);
  atomic<int> actual_shard(0);
  const auto func = [&total, &actual_shard](int64_t start, int64_t end) {
    actual_shard.fetch_add(1);
    for (int64_t i = start; i < end; i++) {
      total.fetch_add(1);
    }
  };

  Shard(func, 1000, 10, 1);
  EXPECT_EQ(total.load(), 1000);
  EXPECT_EQ(actual_shard.load(), 10);
}