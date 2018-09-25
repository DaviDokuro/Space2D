# Universe-Sandbox
### A simulation of 2D gravitational bodies written in java. Uses LWJGL Libraries
<sub>(working title (mostly tongue in cheek))</sub>

I made this project many years ago, but have always found myself coming back to it. 
I just have so much fun makin my computer try to handle absurd amounts of math, 
while making the results as accurate and efficient as I possibly can.

Lately, this project has become an exercize in optimization. Feel free to clone the repo to come up with your own sorts of optimizations.

## Using 

If you want to play with this project, just clone the repo to your eclipse workspace. Thats pretty much most of the work done. 

The one additional step is to configure the natives folder for the lwjgl jar file. 
To do this, just right click on the jar file and configure the buld path. 
Just select the natives for your OS (win or lin) and apply them to the lwjgl jar file.

Once you've done this, you're done. You can just press the run button and it will run (probably. like 99% sure).

## Configuring

There are a bunch of configurable variables at the top of the code, listed below with their default values. 

    int FPSCAP = 60 
    int FRAMEWIDTH = 1920
    int FRAMEHEIGHT = 1080
    int FSWIDTH = 3840
    int FSHEIGHT  = 2160
    int FRAMESKIP = 4
    int RUNTIME = 40000
    int THREADCOUNT = 8
    boolean SCREENCAP = false
    boolean RENDERLIMIT = false
    String screenshotFolder = "D:/Phys Sim/run1/"

* FPSCAP - The maximum framerate allowed to be displayed, 60 for a 60hz monitor. Fun to set insanely high frametargets.
* FRAMEWIDTH - The horizontal windowed frame resolution, 1920 for a 1080p window.
* FRAMEHEIGHT - The vertical windowed frame resolution, 1080 for a 1080p window.
* FSWIDTH - The horizontal fullscreen frame resolution, 3840 for UHD.
* FSHEIGHT - The vertical fullscreen frame resolution, 2160 for UHD.
* FRAMESKIP - The number of frames to skip when recording. When simulating at low speeds, changing this value can save a lot of drive space.
* RUNTIME - The number of steps to calculate before closing the application. Only matters when RENDERLIMIT is set to true. RUNTIME/FRAMESKIP will tell you how many screenshots you will have when the simulation is done.
* THREADCOUNT - The number of processor threads you have available. I have a quad-core with 8 threads.
* SCREENCAP - Boolean value to determine if screenshots will be taken or not.
* RENDERLIMIT - Boolean value to determine if application will be closed after RUNTIME is reached.
* screenshotFolder - Path to where you want screenshots saved.
