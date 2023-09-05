#include "safe_check.h"

#include <boost/stacktrace.hpp>
#include <ctime>
#include <iomanip>
#include <ostream>

namespace logging {
std::ostream& operator<<(std::ostream& out, const std::nullptr_t& n) {
  out << "nullptr";
  return out;
}
}  // namespace logging

namespace agl {
CheckLogger::CheckLogger(const char* fname, int line, const char* func_name)
    : fname_(fname), line_(line), func_name_(func_name) {}

CheckLogger::~CheckLogger() noexcept(false) {
  struct tm local_tm = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, NULL};
  time_t t = time(NULL);
  localtime_r(&t, &local_tm);
  std::stringstream ss;
  const char prev_fill = ss.fill('0');
  ss << std::setw(2) << local_tm.tm_mon + 1 << std::setw(2) << local_tm.tm_mday
     << ' ' << std::setw(2) << local_tm.tm_hour << ':' << std::setw(2)
     << local_tm.tm_min << ':' << std::setw(2) << local_tm.tm_sec;
  ss.fill(prev_fill);
  ss << ' ' << fname_ << ':' << line_ << "] " << '(' << func_name_ << ") ";
  ss << ostream_.str() << "\n" << boost::stacktrace::stacktrace();
  fprintf(stderr, "%s\n", ss.str().c_str());

  throw ss.str();  // keep
}
std::ostringstream& CheckLogger::ostream() { return ostream_; }

}