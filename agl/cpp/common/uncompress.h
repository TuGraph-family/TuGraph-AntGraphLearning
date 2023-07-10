#ifndef AGL_UNCOMPRESS_H
#define AGL_UNCOMPRESS_H
#include <zlib.h>
#include <iomanip>
#include <iostream>
#include <sstream>
#include <stdexcept>
#include <string>

namespace agl {
#define CHUNK 16384

inline bool uncompress(const std::string& input, std::string& output) {
  int ret;
  unsigned have;
  z_stream strm;
  unsigned char out[CHUNK];

  strm.zalloc = Z_NULL;
  strm.zfree = Z_NULL;
  strm.opaque = Z_NULL;
  strm.avail_in = 0;
  strm.next_in = Z_NULL;
  if (inflateInit2(&strm, 16 + MAX_WBITS) != Z_OK) {
    return false;
  }

  strm.avail_in = input.size();
  strm.next_in = (unsigned char*)input.c_str();
  do {
    strm.avail_out = CHUNK;
    strm.next_out = out;
    ret = inflate(&strm, Z_NO_FLUSH);
    switch (ret) {
      case Z_NEED_DICT:
      case Z_DATA_ERROR:
      case Z_MEM_ERROR:
        inflateEnd(&strm);
        return false;
    }
    have = CHUNK - strm.avail_out;
    output.append((char*)out, have);
  } while (strm.avail_out == 0);

  if (inflateEnd(&strm) != Z_OK) {
    return false;
  }

  return true;
}
}


#endif  // AGL_UNCOMPRESS_H
