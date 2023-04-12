CWD=$(dirname $0)
CWD=$(
  cd "$CWD" || exit 1
  pwd
)

# https://stackoverflow.com/questions/3601515/how-to-check-if-a-variable-is-set-in-bash
if [[ -z "${PROTOC_BIN_DIR+x}" ]]; then
  echo "ERROR: plz set PROTOC_BIN_DIR" && exit 1
fi

pushd "$CWD" || exit 1
CPP_OUT="${CWD}/../cpp/proto"
PY_OUT="${CWD}/../python/proto"

test ! -d "$CPP_OUT" && mkdir -p "$PY_OUT"
test ! -d "$CPP_OUT" && mkdir -p "$CPP_OUT"

"$PROTOC_BIN_DIR"/protoc \
  --proto_path=./ \
  --cpp_out="$CPP_OUT" \
  --python_out="${PY_OUT}" \
  ./*.proto
popd || exit 1
