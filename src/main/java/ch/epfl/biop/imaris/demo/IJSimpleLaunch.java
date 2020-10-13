package ch.epfl.biop.imaris.demo;

import net.imagej.ImageJ;

// TODO: Consider removing or adding to EasyXT directly as a means of debugging
public class IJSimpleLaunch {

    public static void main(String... args) {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }
}