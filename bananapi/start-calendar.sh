#!/bin/bash

URL="https://thorondor.fr"

DISPLAY=:0 XAUTHORITY=/home/kiosk/.Xauthority unclutter -idle 1 -root &

while true; do
    DISPLAY=:0 XAUTHORITY=/home/kiosk/.Xauthority chromium \
        --kiosk \
        --no-first-run \
        --user-data-dir=/home/kiosk/.config/chromium \
        --disable-gpu \
        --use-gl=angle \
        --use-angle=swiftshader \
        --enable-unsafe-swiftshader \
        --disable-gpu-sandbox \
        "$URL"

    sleep 3
done
