/*-
 * #%L
 * API and commands to facilitate communication between Imaris and FIJI
 * %%
 * Copyright (C) 2020 - 2021 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package ch.epfl.biop.imaris.demo;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;

// TODO: Consider removing or adding to EasyXT directly as a means of debugging
public class IJSimpleLaunch {

    public static void main(String... args) throws ScriptException, FileNotFoundException {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        //String script_path = "D:/github/github_BIOP/EasyXT-Fiji/EasyXT/scripts/LabelAndTracks.groovy";
        //ij.script().run(new File(script_path) , true);

    }
}
