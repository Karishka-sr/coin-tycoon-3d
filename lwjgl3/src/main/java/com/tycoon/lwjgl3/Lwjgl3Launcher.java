package com.tycoon.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.tycoon.TycoonGame;

/** Desktop entry point. */
public final class Lwjgl3Launcher {

    private Lwjgl3Launcher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Coin Tycoon 3D");
        configuration.setWindowedMode(1280, 720);
        configuration.useVsync(true);
        configuration.setForegroundFPS(120);
        configuration.setBackBufferConfig(8, 8, 8, 8, 16, 0, 0);
        new Lwjgl3Application(new TycoonGame(), configuration);
    }
}
