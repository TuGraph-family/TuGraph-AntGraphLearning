#include "thread_pool.h"

#include <algorithm>

#include "safe_check.h"

namespace agl{
void Shard(const std::function<void(const int64_t, const int64_t)>& func,
           int64_t total_num, int64_t shard_num, int64_t min_count) {
  AGL_CHECK_GREAT_EQUAL(min_count, 0)
      << ". total_num:" << total_num << "shard_num:" << shard_num;
  // Update shard_num by min_count
  int64_t even = std::max(min_count, total_num / shard_num);
  int64_t new_shard_num = std::max(static_cast<int64_t>(1), total_num / even);
  // todo add log
  /*if (new_shard_num != shard_num) {
    LOG(VERBOSE) << "ShardNum:" << shard_num << "->" << new_shard_num
                 << ", total_num:" << total_num << ", shard_num:" << shard_num
                 << ", min_count:" << min_count;
  }*/

  even = total_num / new_shard_num;
  int64_t residue = total_num % new_shard_num;
  auto offset = [even, residue](int64_t shard_id) {
    return even * shard_id + std::min(shard_id, residue);
  };

  if (new_shard_num == 1) {
    func(0, total_num);
  } else {
    ThreadPool pool(new_shard_num);
    for (int i = 0; i < new_shard_num; i++) {
      pool.AddTask(
          [&func, &offset, i]() { return func(offset(i), offset(i + 1)); });
    }
    pool.CloseAndJoin();
  }
}
}
