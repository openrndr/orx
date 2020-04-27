#/bin/bash
xvfb-run -e /dev/stdout -a java "$@"
