package galaxy.sim;

import static org.lwjgl.opengl.GL11.*;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.*;
import org.lwjgl.*;

public class Space2D {

	// USER CONFIGURABLE
	public static int FRAMEWIDTH = 1920, FRAMEHEIGHT = 1080, FRAMEINTERVAL = 100, CAPTUREDFRAMES = 10000;

	public static boolean VSYNC = false, SCREENCAP = false, COLLISION = true, MULTITHREADED = false;

	public static double speed = 1 * Math.pow(10, 3), scale = 1 * Math.pow(10, -8);

	private static String screenshotFolder = "D:/Phys Sim/";
	private static String projectName = "SolarSystem2";
	// End config

	static Camera camera = new Camera(FRAMEWIDTH / 2, FRAMEHEIGHT / 2);
	// The camera is at resolution scale. It will contain values typically in the
	// hundreds and thousands, unlike points of mass whose position contains values
	// of trillions and so on.

	public static double FPS;
	public static long frameTime;
	public static long FRAME = 0;
	
	
	public static int CORECOUNT = Runtime.getRuntime().availableProcessors();
	public static int THREADCOUNT = CORECOUNT;
	
	public static GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	public static int FSWIDTH = gd.getDisplayMode().getWidth();
	public static int FSHEIGHT = gd.getDisplayMode().getHeight();
	public static int refreshRate = gd.getDisplayMode().getRefreshRate();
	
	// Universal Constants and Measures
	public static double G = 6.673 * Math.pow(10, -11); // Newton meters Squared
														// per kg Squared
	public static double solarmass = 1.989 * Math.pow(10, 30); // kg
	public static double solarradius = 6.957 * Math.pow(10, 8); // meters
	public static double mwblackholemass = 8.2 * Math.pow(10, 36); // kg
	public static double mwblackholeradius = 2.25 * Math.pow(10, 10); // meters
	public static double radius_of_milky_way = 5 * Math.pow(10, 20); // meters
	public static double speed_of_light = 299792458; // meters per second

	boolean rulerStart = false;
	double rulerX = 0, rulerY = 0; // in meters

	static List<CelestialBody> universe = new ArrayList<CelestialBody>();

	static PhysicsThread[] threads = new PhysicsThread[THREADCOUNT];

	public boolean paused = false;

	public static void main(String[] args) {
		if (SCREENCAP) {
			File theDir = new File(screenshotFolder);

			// if the directory does not exist, create it
			if (!theDir.exists()) {

				try {
					theDir.mkdir();
				} catch (SecurityException se) {
					// handle it
				}
			}

			theDir = new File(screenshotFolder + projectName + "/");

			// if the directory does not exist, create it
			if (!theDir.exists()) {

				try {
					theDir.mkdir();
				} catch (SecurityException se) {
					// handle it
				}
			}
		}

		new Space2D();
	}

	public Space2D() {

		try {
			Display.setDisplayMode(new DisplayMode((int) FRAMEWIDTH, (int) FRAMEHEIGHT));
			Display.setTitle("Play with the universe!");
			Display.setResizable(true);
			Display.setVSyncEnabled(false);
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
		
		

		initOGL();
		spawnBalls();

		while (!Display.isCloseRequested()) {

			mouseaction();
			keypresses();
			render();
			Display.update();
			if (!paused) {
				if (FRAME % FRAMEINTERVAL == 0 && SCREENCAP) {
					screenShot();
				}

				FRAME++;

				if (FRAME > CAPTUREDFRAMES * FRAMEINTERVAL && !SCREENCAP) {
					SCREENCAP = false;
				}
				
				
				if (MULTITHREADED) {
					multithreadedComp();
				} else {
					regularComp();
				}
			}
			// sync display
			if (VSYNC)
				Display.sync((int) refreshRate);
			updateFPS();
			updateTitle();
		}

		close();
	}

	public void regularComp() {

		for (int i = 0; i < universe.size(); i++) {
			for (int j = i + 1; j < universe.size(); j++) {
				universe.get(i).attractedTo(universe.get(j));
			}
		}
		
		if (COLLISION) {
			for (int i = 0; i < universe.size(); i++) {
				for (int j = i + 1; j < universe.size(); j++) {
					universe.get(i).collidesWith(universe.get(j));
				}
			}
		}
		for (int i = 0; i < universe.size(); i++) {		
			if (universe.get(i).eaten) {
				universe.get(i).collisionUpdate();
				i -= 1;
			} else {
				universe.get(i).positionUpdate();
			}
		}

	}

	public void multithreadedComp() {

		for (int i = 0; i < THREADCOUNT; i++) {
			threads[i] = new GravityThread("Thread" + i, i);
			threads[i].start();
		}
		for (int i = 0; i < THREADCOUNT; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (COLLISION) {
			for (int i = 0; i < THREADCOUNT; i++) {
				threads[i] = new CollisionThread("Thread" + i, i);
				threads[i].start();
			}
			for (int i = 0; i < THREADCOUNT; i++) {
				try {
					threads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}			
		}
		
		for (int i = 0; i < universe.size(); i++) {		
			if (universe.get(i).eaten) {
				universe.get(i).collisionUpdate();
				i -= 1;
			} else {
				universe.get(i).positionUpdate();
			}
		}

	}

	public void updateFPS() {

		long difference = (System.nanoTime() - frameTime) / 1000;

		FPS = 1000000.0 / difference;
		frameTime = System.nanoTime();

	}

	public void updateTitle() {
		Display.setTitle(Display.getWidth() + "x" + Display.getHeight() + "  |  Dots: " + universe.size()
				+ "  |  Scale: " + String.format("%6.3e", 1 / scale) + " m/px  |  Speed: "
				+ String.format("%6.3e", speed) + " s/f  |  " + String.format("%6.3e", speed * FPS)
				+ " x Realtime  |  FPS: " + String.format("%.2f", FPS));
	}

	// mouse inputs
	public void mouseaction() {
		if (Mouse.isButtonDown(0)) { // add stars of random color at the cursor
										// with 0 velocity
			universe.add(new CelestialBody((Mouse.getX() - camera.x) / scale, (Mouse.getY() - camera.y) / scale, 0,
					2 * Math.random() * Math.PI, solarmass, Math.random(), (Math.random() + 1) * .5,
					(Math.random() + 1) * .5, solarradius));
		}

		if (Mouse.isButtonDown(2)) { // pan screen with mouse
			camera.pan(Mouse.getDX(), Mouse.getDY());
		}

		if (Mouse.isButtonDown(3)) {

		}

		if (Mouse.isButtonDown(1)) {
			ruler((Mouse.getX() - camera.x) / scale, (Mouse.getY() - camera.y) / scale);
		} else if (rulerStart) {
			rulerStart = false;
		}

		while (Mouse.next()) {
			if (Mouse.isButtonDown(4) && Mouse.getEventButton() == 4) {
				universe.add(new CelestialBody((Mouse.getX() - camera.x) / scale, (Mouse.getY() - camera.y) / scale,
						0 * Math.random(), 2 * Math.random() * Math.PI, solarmass, Math.random(),
						(Math.random() + 1) * .5, (Math.random() + 1) * .5, solarradius));
			}
		}

		int mouseWheel = Mouse.getDWheel() / 120;
		if (mouseWheel != 0) {
			double mouseXPre = (Mouse.getX() - camera.x) / scale;
			double mouseYPre = (Mouse.getY() - camera.y) / scale;

			// fixed to 0,0 by default
			scale *= Math.pow(1.1, mouseWheel);

			double camPanX = (((Mouse.getX() - camera.x) / scale) - mouseXPre) * scale;
			double camPanY = (((Mouse.getY() - camera.y) / scale) - mouseYPre) * scale;

			// Fix to cursor target
			camera.pan(camPanX, camPanY);

		}
	}

	// keyboard inputs
	public void keypresses() {
		if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			close();
		}
		
		if (Keyboard.isKeyDown(Keyboard.KEY_X)) {
			universe.add(new CelestialBody(Display.getWidth() / (scale * 2) - (camera.x / scale),
					Display.getHeight() / (scale * 2) - (camera.y / scale), 0, 0, solarmass, Math.random(),
					Math.random(), Math.random(), solarradius));
		}
		
		if (Keyboard.isKeyDown(Keyboard.KEY_V)) {
			camera.pan(Mouse.getDX(), Mouse.getDY());
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			camera.x -= 10;
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			camera.x += 10;
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
			camera.y -= 10;
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			camera.y += 10;
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_ADD)) {
			speed *= 1 + 1.0 / (float) FPS;
		}
		
		if (Keyboard.isKeyDown(Keyboard.KEY_SUBTRACT)) {
			speed /= 1 + 1.0 / (float) FPS;
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_Z)) {
			universe.add(new CelestialBody(((Display.getWidth() * Math.random()) / scale) - (camera.x / scale),
					((Display.getHeight() * Math.random()) / scale) - (camera.y / scale), 0,
					2 * Math.random() * Math.PI, solarmass, Math.random(), Math.random(), Math.random(), solarradius));

		}
		
		while (Keyboard.next()) {
			if (Keyboard.getEventKey() == Keyboard.KEY_C && Keyboard.getEventKeyState()) {
				universe.add(new CelestialBody((Display.getWidth() / (2 * scale)) - (camera.x / scale),
						(Display.getHeight() / (2 * scale)) - (camera.y / scale), 0, 2 * Math.random() * Math.PI,
						solarmass, Math.random(), Math.random(), Math.random(), solarradius));
			}
			
			if (Keyboard.getEventKey() == Keyboard.KEY_S && Keyboard.getEventKeyState()) {
				System.out.println(universe.size());
			}

			if (Keyboard.getEventKey() == Keyboard.KEY_G && Keyboard.getEventKeyState()) {
				spawnGalaxy((Mouse.getX() - camera.x) / scale, (Mouse.getY() - camera.y) / scale);
			}

			if (Keyboard.getEventKey() == Keyboard.KEY_F && Keyboard.getEventKeyState()) {
				spawnCluster((Mouse.getX() - camera.x) / scale, (Mouse.getY() - camera.y) / scale);
			}

			if (Keyboard.getEventKey() == Keyboard.KEY_D && Keyboard.getEventKeyState()) {
				spawnSolarSystem((Mouse.getX() - camera.x) / scale, (Mouse.getY() - camera.y) / scale);
			}

			if (Keyboard.getEventKey() == Keyboard.KEY_P && Keyboard.getEventKeyState()) {
				paused = !paused;
			}
			
			if (Keyboard.getEventKey() == Keyboard.KEY_M && Keyboard.getEventKeyState()) {
				MULTITHREADED = !MULTITHREADED;
			}
			
			if (Keyboard.getEventKey() == Keyboard.KEY_R && Keyboard.getEventKeyState()) {
				universe = new ArrayList<CelestialBody>();
				spawnBalls();
			}
			
			if (Keyboard.getEventKey() == Keyboard.KEY_F10 && Keyboard.getEventKeyState()) {				
				VSYNC = !VSYNC;				
			}
			
			if (Keyboard.getEventKey() == Keyboard.KEY_F11 && Keyboard.getEventKeyState()) {
				
				if (Display.isFullscreen()) {
					setDisplayMode(FRAMEWIDTH, FRAMEHEIGHT, false);
					camera.pan((FRAMEWIDTH - FSWIDTH) / 2, (FRAMEHEIGHT - FSHEIGHT) / 2);
				} else {
					setDisplayMode(FSWIDTH, FSHEIGHT, true);
					camera.pan((FSWIDTH - FRAMEWIDTH) / 2, (FSHEIGHT - FRAMEHEIGHT) / 2);
				}
			}

		}
	}

	private void ruler(double x, double y) {
		if (!rulerStart) {
			rulerStart = true;
			rulerX = x;
			rulerY = y;
		} else {
			System.out.println(Math.sqrt(((rulerX - x) * (rulerX - x)) + ((rulerY - y) * (rulerY - y))) + " meters");
		}

	}

	public void spawnBalls() {

//		speed = 4 * Math.pow(10, 3);
//		scale = 5 * Math.pow(10, -10);
//		spawnSolarSystem(0, 0);

//		COLLISION = false;
//		speed = 1 * Math.pow(10, 14);
//		scale = 6 * Math.pow(10, -18);
//		spawnGalaxy(0, 0);

	}

	public void spawnCluster(double d, double e) {
		double starmass = solarmass;
		double radius = Math.pow(10, 12);
		double number = 100;

		double galaxy = Math.random();

		for (int i = 0; i < number; i++) {
			double phi = 2 * Math.random() * Math.PI;

			double r = Math.random() * radius;

			double dx = (r * Math.cos(phi));
			double dy = (r * Math.sin(phi));

			if (Math.random() > galaxy) {
				universe.add(new CelestialBody(d + dx, e + dy, 5, phi + (Math.PI / 2), starmass, 1,
						(Math.random() + 1) * .5, (Math.random() + 1) * .3, solarradius));
			} else {
				universe.add(new CelestialBody(d + dx, e + dy, 5, phi + (Math.PI / 2), starmass,
						(Math.random() + 1) * .3, (Math.random() + 1) * .5, 1, solarradius));
			}
		}
	}

	public void spawnGalaxy(double x, double y) {

		double number_of_stars = 10000;

		double red = 0;
		double green = 0;
		double blue = 0;

		double galaxy_type = (Math.random() + Math.random()) / 2;

		universe.add(new CelestialBody(x, y, 0, 0, mwblackholemass, 0.1, 0.1, 0.1, mwblackholeradius));

		int count = 0;
		while (count < number_of_stars) {
			double phi = 2 * Math.random() * Math.PI;

			double r = Math.pow(10, 19) + Math.random() * radius_of_milky_way / 8;

			double dx = (r * Math.cos(phi));
			double dy = (r * Math.sin(phi));

			double thisStarMass = solarmass * 100 * Math.random();

			double blackgrav = Math.sqrt((G * (mwblackholemass + thisStarMass)) / (r));

			if (Math.random() > galaxy_type) {
				red = 1;
				green = (Math.random() + 1) * .5;
				blue = (Math.random() + 1) * .3;

			} else {
				red = (Math.random() + 1) * .3;
				green = (Math.random() + 1) * .5;
				blue = 1;
			}
			universe.add(new CelestialBody((x + dx), (y + dy), blackgrav, phi + (Math.PI / 2), thisStarMass, red, green,
					blue, solarradius));
			count++;
		}
	}

	public void spawnSolarSystem(double x, double y) {

		double planet[][] = new double[8][7]; // planets 1 through 8; 0=mass,
												// 1=velocity, 2=orbit radius,
												// 3=planet radius, 4=red, 5=green, 6=blue

		planet[0][0] = 3.285 * Math.pow(10, 23); // mercury
		planet[0][1] = 47360;
		planet[0][2] = 5.791 * Math.pow(10, 10);
		planet[0][3] = 2.4397 * Math.pow(10, 6);
		planet[0][4] = 117;
		planet[0][5] = 115;
		planet[0][6] = 117;

		planet[1][0] = 4.867 * Math.pow(10, 24); // venus
		planet[1][1] = 35020;
		planet[1][2] = 1.0821 * Math.pow(10, 11);
		planet[1][3] = 6.0518 * Math.pow(10, 6);
		planet[1][4] = 123;
		planet[1][5] = 108;
		planet[1][6] = 86;

		planet[2][0] = 5.972 * Math.pow(10, 24); // earth
		planet[2][1] = 29784.73;
		planet[2][2] = 1.4959787 * Math.pow(10, 11);
		planet[2][3] = 6.371 * Math.pow(10, 6);
		planet[2][4] = 104;
		planet[2][5] = 111;
		planet[2][6] = 165;

		planet[3][0] = 6.39 * Math.pow(10, 23); // mars
		planet[3][1] = 24000;
		planet[3][2] = 2.28 * Math.pow(10, 11);
		planet[3][3] = 3.39 * Math.pow(10, 6);
		planet[3][4] = 160;
		planet[3][5] = 87;
		planet[3][6] = 21;

		planet[4][0] = 1.898 * Math.pow(10, 27); // jupiter
		planet[4][1] = 13100;
		planet[4][2] = 7.786 * Math.pow(10, 11);
		planet[4][3] = 6.9911 * Math.pow(10, 7);
		planet[4][4] = 213;
		planet[4][5] = 191;
		planet[4][6] = 161;

		planet[5][0] = 5.683 * Math.pow(10, 26); // saturn
		planet[5][1] = 9600;
		planet[5][2] = 1.433 * Math.pow(10, 12);
		planet[5][3] = 5.8232 * Math.pow(10, 7);
		planet[5][4] = 174;
		planet[5][5] = 155;
		planet[5][6] = 116;

		planet[6][0] = 8.681 * Math.pow(10, 25); // uranus
		planet[6][1] = 6800;
		planet[6][2] = 2.870658 * Math.pow(10, 12);
		planet[6][3] = 2.5362 * Math.pow(10, 7);
		planet[6][4] = 182;
		planet[6][5] = 219;
		planet[6][6] = 224;

		planet[7][0] = 1.024 * Math.pow(10, 26); // neptune
		planet[7][1] = 5400;
		planet[7][2] = 4.495 * Math.pow(10, 12);
		planet[7][3] = 2.4622 * Math.pow(10, 7);
		planet[7][4] = 62;
		planet[7][5] = 93;
		planet[7][6] = 226;

		// add moon?

		universe.add(new CelestialBody((x + planet[2][2] + 385000000.0), y, planet[2][1] + 1023.056, Math.PI / 2,
				7.34767309 * Math.pow(10, 22), 1, 1, 1, 1737000.0));

		// Add asteroid belt
		int count = 0;
		while (count < 2000) {
			double phi = 2 * Math.random() * Math.PI;

			double r = (3 * Math.pow(10, 11)) + (Math.random() * 5 * Math.pow(10, 11));

			double dx = (r * Math.cos(phi));
			double dy = (r * Math.sin(phi));

			double thisMass = 5 * Math.pow(10, 16) * Math.random();

			double gravA = Math.sqrt((G * (solarmass + thisMass)) / (r));

			double red = .5;
			double green = .3;
			double blue = .2;

			universe.add(new CelestialBody((x + dx), (y + dy), gravA, phi + (Math.PI / 2), thisMass, red, green, blue,
					50000));
			count++;
		}

		// Add Keiper belt
		count = 0;
		while (count < 100) {
			double phi = 2 * Math.random() * Math.PI;

			double r = (30 * planet[2][2]) + (Math.random() * 20 * planet[2][2]);

			double dx = (r * Math.cos(phi));
			double dy = (r * Math.sin(phi));

			double thisMass = Math.pow(10, 22);

			double gravA = Math.sqrt((G * (solarmass + thisMass)) / (r));

			double red = .5;
			double green = .4;
			double blue = .4;

			universe.add(new CelestialBody((x + dx), (y + dy), gravA, phi + (Math.PI / 2), thisMass, red, green, blue,
					50000));
			count++;
		}

		// add planets
		for (int i = 0; i < planet.length; i++) {
			universe.add(new CelestialBody((x + planet[i][2]), y, planet[i][1], Math.PI / 2, planet[i][0],
					planet[i][4] / 255, planet[i][5] / 255, planet[i][6] / 255, planet[i][3]));

		}

		// add sun
		universe.add(new CelestialBody(x, y, 16.2, 1.5 * Math.PI, solarmass, 1, 1, 0, solarradius));

	}

	// i got this screenshot code from stack overflow. It worked so well I had
	// no need to make my own.
	private void screenShot() {

		// Creating an rbg array of total pixels
		int[] pixels = new int[(int) (Display.getWidth() * Display.getHeight())];
		int bindex;
		// allocate space for RBG pixels
		ByteBuffer fb = ByteBuffer.allocateDirect((int) (Display.getWidth() * Display.getHeight() * 3));

		// grab a copy of the current frame contents as RGB
		glReadPixels(0, 0, (int) Display.getWidth(), (int) Display.getHeight(), GL_RGB, GL_UNSIGNED_BYTE, fb);

		BufferedImage imageIn = new BufferedImage((int) Display.getWidth(), (int) Display.getHeight(),
				BufferedImage.TYPE_INT_RGB);
		// convert RGB data in ByteBuffer to integer array
		for (int i = 0; i < pixels.length; i++) {
			bindex = i * 3;
			pixels[i] = ((fb.get(bindex) << 16)) + ((fb.get(bindex + 1) << 8)) + ((fb.get(bindex + 2) << 0));
		}
		// Allocate colored pixel to buffered Image
		imageIn.setRGB(0, 0, (int) Display.getWidth(), (int) Display.getHeight(), pixels, 0, (int) Display.getWidth());

		// Creating the transformation direction (horizontal)
		AffineTransform at = AffineTransform.getScaleInstance(1, -1);
		at.translate(0, -imageIn.getHeight(null));

		// Applying transformation
		AffineTransformOp opRotated = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		BufferedImage imageOut = opRotated.filter(imageIn, null);

		try {// Try to create image, else show exception.
			ImageIO.write(imageOut, "png", new File(
					screenshotFolder + projectName + "/" + "capture" + (int) (FRAME / FRAMEINTERVAL) + ".png"));
		} catch (Exception e) {
			System.out.println("ScreenShot() exception: " + e);
		}
	}

	// Found this code online, works but needs modification
	/**
	 * Set the display mode to be used
	 * 
	 * @param width      The width of the display required
	 * @param height     The height of the display required
	 * @param fullscreen True if we want fullscreen mode
	 */
	public void setDisplayMode(int width, int height, boolean fullscreen) {

		// return if requested DisplayMode is already set
		if ((Display.getDisplayMode().getWidth() == width) && (Display.getDisplayMode().getHeight() == height)
				&& (Display.isFullscreen() == fullscreen)) {
			return;
		}

		try {
			DisplayMode targetDisplayMode = null;

			if (fullscreen) {
				DisplayMode[] modes = Display.getAvailableDisplayModes();
				int freq = 0;

				for (int i = 0; i < modes.length; i++) {
					DisplayMode current = modes[i];

					if ((current.getWidth() == width) && (current.getHeight() == height)) {
						if ((targetDisplayMode == null) || (current.getFrequency() >= freq)) {
							if ((targetDisplayMode == null)
									|| (current.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel())) {
								targetDisplayMode = current;
								freq = targetDisplayMode.getFrequency();
							}
						}

						// if we've found a match for bpp and frequence against the
						// original display mode then it's probably best to go for this one
						// since it's most likely compatible with the monitor
						if ((current.getBitsPerPixel() == Display.getDesktopDisplayMode().getBitsPerPixel())
								&& (current.getFrequency() == Display.getDesktopDisplayMode().getFrequency())) {
							targetDisplayMode = current;
							break;
						}
					}
				}
			} else {
				targetDisplayMode = new DisplayMode(width, height);
			}

			if (targetDisplayMode == null) {
				System.out.println("Failed to find value mode: " + width + "x" + height + " fs=" + fullscreen);
				return;
			}

			Display.setDisplayMode(targetDisplayMode);
			Display.setFullscreen(fullscreen);
			glLoadIdentity();
			glOrtho(0, width, 0, height, 1, -1);
			glViewport(0, 0, width, height);

		} catch (LWJGLException e) {
			System.out.println("Unable to setup mode " + width + "x" + height + " fullscreen=" + fullscreen + e);
		}
	}

	private void render() {

		glClear(GL_COLOR_BUFFER_BIT);

		for (CelestialBody b : universe) {
			b.draw();
		}
	}

	private void initOGL() {

		// always here code OGL
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, FRAMEWIDTH, 0, FRAMEHEIGHT, 1, -1);
		glViewport(0, 0, FRAMEWIDTH, FRAMEHEIGHT);
	}

	private void close() {

		Display.destroy();
		System.exit(0);
	}

}

class CelestialBody {

	double drawAngle = 10;
	double drawAngleLOD = 45;

	private int SEGMENTS = (int) (360 / drawAngle);
	private int SEGMENTSLOD = (int) (360 / drawAngleLOD);

	public double x, y, vx, vy, dvx, dvy, m;
	private double colorRed, colorBlue, colorGreen;
	private double radius, minrad = 1;

	public boolean eaten = false;

	private double[][] ballPoints = new double[SEGMENTS][2];
	private double[][] ballPointsLOD = new double[SEGMENTSLOD][2];

	CelestialBody(double x, double y, double v, double theta, double mass, double red, double green, double blue,
			double radius) {
		this.x = x;
		this.y = y;
		this.vx = v * Math.cos(theta);
		this.vy = v * Math.sin(theta);
		this.dvx = 0;
		this.dvy = 0;
		this.m = mass;
		this.radius = radius;

		colorRed = red;
		colorGreen = green;
		colorBlue = blue;

		setDrawSize();
		setDrawSizeLOD();
	}

	boolean inClick(int mousex, int mousey) {
		double dx2 = (this.x - mousex) * (this.x - mousex);
		double dy2 = (this.y - mousey) * (this.y - mousey);
		if (Math.sqrt(dx2 + dy2) < this.radius) {
			return true;
		}
		return false;
	}

	public boolean checkTrec(CelestialBody that) {
		// add code to do something
		// hopefully to prevent garbage spewing everywhere

		double dx = (that.x - this.x);
		double dy = (that.y - this.y);

		double dx2 = dx * dx;
		double dy2 = dy * dy;

		double h = Math.sqrt((dx2 + dy2));

		if ((h <= this.radius + that.radius)) {
			return true;
		}

		return false;

	}

	public void collidesWith(CelestialBody that) {
		if (that != this) {
			if (!this.eaten && !that.eaten && checkTrec(that)) {
				if (this.m >= that.m) {
					this.dvx = ((this.m * this.vx) + (that.m * that.vx)) / (this.m + that.m);
					this.dvy = ((this.m * this.vy) + (that.m * that.vy)) / (this.m + that.m);
					this.m += that.m;
					
					this.radius = Math.cbrt((this.radius * this.radius * this.radius) + (that.radius * that.radius * that.radius));
					that.eaten = true;
					this.setDrawSize();
				} else {
					that.dvx = ((this.m * this.vx) + (that.m * that.vx)) / (this.m + that.m);
					that.dvy = ((this.m * this.vy) + (that.m * that.vy)) / (this.m + that.m);
					that.m += this.m;
					
					that.radius = Math.cbrt((this.radius * this.radius * this.radius) + (that.radius * that.radius * that.radius));
					this.eaten = true;
					that.setDrawSize();
				}
			}
		}
	}

	public void collidesWithMT(CelestialBody that) {
		if (that != this) {
			if (!this.eaten && !that.eaten && checkTrec(that)) {
				if (this.m >= that.m) {
					this.dvx = ((this.m * this.vx) + (that.m * that.vx)) / (this.m + that.m);
					this.dvy = ((this.m * this.vy) + (that.m * that.vy)) / (this.m + that.m);
					this.m += that.m;
					
					this.radius = Math.cbrt((this.radius * this.radius * this.radius) + (that.radius * that.radius * that.radius));
					that.eaten = true;
					this.setDrawSize();
				}
			}
		}
	}

	public void collisionUpdate() {
		if (eaten) {
			Space2D.universe.remove(this);
		}
	}

	public void attractedTo(CelestialBody that) {
		if (that != this) {
			if (that.dvx == 0 && that.dvy == 0) {
				that.dvx = that.vx;
				that.dvy = that.vy;
			}

			if (dvx == 0 && dvy == 0) {
				dvx = vx;
				dvy = vy;
			}

			double dx = (that.x - this.x);
			double dy = (that.y - this.y);

			double dx2 = dx * dx;
			double dy2 = dy * dy;

			gravityCalc(that, dx, dy, dx2 + dy2);

		}
	}

	public void gravityCalc(CelestialBody that, double dx, double dy, double r2) {
		double G = Space2D.G;

		double h = Math.sqrt(r2);

		double fx = (G * m * that.m * (dx / h)) / (r2);
		double fy = (G * m * that.m * (dy / h)) / (r2);

		that.dvx -= (fx * Space2D.speed) / (that.m);
		that.dvy -= (fy * Space2D.speed) / (that.m);

		this.dvx += (fx * Space2D.speed) / (this.m);
		this.dvy += (fy * Space2D.speed) / (this.m);

	}

	public void attractedToMT(CelestialBody that) {
		if (that != this) {

			if (dvx == 0 && dvy == 0) {
				dvx = vx;
				dvy = vy;
			}

			double dx = (that.x - this.x);
			double dy = (that.y - this.y);

			double dx2 = dx * dx;
			double dy2 = dy * dy;

			gravityCalcMT(that, dx, dy, dx2 + dy2);

		}
	}

	public void gravityCalcMT(CelestialBody that, double dx, double dy, double r2) {
		double G = Space2D.G;

		double h = Math.sqrt(r2);

		double fx = (G * m * that.m * (dx / h)) / (r2);
		double fy = (G * m * that.m * (dy / h)) / (r2);

		this.dvx += (fx * Space2D.speed) / (this.m);
		this.dvy += (fy * Space2D.speed) / (this.m);

	}

	public void positionUpdate() {
		vx = dvx;
		vy = dvy;
		dvx = dvy = 0;

		x += vx * Space2D.speed;
		y += vy * Space2D.speed;

	}

	private void setDrawSize() {
		for (int i = 0; i < SEGMENTS; i++) {
			ballPoints[i][0] = radius * Math.cos(Math.toRadians(i * drawAngle));
			ballPoints[i][1] = radius * Math.sin(Math.toRadians(i * drawAngle));
		}
	}

	private void setDrawSizeLOD() {
		for (int i = 0; i < SEGMENTSLOD; i++) {
			ballPointsLOD[i][0] = minrad * Math.cos(Math.toRadians(i * drawAngleLOD));
			ballPointsLOD[i][1] = minrad * Math.sin(Math.toRadians(i * drawAngleLOD));
		}
	}

	public void draw() {

		glColor3f((float) colorRed, (float) colorGreen, (float) colorBlue);

		double xToDraw = (x * Space2D.scale) + Space2D.camera.x;
		double yToDraw = (y * Space2D.scale) + Space2D.camera.y;

		double radiusToDraw = radius * Space2D.scale;

		if (xToDraw + radiusToDraw > 0 && xToDraw - radiusToDraw < Display.getWidth()
				&& yToDraw + radiusToDraw > 0
				&& yToDraw - radiusToDraw < Display.getHeight()) {

			if (radiusToDraw < minrad) {
				glBegin(GL_POLYGON);
				for (int i = 0; i < SEGMENTSLOD; i++) {
					glVertex2i((int) (xToDraw + ballPointsLOD[i][0]), (int) (yToDraw + ballPointsLOD[i][1]));
				}
				glEnd();
			} else {
				glBegin(GL_POLYGON);
				for (int i = 0; i < SEGMENTS; i++) {
					glVertex2i((int) (xToDraw + ballPoints[i][0] * Space2D.scale),
							(int) (yToDraw + ballPoints[i][1] * Space2D.scale));
				}
				glEnd();

			}
		}

	}
}

class Camera {
	double x, y;

	Camera(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void pan(double x, double y) {
		this.x += x;
		this.y += y;
	}
}

class PhysicsThread extends Thread {
	int thread;
	long starttime;

	public PhysicsThread(String name, int thread) {
		super(name);
		this.thread = thread;
		starttime = System.nanoTime();
		// System.out.println(thread + " " + startIndex() + " " + endIndex());
	}

	protected int startIndex() {
		return makeIndex(thread);

	}

	protected int endIndex() {
		return makeIndex(thread + 1);

	}

	private int makeIndex(int thread) {

		int top = (thread * Space2D.universe.size()) / (Space2D.THREADCOUNT);

		// double logging = Math.log(thread + 1) / Math.log(UniverseSandbox.THREADCOUNT
		// + 1);

		// return (int) (top * logging);

		return top;

	}

}

class CollisionThread extends PhysicsThread {

	public CollisionThread(String name, int thread) {
		super(name, thread);
	}

	@Override
	public void run() {
		int startpoint = startIndex();
		int endpoint = endIndex();
		for (int i = startpoint; i < endpoint; i++) {
			for (int j = 0; j < Space2D.universe.size(); j++) {
				Space2D.universe.get(i).collidesWithMT(Space2D.universe.get(j));
			}
		}
		// System.out.println(super.thread + " " + (System.nanoTime() -
		// super.starttime));
	}
}

class GravityThread extends PhysicsThread {

	public GravityThread(String name, int thread) {
		super(name, thread);
	}

	@Override
	public void run() {
		int startpoint = startIndex();
		int endpoint = endIndex();
		for (int i = startpoint; i < endpoint; i++) {
			for (int j = 0; j < Space2D.universe.size(); j++) {
				Space2D.universe.get(i).attractedToMT(Space2D.universe.get(j));
			}
		}
		// System.out.println(super.thread + " " + (System.nanoTime() -
		// super.starttime));
	}
}