# How to setup the HDMI display with a BananaPi M2-zero

## Preparation of the OS
Download the image : 

```sh
pc$ wget https://dl.armbian.com/bananapim2zero/Trixie_current_minimal
```

Then flash on the SD card : 
```sh
# change the disk for you sd card (check for `lsblk`)
pc$ xz -dc Trixie_current_minimal | sudo dd of=/dev/sda bs=4M status=progress conv=fsync
pc$ sync
```

Plug the SD card in the BananaPi with HDMI and PC connection (USB).
Power the card and you should see the board booting.

In a new shell run : 
```sh
pc$ sudo minicom -D /dev/ttyACM0
```

You have now access to the tty, so you can start the configuration of the users and locals.
When its done you will arrived on a terminal and run `armbian-config` to configure the wifi connection.

When you are connected to internet : 
```sh
bananapi$ sudo apt update
bananapi$ sudo apt full-upgrade
bananapi$ sudo reboot
```

---

## SSH

to check

---

## Install the graphical kiosk components

Check board setup : 
```sh
bananapi$ dpkg --print-architecture
bananapi$ free -h
```

Install chromium : 
```sh
bananapi$ sudo apt update
bananapi$ apt-cache policy chromium
```

Install display tools : 
```sh
bananapi$ sudo apt install xserver-xorg xinit openbox chromium x11-xserver-utils x11-utils xserver-xorg-legacy python3-xdg vim unclutter
```

Then : 
```sh
bananapi$ sudo adduser kiosk
bananapi$ sudo usermod -aG video,audio,render kiosk

# With kiosk user
bananapi$ sudo dpkg-reconfigure xserver-xorg-legacy
```

---

## Configure automatic fullscreen startup

```sh
bananapi$ sudo -u kiosk mkdir -p /home/kiosk/.config/openbox
bananapi$ sudo -u kiosk mkdir -p /home/kiosk/bin
```

Copy the file start-calendar to the BananaPi : 
```sh
pc$ scp start-calendar.sh kiosk@192.168.1.40:/home/kiosk/bin/start-calendar.sh
# On the BananaPi
bananapi$ sudo chmod +x /home/kiosk/bin/start-calendar.sh
```

Same for `autostart` and `.xinitrc` files : 
```sh
pc$ scp autostart kiosk@192.168.1.40:/home/kiosk/.config/openbox/autostart
pc$ scp .xinitrc kiosk@192.168.1.40:/home/kiosk/.xinitrc
```

Set ownership : 
```sh
bananapi$ sudo chmod +x /home/kiosk/.xinitrc
bananapi$ sudo chown -R kiosk:kiosk /home/kiosk
```

Now you can test if it work bu running the .sh file.

---

## Automatically start the kiosk after power-on

Edit the `.bash-profile` and add : 
```sh
if [ -z "$DISPLAY" ] && [ "$(tty)" = "/dev/tty1" ]; then
    exec startx
fi
bananapi$ sudo chown kiosk:kiosk /home/kiosk/.bash_profile
```

Auto login : 

```sh
bananapi$ sudo mkdir -p /etc/systemd/system/getty@tty1.service.d
bananapi$ sudo tee /etc/systemd/system/getty@tty1.service.d/override.conf > /dev/null <<'EOF'
[Service]
ExecStart=
ExecStart=-/sbin/agetty --autologin kiosk --noclear %I $TERM
EOF

bananapi$ sudo systemctl daemon-reload
```

Test the graphical session : 
```sh
bananapi$ sudo -u kiosk startx
```

---

## Turn the screen off at night

Replace `xset -dpms` with `xset +dpms` and `xset dpms 600 600 600` to `.xinitrc`.

```sh
pc$ scp screen-schedule.sh kiosk@192.168.1.40:/home/kiosk/bin/screen-schedule.sh
TODO change PERM

bananapi$ sudo chmod +x /home/kiosk/bin/screen-schedule.sh
bananapi$ sudo chown kiosk:kiosk /home/kiosk/bin/screen-schedule.sh
```

Then add it to `autostart` file.

## TODO login into app

