#/bin/bash
xvfb-run -e /dev/stdout java "$@"
