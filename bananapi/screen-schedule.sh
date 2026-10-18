#!/bin/bash

export DISPLAY=:0
export XAUTHORITY=/home/kiosk/.Xauthority

OFF="23:00"
ON="07:00"
STATE=""

while true; do
    NOW_STR=$(date +%H%M)
    NOW=$((10#$NOW_STR))
    OFF_NUM=$((10#${OFF//:/}))
    ON_NUM=$((10#${ON//:/}))

    if (( OFF_NUM == ON_NUM )); then
        # Equal times: keep the screen on all day
        NEW_STATE="on"
    elif (( OFF_NUM < ON_NUM )); then
        # Same-day off period, e.g. 18:55 to 18:58
        if (( NOW >= OFF_NUM && NOW < ON_NUM )); then
            NEW_STATE="off"
        else
            NEW_STATE="on"
        fi
    else
        # Overnight off period, e.g. 22:00 to 07:00
        if (( NOW >= OFF_NUM || NOW < ON_NUM )); then
            NEW_STATE="off"
        else
            NEW_STATE="on"
        fi
    fi

    if [[ "$NEW_STATE" != "$STATE" ]]; then
        if [[ "$NEW_STATE" == "off" ]]; then
            xset dpms force off
        else
            xset dpms force on
        fi
        STATE="$NEW_STATE"
    fi

    sleep 30
done
