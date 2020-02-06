package ch.epfl.biop.imaris.demo;

import net.imagej.ImageJ;

public class IJSimpleLaunch {

    public static void main(String... args) {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }
}