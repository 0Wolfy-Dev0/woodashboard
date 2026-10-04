Using a Raspberry Pi is the perfect way to build a dedicated calendar display. To make this work seamlessly, you want to set the Pi up in Kiosk Mode. This forces the Raspberry Pi to boot straight into a fullscreen web browser pointing to your calendar URL, while hiding the mouse cursor and preventing the screen from going to sleep. [1, 2, 3, 4] 
## Step 1: Install the Operating System

   1. Download the [Raspberry Pi Imager](https://www.raspberrypi.com/software/) on a regular computer. [2] 
   2. Insert your microSD card into your computer.
   3. In the Imager, choose your Raspberry Pi model and select Raspberry Pi OS (64-bit) (the standard desktop version). [2] 
   4. Click the gear icon (or choose "Edit Settings") to configure OS customization:
   * Enable Wi-Fi and enter your home network details.
      * Set a username and password. [2, 5] 
   5. Write the OS to the microSD card, insert it into your Pi, plug in your monitor, and power it on. [2, 5] 

## Step 2: Configure Auto-Login
Your Pi needs to log into the desktop automatically without asking for a password when turned on. [3, 6] 

   1. On your Pi, open the terminal application.
   2. Open the configuration tool:
   
   sudo raspi-config
   
   3. Navigate to System Options -> Boot / Auto Login.
   4. Select Desktop Autologin.
   5. Select Finish and choose Yes to reboot. [3, 6] 

## Step 3: Create the Kiosk Startup Script
We will write a small automated script that hides the mouse cursor, prevents the screen from blanking, and launches Chromium in a dedicated kiosk mode. [3, 4] 

   1. Open the terminal and install unclutter (a tool that automatically hides your mouse cursor):
   
   sudo apt update && sudo apt install -y unclutter
   
   2. Create a new script file using the text editor:
   
   nano ~/run_kiosk.sh
   
   3. Copy and paste the following script into the editor (make sure to replace http://your-server-ip:port/calendar with the actual URL of your calendar webapp):
   
   #!/bin/bash# Hide the mouse cursor after 2 seconds of inactivity
   unclutter -idle 2 &
   # Prevent the screen from turning off or blanking
   xset s off
   xset s noblank
   xset -dpms
   # Launch Chromium in fullscreen kiosk mode
   chromium-browser --kiosk --noerrdialogs --disable-infobars --no-first-run "http://your-server-ip:port/calendar" &
   
   4. Save and exit by pressing Ctrl + X, then Y, and then Enter.
   5. Make the script executable by running: [3, 4, 7] 
   
   chmod +x ~/run_kiosk.sh
   
   
## Step 4: Make it Run on Boot
Now, tell the Raspberry Pi desktop environment to trigger your script as soon as it loads.

   1. Open the desktop autostart configuration file:
   
   nano ~/.config/lxsession/LXDE-pi/autostart
   
   (Note: If the file or folder doesn't exist, create it by running mkdir -p ~/.config/lxsession/LXDE-pi/ first).
   2. Add a line at the very bottom pointing to your script:
   
   @/home/YOUR_PI_USERNAME/run_kiosk.sh
   
   (Be sure to replace YOUR_PI_USERNAME with the username you created during setup, such as pi).
   3. Save and exit (Ctrl + X, Y, Enter). [3, 6, 7, 8] 

Once you restart your Raspberry Pi using sudo reboot, it will boot cleanly directly into your full-screen calendar website. [2, 3] 
To help tailor the next steps, tell me:

* 
* Does your calendar website require a login/password every time it opens?
* Do you want the screen to stay on 24/7, or would you like to set a schedule to turn the display off at night?
* 


[1] [https://www.raspberrypi.com](https://www.raspberrypi.com/tutorials/how-to-use-a-raspberry-pi-in-kiosk-mode/)
[2] [https://www.youtube.com](https://www.youtube.com/watch?v=16wLv9YpdN4&t=30)
[3] [https://core-electronics.com.au](https://core-electronics.com.au/videos/how-to-set-up-a-raspberry-pi-kiosk-launch-a-fullscreen-webpage-on-boot)
[4] [https://gist.github.com](https://gist.github.com/danawoodman/75d84b33c14445f2e08582aad609beb2)
[5] [https://www.youtube.com](https://www.youtube.com/watch?v=Anv3t1kgmUU&vl=en&t=597)
[6] [https://core-electronics.com.au](https://core-electronics.com.au/guides/raspberry-pi-kiosk-mode-setup/)
[7] [https://www.youtube.com](https://www.youtube.com/watch?v=_32QV9ntbUg)
[8] [https://forums.raspberrypi.com](https://forums.raspberrypi.com/viewtopic.php?t=219952)


To automatically schedule your calendar screen to turn off at night and wake up in the morning, you can use the built-in Linux scheduling tool called cron along with the display tool wlopm (Wayland Output Power Management). [1, 2]
Modern Raspberry Pi OS releases use a window system called Wayland (specifically the Labwc desktop environment). Old methods like vcgencmd or xset no longer work on recent Pi operating systems, so we will use the correct Wayland commands. [1, 3, 4, 5]
------------------------------
## Step 1: Install the Power Management Tool
Open the terminal on your Raspberry Pi and install wlopm: [1, 2]

sudo apt update
sudo apt install -y wlopm

## Step 2: Open the Schedule Configuration (Crontab)
The crontab file is where you save automated time-based tasks. Open it by typing:

crontab -e

If it asks you to pick an editor, press 1 for nano.
## Step 3: Add Your Schedule Rules
Scroll to the very bottom of the file and paste the following lines.
For this example, the screen will turn off at 10:00 PM (22:00) and turn back on at 7:00 AM (07:00) every day: [6]

# Turn the screen OFF at 10:00 PM (22:00)
0 22 * * * export WAYLAND_DISPLAY=wayland-1; export XDG_RUNTIME_DIR=/run/user/1000; wlopm --off \*

# Turn the screen ON at 7:00 AM (07:00)
0 7 * * * export WAYLAND_DISPLAY=wayland-1; export XDG_RUNTIME_DIR=/run/user/1000; wlopm --on \*

(Note: If you created a custom username instead of the default pi, make sure your user ID is 1000 by typing id -u in the terminal. 1000 is almost always the default first user). [3]
## Step 4: Save and Exit

   1. Press Ctrl + X to exit.
   2. Press Y to confirm saving the changes.
   3. Press Enter to finalize the file name.

You should see a message saying crontab: installing new crontab. Your Pi will now safely cut the video signal to your monitor at night, letting it go into its energy-saving sleep state, and wake it back up right before you start your day! [2, 6]
If you'd like to adjust this further, let me know:

*
* Do you want a different schedule for weekends (e.g., staying on later)?
* Would you like the display to also automatically dim or reduce brightness in the evening before completely turning off?
*


[1] [https://forums.raspberrypi.com](https://forums.raspberrypi.com/viewtopic.php?t=380383)
[2] [https://www.thedigitalpictureframe.com](https://www.thedigitalpictureframe.com/how-to-schedule-auto-dim-and-screen-standby-on-your-raspberry-pi-2-3-or-zero-2-w/)
[3] [https://forums.raspberrypi.com](https://forums.raspberrypi.com/viewtopic.php?t=381199)
[4] [https://forums.raspberrypi.com](https://forums.raspberrypi.com/viewtopic.php?t=361090)
[5] [https://www.thedigitalpictureframe.com](https://www.thedigitalpictureframe.com/how-to-automatically-turn-your-digital-picture-frame-on-and-off-at-fixed-times/)
[6] [https://www.reddit.com](https://www.reddit.com/r/raspberry_pi/comments/jyvj5q/heres_how_i_turn_my_monitors_on_and_off/)

