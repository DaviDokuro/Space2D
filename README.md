# Space2D
### A simulation of 2D gravitational bodies written in Java using LWJGL
<sub>(working title)</sub>

I made this project many years ago, but have always found myself coming back to it. 
I just have so much fun makin my computer try to handle absurd amounts of math, 
while making the results as accurate and efficient as I possibly can.

Lately, this project has become an exercize in optimization. Feel free to clone the repo to come up with your own sorts of optimizations.

Because this project is so old, it was made with LWJGL 2 libraries. I am considering porting it to LWJGL 3, but am not currently working on that port. As far as I'm concerned, LWJGL 2 is doing fine for this application; The rendering itself is the least intensive part.

## Using 
If you want to play with this project, just clone the repo to your eclipse workspace. Thats pretty much most of the work done. 

The one additional step is to configure the natives folder for the lwjgl jar file. 
To do this, just right click on the jar file and configure the buld path. 
Just select the natives for your OS (win or lin) and apply them to the lwjgl jar file.

Once you've done this, you're done. You can just press the run button and it will run (probably. like 99% sure).

## Configuring
There are a bunch of configurable variables at the top of the code, listed below with their default values. 

    int FRAMEWIDTH = 1920
    int FRAMEHEIGHT = 1080
    int FRAMEINTERVAL = 100
    int CAPTUREDFRAMES = 100000
    boolean VSYNC = true
    boolean SCREENCAP = false
    boolean COLLISION = true
    boolean MULTITHREADED = false
    double speed = 6 * Math.pow(10, 2)
    double scale = 1 * Math.pow(10, -8)
    String screenshotFolder = "D:/Phys Sim/"
    String projectName = "SolarSystem2"

* FRAMEWIDTH - The horizontal windowed frame resolution, 1920 for a 1080p window.
* FRAMEHEIGHT - The vertical windowed frame resolution, 1080 for a 1080p window.
* FRAMEINTERVAL - The number of frames to skip when recording. When simulating at low speeds, changing this value can save a lot of drive space. It can be thought of as a timelapse of your simulation. Setting this to 1 will take a screenshot every frame.
* CAPTUREDFRAMES - The number of frames to capture before disabling screencap. FRAMEINTERVAL * CAPTUREDFRAMES will tell you the runtime of the simulation.
* VSYNC - Syncronize framerate with display refresh rate. Enabled by default, can be toggled at runtime with F10.
* SCREENCAP - Boolean value to determine if screenshots will be taken or not.
* COLLISION - Boolean value to determine if the algorythm checks for collisions. I recently added a collision function that scales with speed, but it is a bit overzealous at high speeds. Use with caution.
* MULTITHREADED - Boolean to determine if it uses java's default thread management or to spawn a thread for each core. Can be toggled at runtime by pressing M. NOTE: Multithreading still has issues with frame pacing. It tends to stabalize after a while, but can sometimes take a minute or two. Best to enable multithreading when fps is below 10 or so.
* speed - The speed of simulation, in seconds per frame. Default value is 10 minutes per frame (600 seconds). Can be adjusted at runtime with + or - on the numpad.
* scale - The scale of the view, in pixels per meter. Can be changed at runtime with scroll wheel.
* screenshotFolder - Path to where you want screenshots saved.
* projectName - Folder to save images in. Images will be saved in screenshotFolder + projectName + "/", eg. D:/Phys Sim/SolarSystem2/

Don't limit yourself to just these though, change anything in the code if you think it will make it better. The world is your oyster.

## Controls
### Mouse
* Left Click - Spam-spawn stars at the cursor, 1 new star every frame
* Right Click - Drag to measure space, results output to console. It's pretty bad, but works.
* Middle Click - Drag to pan camera. If no middle click on your mouse, press V for same effect.
* Scroll - Zoom in and out at cursor
* Mouse Button 5 - Spawn 1 star at cursor
    
### Keyboard
* ESC Key - Close Application
* Arrow Keys - Pan the camera.
* V Key - Alternative camera-pan key if no middle click is available.
* X Key - Spam-spawn stars in the middle of the frame, fun when used with arrow keys.
* Z Key - Spam-spawn stars randomly across the entire screen
* Numpad+ - Increase simulation speed (reduces accuracy, but faster)
* Numpad- - Decrease simulation speed (increases accuracy, but slower)
* C Key - Spawn 1 star in middle of frame.
* S Key - Output number of stars to console (pretty useless now, but was useful while debugging).
* D Key - Spawn Solar System at cursor.
* F Key - Spawn 100 suns around the cursor.
* G Key - Spawn a galaxy at the cursor (based on milkyway).
* P Key - Pause simulation. You can still add stars, only physics and screenshots are paused.
* R Key - Resets simulation.
* M Key - Toggles between Single or Multithreaded
* F10 Key - Toggles V-Sync
* F11 Key - Toggles Fullscreen
