#ifndef AGL_SAFE_CHECK_H
#define AGL_SAFE_CHECK_H
#include <cstddef>
#include <ostream>
#include <sstream>

namespace logging {
// we have EQ/NE with nullptr
std::ostream& operator<<(std::ostream& out, const std::nullptr_t& n);
}  // namespace logging

// AGL_CHECK:
// safe assert can be used in server to avoid std::assert making server crash.
// server will take the string error and pass it as error_message to client.

#ifdef USE_LIKELY
#define likely(x) (__builtin_expect(!!(x), 1))
#define unlikely(x) (__builtin_expect(x, 0))
#else
#define likely(x) (x)
#define unlikely(x) (x)
#endif

#define AGL_CHECK(condition)                                              \
  if (!likely(condition))                                                 \
  agl::CheckLogger(__FILE__, __LINE__, __FUNCTION__).ostream() \
      << "Check failed: " #condition " "
/*
#define AGL_CHECK_OP(name, op, val1, val2)                                \
  if (std::string* _result = ::logging::Check##name##Impl(                \
          (val1), (val2), #val1 " " #op " " #val2))                       \
  agl::CheckLogger(__FILE__, __LINE__, __FUNCTION__).ostream() \
      << "Check failed:" << *_result

#define AGL_CHECK_EQUAL(val1, val2) AGL_CHECK_OP(EQ, ==, val1, val2)
#define AGL_CHECK_NOT_EQUAL(val1, val2) AGL_CHECK_OP(NE, !=, val1, val2)
#define AGL_CHECK_GREAT_EQUAL(val1, val2) AGL_CHECK_OP(GE, >=, val1, val2)
#define AGL_CHECK_GREAT_THAN(val1, val2) AGL_CHECK_OP(GT, >, val1, val2)
#define AGL_CHECK_LESS_EQUAL(val1, val2) AGL_CHECK_OP(LE, <=, val1, val2)
#define AGL_CHECK_LESS_THAN(val1, val2) AGL_CHECK_OP(LT, <, val1, val2)*/

// todo 是否用上述的宏，还是使用AGL_CHECK 继续拧如下述的实现

#define AGL_CHECK_EQUAL(val1, val2) AGL_CHECK(val1 == val2)
#define AGL_CHECK_NOT_EQUAL(val1, val2) AGL_CHECK( val1 != val2)
#define AGL_CHECK_GREAT_EQUAL(val1, val2) AGL_CHECK( val1 >= val2)
#define AGL_CHECK_GREAT_THAN(val1, val2) AGL_CHECK(val1 > val2)
#define AGL_CHECK_LESS_EQUAL(val1, val2) AGL_CHECK( val1 <= val2)
#define AGL_CHECK_LESS_THAN(val1, val2) AGL_CHECK( val1 < val2)


namespace agl {
class CheckLogger {
 public:
  CheckLogger(const char* fname, int line, const char* func_name);
  ~CheckLogger() noexcept(false);

  std::ostringstream& ostream();

 private:
  const char* fname_;
  const char* func_name_;
  int line_;
  std::ostringstream ostream_;
};
}
#endif  // AGL_SAFE_CHECK_H
