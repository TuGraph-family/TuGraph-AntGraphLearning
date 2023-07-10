#ifndef AGL_THREAD_POOL_H
#define AGL_THREAD_POOL_H
#include <boost/asio/post.hpp>
#include <boost/asio/thread_pool.hpp>
#include <cassert>
#include <memory>

#include "safe_check.h"

namespace agl {
class ThreadPool {
 public:
  explicit ThreadPool(int number_thread) : pool_(number_thread), closed(false) {
    assert(number_thread > 0);
    // LOG(VERBOSE) << "Create ThreadPool. number_thread: " << number_thread;
  }
  virtual ~ThreadPool() { pool_.stop(); }
  template <class Function>
  void AddTask(Function function) {
    AGL_CHECK(!closed.load()) << "AddTask fail. ThreadPool is closed";
    boost::asio::post(pool_, function);
  }

  //不再接受新的任务，并等待已有任务结束
  void CloseAndJoin() {
    closed = true;
    pool_.join();
  };

 private:
  boost::asio::thread_pool pool_;
  std::atomic<bool> closed;
};

void Shard(const std::function<void(const int64_t, const int64_t)>& func,
           int64_t total_num, int64_t shard_num, int64_t min_count);
}

#endif  // AGL_THREAD_POOL_H
