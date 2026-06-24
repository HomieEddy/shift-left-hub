#!/bin/sh
# Docker entrypoint: ensure runtime cache dirs are writable by appuser
# (handles EACCES on host-mounted volumes where root owns the dir from
# the original image build), then drop privileges and run the dev server.

set -e

# Fix ownership of any cache dirs that may have been created as root
# (silent if path doesn't exist)
mkdir -p /app/.angular /app/.angular/cache /app/.vite /tmp/.ng-cache 2>/dev/null || true
chown -R appuser:appgroup /app/.angular /app/.vite /tmp/.ng-cache 2>/dev/null || true

# Drop privileges to appuser and exec the actual command using gosu
# (Alpine's su requires suid bit which is stripped by Docker, and BusyBox
# setpriv lacks --reuid/--regid support)
exec gosu appuser "$@"
