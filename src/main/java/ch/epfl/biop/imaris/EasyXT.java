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

package ch.epfl.biop.imaris;

import Ice.ObjectPrx;
import Imaris.Error;
import Imaris.*;
import com.bitplane.xt.IceClient;
import ij.*;
import ij.macro.Variable;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.process.*;
import inra.ijpb.label.LabelImages;
import mcib3d.geom.ObjectCreator3D;
import mcib3d.geom.Point3D;
import mcib3d.geom.Vector3D;
import mcib3d.image3d.ImageByte;
import mcib3d.image3d.ImageShort;
import net.imagej.ImageJ;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO: Detecting Spots and Surfaces, with tracking
// TODO: Mask Channel using spots or surfaces
// TODO: Find way to batch process OpenImage() with doc
// TODO: Make Documentation
// TODO: How to add autocomplete for EasyXT, Romain contacts Robert Haase

/**
 * Main EasyXT Static class
 *
 * @author Olivier Burri
 * @author Nicolas Chiaruttini
 * @author Romain Guiet
 * This is the main static class you should access when you want to interact with Imaris.
 */
public class EasyXT {

    // APP is not final because we reserve the right to restart the connection
    private static IApplicationPrx APP;
    private static final String M_END_POINTS = "default -p 4029";
    public static Map<tType, Integer> datatype;
    public static Logger log = Logger.getLogger(EasyXT.class.getName());
    private static IceClient mIceClient;

    /*
      Static initialisation :
      Gets the Imaris server
      Ensure the connection is closed on JVM closing
     */
    static {
        log.info("Initializing EasyXT");

        // Populate static map : Imaris Type -> Bit Depth
        Map<tType, Integer> tmap = new HashMap<>(4);
        tmap.put(tType.eTypeUInt8, 8);    // Unsigned integer, 8  bits : 0..255
        tmap.put(tType.eTypeUInt16, 16);  // Unsigned integer, 16 bits : 0..65535
        tmap.put(tType.eTypeFloat, 32);   // Float 32 bits
        tmap.put(tType.eTypeUnknown, -1); // ? Something else

        datatype = Collections.unmodifiableMap(tmap);

        // Get the client and get the instance of the Imaris Application, which is the starting point for all
        // Imaris related queries
        mIceClient = new IceClient("ImarisServer", M_END_POINTS, 10000);
        ObjectPrx potentialApp = mIceClient.GetServer().GetObject(0);
        APP = IApplicationPrxHelper.checkedCast(potentialApp);

        // Closing connection on jvm close
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    log.info("Closing ICE Connection from Imaris...");
                    CloseIceClient();
                    log.info("Done.");
                })

        );
        log.info("Initialization Done. Ready to work with EasyXT");
    }

    /**
     * Close the ICE client after using EasyXT
     */
    private static void CloseIceClient() {
        if (mIceClient != null) {
            log.info("Closing previous Imaris ICE connection...");
            mIceClient.Terminate();
            mIceClient = null;
            log.info("Imaris ICE connection closed.");

        }
    }

    /**
     * Main method for debugging EasyXT
     *
     * @param args optional parameters
     */
    public static void main(String... args) {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }

    /**
     * This subclass contains methods to open images in Imaris
     * There are also methods to find all the demo Images from Imaris
     */
    public static class Files {
        /**
         * overloaded method, see {@link #openImage(File, String)}
         *
         * @param filepath to an *.ims file
         * @throws Error an Imaris Error
         */
        public static void openImage(File filepath) throws Error {
            openImage(filepath, "");
        }

        /**
         * openImage, opens the file from filepath in a new imaris scene
         *
         * @param filepath path  to an *.ims file
         * @param options  option string cf : xtinterface/structImaris_1_1IApplication.html/FileOpen
         * @throws Error an Imaris Error
         */
        public static void openImage(File filepath, String options) throws Error {
            if (!filepath.exists()) {
                log.severe(filepath + "doesn't exist");
                throw new Error();
            }

            if (!filepath.isFile()) {
                log.severe(filepath + "is not a file");
                throw new Error();
            }

            if (!filepath.getName().endsWith("ims")) {
                log.severe(filepath + "is not an imaris file, please convert your image first");
                throw new Error();
            }

            // All sanity checks passed, open the file
            Utils.getImarisApp().FileOpen(filepath.getAbsolutePath(), options);
        }

        /**
         * overloaded method , see {@link #saveImage(File, String)}
         *
         * @param filepath path to save ims file
         * @throws Error an Imaris Error
         */
        public static void saveImage(File filepath) throws Error {
            saveImage(filepath, "");
        }

        /**
         * Saves the current imaris scene to an imaris file
         *
         * @param filepath path to save ims file
         * @param options  option string cf : xtinterface/structImaris_1_1IApplication.html/FileSave eg writer="BMPSeries".
         *                 List of formats available: Imaris5, Imaris3, Imaris2,SeriesAdjustable, TiffSeriesRGBA, ICS,
         *                 OlympusCellR, OmeXml, BMPSeries, MovieFromSlices.
         * @throws Error an Imaris Error
         */
        public static void saveImage(File filepath, String options) throws Error {
            if (!filepath.getName().endsWith("ims")) {
                filepath = new File(filepath.getAbsoluteFile() + ".ims");
                log.info("Saved as : " + filepath.getAbsoluteFile());
            }
            Utils.getImarisApp().FileSave(filepath.getAbsolutePath(), options);
        }

        /**
         * Gets the name of the currently open imaris file, as shown on the top of the Imaris window
         *
         * @return the name of the currently open imaris file
         * @throws Error an Imaris Error
         */
        public static String getOpenFileName() throws Error {
            return getOpenFile().getName();
        }

        /**
         * Get the path of the currently open Imaris file, as a File
         *
         * @return the File pointing to the currently open image
         * @throws Error an Imaris Error
         */
        public static File getOpenFile() throws Error {
            return new File(Utils.getImarisApp().GetCurrentFileName());
        }
    }

    /**
     * This Class takes care of downloading and using sample files
     */
    public static class Samples {

        // This is a url pointing to a sample dataset for EasyXT
        private static final String SAMPLE_IMAGE_URL = "https://zenodo.org/record/4449687/files/HeLa_H2BmCherry_GFPtubulin_Mitotracker.ims";

        // Default Demo Images Folder Name
        private static final String DEMO_IMAGES_FOLDER = "Imaris Demo Images";

        /**
         * returns a File pointing to a demo dataset that can be used with {@link EasyXT.Files#openImage(File)}
         * It will try to find the main Fiji folder, and place it in a "samples" subdirectory. It will not download it again if it is not present.
         *
         * @return the file that was either already local or downloaded
         */
        public static File getSampleFile() {

            URI sampleUri = URI.create(SAMPLE_IMAGE_URL);

            // Destination File Name
            String destName = new File(sampleUri.getPath()).getName();

            // Define the directory where we will store the sample image
            File samples_directory = new File(IJ.getDirectory("imagej"), "samples");

            // Create the directory if it does not exist
            samples_directory.mkdirs();

            // Define the local path to the sample file
            Path destPath = new File(samples_directory, destName).toPath();

            // If the file is not there, download it
            if (!destPath.toFile().exists()) {
                log.info("Sample file " + destPath.toFile().getAbsolutePath() + " does not exist, downloading...");
                log.info("From: " + sampleUri.toString());

                // To download the file, we open an InputStream to it and use Files.copy
                try (InputStream in = sampleUri.toURL().openStream()) {
                    java.nio.file.Files.copy(in, destPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Download complete, returning path to " + destPath.toFile().getName());
                    return destPath.toFile();

                } catch (IOException e) {
                    log.log(Level.SEVERE, "Error downloading " + destPath.toFile().getName(), e);
                    return null;
                }
            } else {
                log.info("Sample file " + destPath.toFile().getAbsolutePath() + " exists, no need to download");
                return destPath.toFile();
            }
        }

        /**
         * Allows you to get the path to any Imaris Demo image, by providing its full name (including extension
         *
         * @param imageName the name of the image, located in the "Imaris Demo Images" default location
         * @return the file to open using {@link Files#openImage(File)}
         * @throws Error an Imaris Error
         */
        public static File getImarisDemoFile(String imageName) throws Error {
            // Get user folder
            File userFolder = new File(System.getProperty("user.home"));
            File demoImageFolder = new File(userFolder, DEMO_IMAGES_FOLDER);
            if (!demoImageFolder.exists()) {
                log.log(Level.SEVERE, "No Imaris Demo Folder at '" + demoImageFolder.getAbsolutePath() + "'");
                throw new Error("Demo File Open Error", "No Imaris Demo Folder at '" + demoImageFolder.getAbsolutePath() + "'", "getImarisDemoImage");
            }
            // Get the selected image
            File selectedImage = new File(demoImageFolder, imageName);
            if (!selectedImage.exists()) {
                log.log(Level.SEVERE, "No Demo file named '" + selectedImage.getName() + "'");
                throw new Error("Demo File Open Error", "No Demo file named '" + selectedImage.getName() + "'", "getImarisDemoImage");
            }
            return selectedImage;
        }

        /**
         * get a list of all images in the default Imaris demo images folder
         *
         * @return a list of file names in that folder ending with .ims
         * @throws Error an Imaris Error
         */
        public static List<String> listAllImarisDemoImages() throws Error {
            File userFolder = new File(System.getProperty("user.home"));
            File demoImageFolder = new File(userFolder, DEMO_IMAGES_FOLDER);
            if (!demoImageFolder.exists()) {
                log.log(Level.SEVERE, "No Imaris Demo Folder at '" + demoImageFolder.getAbsolutePath() + "'");
                throw new Error("Demo File Open Error", "No Imaris Demo Folder at '" + demoImageFolder.getAbsolutePath() + "'", "getImarisDemoImage");
            }
            return Arrays.stream(demoImageFolder.list()).filter(f -> f.endsWith(".ims")).collect(Collectors.toList());
        }
    }

    /**
     * This class handles all queries to the surpass scene and to its related Items
     * Method to find spots, surfaces, groups, or arbitrary objects are found inside
     * Methods to get or set item names
     */
    public static class Scene {

        /**
         * Returns the name of the item, prepended by the name of all parent items
         * until we reach the main Surpass Scene.
         *
         * @param item the item whose name we need
         * @return the name of the item
         * @throws Error an Imaris Error Object
         */
        public static String getFullName(IDataItemPrx item) throws Error {
            // Make names recursive
            String finalName = item.GetName();

            IDataContainerPrx scene = getScene();
            IDataContainerPrx parent = item.GetParent();

            while (!scene.equals(parent)) {
                // Append parent to name and go one step up
                finalName = parent.GetName() + "/" + finalName;
            }

            return finalName;
        }

        /**
         * Returns a reference to the Surpass Scene, which is basically the Parent Group
         *
         * @return the first object of the Imaris Scene. The Scene itself
         * @throws Error an Imaris Error
         */
        public static IDataContainerPrx getScene() throws Error {
            return Utils.getImarisApp().GetSurpassScene();
        }

        /**
         * Returns the name of the item as seen from the Imaris GUI
         *
         * @param item the item whose name we need
         * @return the name of the item
         * @throws Error an Imaris Error Object
         */
        public static String getName(IDataItemPrx item) throws Error {

            return item.GetName();
        }

        /**
         * Set the name of this item, avoiding the use of SetName()
         *
         * @param item the item whose name we wish to set
         * @param name the name we wish to set it to
         * @throws Error an Imaris Error Object
         */
        public static void setName(IDataItemPrx item, String name) throws Error {
            item.SetName(name);
        }

        /**
         * Return the first item with the selected name inside the surpass scene
         * Returns null if not found
         *
         * @param name the name of the item to find
         * @return the requested item, null if not found
         * @throws Error an Imaris Error Object
         */
        public static IDataItemPrx findItem(String name) throws Error {
            // If you use parent as null, it is the same as getImarisApp.GetSurpassScene()
            return findItem(name, Scene.getScene());
        }

        /**
         * Return the first item with the selected name inside the given parent object
         * Returns null if not found
         *
         * @param name   the name of the item to find
         * @param parent the parent object, a group
         * @return the requested item, null if not found
         * @throws Error an Imaris Error Object
         */
        public static IDataItemPrx findItem(String name, IDataContainerPrx parent) throws Error {
            ItemQuery query = new ItemQuery.ItemQueryBuilder().setName(name).setParent(parent)
                    .build();

            return query.find(0);
        }

        /**
         * returns the first spots object in the main surpass scene with the given name
         *
         * @param name the name of the spots object to get. Returns the first spots object if there are multiple spots with
         *             the same name (Don't do that)
         * @return the requested spots, null if not found
         * @throws Error an Imaris Error Object
         */
        public static ISpotsPrx findSpots(String name) throws Error {
            return findSpots(name, Scene.getScene());
        }

        /**
         * Get the first spots object with the given name
         *
         * @param name   the name of the spots object to get. Returns the first spots object if there are multiple spots with
         *               the same name (Don't do that)
         * @param parent the parent object, a group
         * @return the requested spots, null if not found
         * @throws Error an Imaris Error Object
         */
        public static ISpotsPrx findSpots(String name, IDataContainerPrx parent) throws Error {
            ItemQuery query = new ItemQuery.ItemQueryBuilder()
                    .setName(name)
                    .setType("Spots")
                    .setParent(parent)
                    .build();

            return (ISpotsPrx) query.findFirst();
        }

        public static ISurfacesPrx findSurfaces(String name) throws Error {
            return findSurfaces(name, Scene.getScene());
        }

        /**
         * Get the first surfaces object with the given name
         *
         * @param name   the name of the surfaces object to get. Returns the first surfaces object if there are multiple
         *               surfaces with the same name (Don't do that)
         * @param parent the parent object, a group
         * @return the requested surfaces, null if not found
         * @throws Error an Imaris Error Object
         */
        public static ISurfacesPrx findSurfaces(String name, IDataContainerPrx parent) throws Error {
            ItemQuery query = new ItemQuery.ItemQueryBuilder().setName(name).setParent(parent).setType("Surfaces").build();

            return (ISurfacesPrx) query.findFirst();
        }

        /**
         * Get all items of the requested type in the main scene as a list (not within subfolder, groups)
         *
         * @param type   the type, defined by a String. See {@link ItemQuery.ItemType}
         * @param parent the parent object, a group
         * @return a list containins the objects
         * * @param parent the parent object, a group
         * @throws Error an Imaris Error Object
         */
        public static List<IDataItemPrx> findAll(String type, IDataContainerPrx parent) throws Error {
            ItemQuery query = new ItemQuery.ItemQueryBuilder().setType(type).setParent(parent).build();
            return query.find();
        }

        /**
         * Get all items of the requested type in the main scene as a list
         * You can set the public field {@link ItemQuery#isRecursiveSearch} to true if you want a recursive search
         *
         * @param type the type, defined by a String. See {@link ItemQuery.ItemType}
         * @return a list containins the objects
         * @throws Error an Imaris Error Object
         */
        public static List<IDataItemPrx> findAll(String type) throws Error {
            return findAll(type, null);
        }

        /**
         * Get all spots objects in the main scene as a list
         * You can set the public field {@link ItemQuery#isRecursiveSearch} to true if you want a recursive search
         *
         * @return the properly cast spots as a list
         * @throws Error an Imaris Error Object
         */
        public static List<ISpotsPrx> findAllSpots() throws Error {
            List<IDataItemPrx> items = findAll("Spots");

            // Explicitly cast to spots
            List<ISpotsPrx> spots = items.stream().map(item -> {
                return (ISpotsPrx) item;
            }).collect(Collectors.toList());

            return spots;
        }

        /**
         * Get all surfaces objects in the main scene as a list
         * You can set the public field {@link ItemQuery#isRecursiveSearch} to true if you want a recursive search
         *
         * @return the surfaces as a list
         * @throws Error an Imaris Error Object
         */
        public static List<ISurfacesPrx> findAllSurfaces() throws Error {
            List<IDataItemPrx> items = findAll("Surfaces");

            // Explicitly cast
            List<ISurfacesPrx> surfs = items.stream().map(item -> {
                return (ISurfacesPrx) item;
            }).collect(Collectors.toList());

            return surfs;
        }

        /**
         * Get all Group objects in the main scene as a list
         * You can set the public field {@link ItemQuery#isRecursiveSearch} to true if you want a recursive search
         *
         * @return the surfaces as a list
         * @throws Error an Imaris Error Object
         */
        public static List<IDataContainerPrx> findAllGroups() throws Error {
            ItemQuery query = new ItemQuery.ItemQueryBuilder().setType("Group").build();
            List<IDataItemPrx> items = query.find();

            // Explicitly cast
            List<IDataContainerPrx> groups = items.stream().map(item -> {
                return (IDataContainerPrx) item;
            }).collect(Collectors.toList());

            return groups;
        }

        /**
         * Adds the provided Item to the Main Imaris Scene (at the bottom)
         *
         * @param item the item (Spot, Surface, Folder) to add
         * @throws Error an Imaris Error Object
         */
        public static void addItem(IDataItemPrx item) throws Error {
            addItem(item, Scene.getScene());
        }

        /**
         * Adds the provided item as the last child to the provided parent item
         *
         * @param parent The parent item
         * @param item   the item to add as a child
         * @throws Error an Imaris Error Object
         */
        public static void addItem(IDataItemPrx item, IDataContainerPrx parent) throws Error {
            parent.AddChild(item, -1); // last element is position. -1 to append at the end.
        }

        /**
         * Removes the provided item from its parent,
         * if it's a Group, it removes the children explicitly as well. Unsure of necessary
         *
         * @param item the item in question
         * @throws Error an Imaris Error Object
         */
        public static void removeItem(IDataItemPrx item) throws Error {
            // if the item is a group
            IFactoryPrx factory = Utils.getImarisApp().GetFactory();
            if (factory.IsDataContainer(item)) {
                IDataContainerPrx group = factory.ToDataContainer(item);
                // make sure to remove all elements in it
                for (int grp = 0; grp < group.GetNumberOfChildren(); grp++) {
                    removeItem(group.GetChild(grp));
                }
            }
            // finally, remove the item
            item.GetParent().RemoveChild(item);
        }

        /**
         * Removes the provided List of items from its parent
         *
         * @param items, the list of items in question
         * @throws Error an Imaris Error Object
         */
        public static void removeItems(List<? extends IDataItemPrx> items) throws Error {
            for (IDataItemPrx it : items) {
                removeItem(it);
            }
        }

        /**
         * Reset the Imaris Scene
         *
         * @throws Error an Imaris Error Object
         */
        public static void reset() throws Error {
            List<ISpotsPrx> spots = findAllSpots();
            List<ISurfacesPrx> surfaces = findAllSurfaces();
            List<IDataContainerPrx> groups = findAllGroups();
            removeItems(spots);
            removeItems(surfaces);
            removeItems(groups);
            //for ( ISpotsPrx sp: spots ) { EasyXT.removeFromScene( sp ); }
            //for ( ISurfacesPrx srf: surfaces ) { EasyXT.removeFromScene( srf ); }
            //for ( IDataContainerPrx grp: groups ) { EasyXT.removeFromScene( grp );
            if (Scene.getScene() != null) selectItem(Scene.getScene());
        }

        /**
         * Creates a "Group" (folder) that can contain other items
         *
         * @param groupName the name to identify the group with
         * @return an item that can be added to a scene ({@link Scene#addItem(IDataItemPrx)}) or to which other items
         * can be added as children {@link Scene#addItem(IDataItemPrx, IDataContainerPrx)}
         * @throws Error an Imaris Error
         */
        public static IDataContainerPrx createGroup(String groupName) throws Error {
            IDataContainerPrx group = Utils.getImarisApp().GetFactory().CreateDataContainer();
            group.SetName(groupName);
            return group;
        }

        /**
         * returns the requested group from the surpass scene or null if empty
         *
         * @param groupName the name of the Group to find
         * @return the requested Group, null otherwise
         * @throws Error an Imaris Error
         */
        public static IDataContainerPrx findGroup(String groupName) throws Error {
            return findGroup(groupName, EasyXT.Scene.getScene());
        }

        /**
         * returns the requested group from the surpass scene or null if empty
         *
         * @param groupName the name of the group to Find
         * @param parent    the parent group to start the search from
         * @return the requested Group, null otherwise
         * @throws Error an Imaris Error
         */
        public static IDataContainerPrx findGroup(String groupName, IDataContainerPrx parent) throws Error {
            IDataItemPrx group = new ItemQuery.ItemQueryBuilder()
                    .setType("Group")
                    .setName(groupName)
                    .setParent(parent)
                    .build().findFirst();

            // Cast and return
            return (IDataContainerPrx) group;
        }

        /**
         * Selects the given item on the Imaris Surpass Scene GUI.
         *
         * @param item the item to select on the scene. Does not matter if it is in a group. Imaris finds it
         * @throws Error an Imaris Error
         */
        public static void selectItem(IDataItemPrx item) throws Error {
            Utils.getImarisApp().SetSurpassSelection(item);
        }
    }

    /**
     * This sublass deals with direct Imaris Datasets, not surface-based datasets
     * This includes methods to transfer, convert and move datasets around Imaris and Fiji
     */
    public static class Dataset {
        /**
         * The the calibration information from the current dataset. This includes extents, pixel size, channel names
         *
         * @return an ImarisCalibration
         * @throws Error an Imaris Error Object
         */
        public static ImarisCalibration getCalibration() throws Error {
            return getCalibration(Dataset.getCurrent());
        }

        /**
         * The the calibration information from a dataset. This includes extents, pixel size, channel names
         *
         * @param dataset the dataset to extract calibration information from
         * @return an ImarisCalibration
         * @throws Error an Imaris Error Object
         */
        public static ImarisCalibration getCalibration(IDataSetPrx dataset) throws Error {
            return new ImarisCalibration(dataset);
        }

        /**
         * Convenience method to return the currently active dataset
         *
         * @return a IDatasetPrx object containing a reference to all the pixel data
         * @throws Error an Imaris Error Object
         */
        public static IDataSetPrx getCurrent() throws Error {
            return Utils.getImarisApp().GetDataSet();
        }

        /**
         * Convenience method to set/replace the current dataset within the current Imaris scene with the one provided
         *
         * @param dataset a IDataSetPrx object containing a reference to all the pixel data
         * @throws Error an Imaris Error Object
         */
        public static void setCurrent(IDataSetPrx dataset) throws Error {
            Utils.getImarisApp().SetDataSet(dataset);
        }

        /**
         * Creates a new dataset with the right shape and calibration information
         *
         * @param calibration a calibration object with the bounds and dimensions of the desired dataset
         * @param bitDepth    bit depth, which can be 8, 16, 32
         * @return the new shaped dataset
         * @throws Error an Imaris Error
         */
        public static IDataSetPrx create(ImarisCalibration calibration, int bitDepth) throws Error {
            IDataSetPrx dataset = EasyXT.Utils.getImarisApp().GetFactory().CreateDataSet();
            return matchDimensionsFromCalibration(dataset, calibration, bitDepth);
        }

        /**
         * Will set the dataset to match the sizes and positions of this calibration. Useful when creating datasets
         *
         * @param dataset     the dataset to modify
         * @param calibration the calibration to base it off
         * @param bitDepth    bit depth, which can be 8, 16, 32
         * @return a new Imaris Dataset with the specified calibration and bit depth
         * @throws Error an Imaris Error
         */
        public static IDataSetPrx matchDimensionsFromCalibration(IDataSetPrx dataset, ImarisCalibration calibration, int bitDepth) throws Error {

            dataset.Create(Utils.getImarisDatasetType(bitDepth),
                    calibration.xSize,
                    calibration.ySize,
                    calibration.zSize,
                    calibration.cSize,
                    calibration.tSize);

            dataset.SetExtendMinX((float) calibration.xOrigin);
            dataset.SetExtendMinY((float) calibration.yOrigin);
            dataset.SetExtendMinZ((float) calibration.zOrigin);

            dataset.SetExtendMaxX((float) calibration.xEnd);
            dataset.SetExtendMaxY((float) calibration.yEnd);
            dataset.SetExtendMaxZ((float) calibration.zEnd);

            // Set the channel colors too, since we have them
            for (int c = 0; c < calibration.cSize; c++) {
                dataset.SetChannelColorRGBA(c, calibration.cColorsRGBA[c]);
                dataset.SetChannelName(c, calibration.cNames[c]);
                dataset.SetChannelRange(c, calibration.cMin[c], calibration.cMax[c]);
            }

            return dataset;
        }

        /**
         * Creates a new dataset with the right shape and calibration information
         *
         * @param imp an ImagePlus with the desired shape
         * @return the new shaped dataset
         * @throws Error an Imaris Error
         */
        public static IDataSetPrx create(ImagePlus imp) throws Error {
            IDataSetPrx dataset = EasyXT.Utils.getImarisApp().GetFactory().CreateDataSet();
            matchDimensionsFromImagePlus(dataset, imp);
            setFromImagePlus(dataset, imp);
            return dataset;
        }

        /**
         * Will set the dataset to match the sizes and positions of this calibration. Useful when creating datasets
         *
         * @param dataset the dataset to modify
         * @param imp     the imagePlus to use as reference
         * @return the new shaped dataset
         * @throws Error an Imaris Error
         */
        public static IDataSetPrx matchDimensionsFromImagePlus(IDataSetPrx dataset, ImagePlus imp) throws Error {

            Calibration cal = imp.getCalibration();

            // Note that Imaris dataset creation order is XYZCT, and ImageJ is usually XYCZT
            dataset.Create(Utils.getImarisDatasetType(imp.getBitDepth()),
                    imp.getWidth(),
                    imp.getHeight(),
                    imp.getNSlices(),
                    imp.getNChannels(),
                    imp.getNFrames());

            dataset.SetExtendMinX((float) cal.xOrigin);
            dataset.SetExtendMinY((float) cal.yOrigin);
            dataset.SetExtendMinZ((float) cal.zOrigin);

            dataset.SetExtendMaxX((float) (cal.xOrigin + (imp.getWidth()) * cal.pixelWidth));
            dataset.SetExtendMaxY((float) (cal.yOrigin + (imp.getHeight()) * cal.pixelHeight));
            dataset.SetExtendMaxZ((float) (cal.zOrigin + (imp.getNSlices()) * cal.pixelDepth));

            // Set channel color and range for dataset
            for (int c = 0; c < imp.getNChannels(); c++) {
                imp.setC(c + 1);
                // Set the color based on the last color in the LUT of this image
                if (imp instanceof CompositeImage) {
                    CompositeImage cimp = (CompositeImage) imp;
                    cimp.setC(c + 1);
                    Color color = cimp.getChannelColor();
                    dataset.SetChannelColorRGBA(c, Utils.getRGBAColor(color));
                }
                dataset.SetChannelRange(c, (float) imp.getDisplayRangeMin(), (float) imp.getDisplayRangeMax());
            }
            return dataset;
        }

        /**
         * Set data from an ImagePlus image into a dataset
         *
         * @param imp     the original image plus
         * @param dataset the dataset to insert the imagePlus into
         * @throws Error an Imaris Error Object
         */
        public static void setFromImagePlus(IDataSetPrx dataset, ImagePlus imp) throws Error {

            ImarisCalibration cal = new ImarisCalibration(dataset);

            // Sanity check, ImagePlus should be the same size as dataset
            if (!cal.isSameSize(imp))
                throw new Error("Inconsistent Sizes", "ImagePlus does not have the same dimensions as dataset", "");

            int w = cal.xSize;
            int h = cal.ySize;

            int nc = cal.cSize;
            int nz = cal.zSize;
            int nt = cal.tSize;

            int dBitDepth = getBitDepth(dataset);
            int iBitDepth = imp.getBitDepth();

            // Ideally we need to convert the bit depth of the dataset to that of the ImagePlus
            if (dBitDepth != iBitDepth) {
                log.warning("Provided dataset bitdepth (" + dBitDepth + "-bit)differs from image bitdepth (" + iBitDepth + "-bit)");
                log.warning("   dataset will be changed to (" + iBitDepth + " bits");
                Dataset.setBitDepth(iBitDepth, dataset);
                dBitDepth = iBitDepth;
            }
            for (int c = 0; c < nc; c++) {
                for (int z = 0; z < nz; z++) {
                    for (int t = 0; t < nt; t++) {
                        int idx = imp.getStackIndex(c + 1, z + 1, t + 1);
                        ImageProcessor ip = imp.getStack().getProcessor(idx);
                        switch (dBitDepth) {
                            case 8:
                                dataset.SetDataSubVolumeAs1DArrayBytes(((byte[]) ip.getPixels()), 0, 0, z, c, t, w, h, 1);
                                break;
                            case 16:
                                dataset.SetDataSubVolumeAs1DArrayShorts((short[]) ip.getPixels(), 0, 0, z, c, t, w, h, 1);
                                break;
                            case 32:
                                dataset.SetDataSubVolumeAs1DArrayFloats((float[]) ip.getPixels(), 0, 0, z, c, t, w, h, 1);
                                break;
                        }
                    }
                }
            }
            // Set channel color and range for dataset
            for (int c = 0; c < imp.getNChannels(); c++) {
                imp.setC(c + 1);
                // Set the color based on the last color in the LUT of this image
                if (imp instanceof CompositeImage) {
                    CompositeImage cimp = (CompositeImage) imp;
                    cimp.setC(c + 1);
                    Color color = cimp.getChannelColor();
                    dataset.SetChannelColorRGBA(c, Utils.getRGBAColor(color));
                }
                dataset.SetChannelRange(c, (float) imp.getDisplayRangeMin(), (float) imp.getDisplayRangeMax());
            }
        }

        /**
         * Returns bitdepth of a dataset. See {@link EasyXT#datatype}
         *
         * @param dataset the dataset to query the type from
         * @return an integer with the dataset type (8, 16, 32 or -1 if the type is not known)
         * @throws Error an Imaris Error Object
         */
        public static int getBitDepth(IDataSetPrx dataset) throws Error {
            tType type = dataset.GetType();
            // Thanks NICO
            return datatype.get(type);
        }

        /**
         * allows to change bit depth of the dataset
         * (Adapted from existing function in EasyXT-Matlab)*
         *
         * @param dataset  the dataset to change
         * @param bitDepth the bit depth (8,16 or 32) to set the dataset to
         * @throws Error and Imaris Error
         */
        public static void setBitDepth(int bitDepth, IDataSetPrx dataset) throws Error {
            tType aType = dataset.GetType();
            String outputString = "Dataset was converted from " + aType;
            switch (bitDepth) {
                case 32:
                    aType = tType.eTypeFloat;
                    break;
                case 16:
                    aType = tType.eTypeUInt16;
                    break;
                case 8:
                    aType = tType.eTypeUInt8;
                    break;
            }
            dataset.SetType(aType);
            log.info(outputString + " to " + aType + "bit");
        }

        /**
         * allows to change bit depth of the current dataset
         *
         * @param bitDepth 8, 16, or 32
         * @throws Error an Imaris Error
         */
        public static void setBitDepth(int bitDepth) throws Error {
            IDataSetPrx dataset = Dataset.getCurrent();
            setBitDepth(bitDepth, dataset);
        }

        /**
         * Returns an ImagePlus image of a dataset TODO : add a way to select only a subpart of it
         *
         * @param dataset an imaris dataset
         * @return an ImagePlus of a dataset
         * @throws Error an Imaris Error
         */
        public static ImagePlus getImagePlus(IDataSetPrx dataset) throws Error {

            ImarisCalibration cal = new ImarisCalibration(dataset);

            int w = cal.xSize;
            int h = cal.ySize;

            int nc = cal.cSize;
            int nz = cal.zSize;
            int nt = cal.tSize;

            int bitDepth = getBitDepth(dataset);

            ImageStack stack = ImageStack.create(w, h, nc * nz * nt, bitDepth);
            ImagePlus imp = new ImagePlus(Utils.getImarisApp().GetCurrentFileName(), stack);
            imp.setDimensions(nc, nz, nt);

            for (int c = 0; c < nc; c++) {
                for (int z = 0; z < nz; z++) {
                    for (int t = 0; t < nt; t++) {
                        int idx = imp.getStackIndex(c + 1, z + 1, t + 1);
                        ImageProcessor ip;
                        switch (bitDepth) {
                            case 8:
                                byte[] dataB = dataset.GetDataSubVolumeAs1DArrayBytes(0, 0, z, c, t, w, h, 1);
                                ip = new ByteProcessor(w, h, dataB, null);
                                stack.setProcessor(ip, idx);
                                break;
                            case 16:
                                short[] dataS = dataset.GetDataSubVolumeAs1DArrayShorts(0, 0, z, c, t, w, h, 1);
                                ip = new ShortProcessor(w, h, dataS, null);
                                stack.setProcessor(ip, idx);
                                break;
                            case 32:
                                float[] dataF = dataset.GetDataSubVolumeAs1DArrayFloats(0, 0, z, c, t, w, h, 1);
                                ip = new FloatProcessor(w, h, dataF, null);
                                stack.setProcessor(ip, idx);
                                break;
                        }
                    }
                }
            }

            imp.setStack(stack);
            imp.setCalibration(cal);

            if (nc*nz*nt>1) imp = HyperStackConverter.toHyperStack(imp, nc, nz, nt);

            // Set LookUpTables
            if (imp instanceof CompositeImage) {

                LUT[] luts = new LUT[nc];

                for (int c = 0; c < nc; c++) {
                    Color color = Utils.getColorFromInt(cal.cColorsRGBA[c]);
                    luts[c] = LUT.createLutFromColor(color);
                }

                ((CompositeImage) imp).setLuts(luts);

            } else if (nc == 1) {
                imp.setLut(LUT.createLutFromColor(Utils.getColorFromInt(cal.cColorsRGBA[0])));
            }

            // Transfer min and max display values.
            for (int c = 0; c < nc; c++) {
                imp.setC(c + 1);
                imp.setDisplayRange(cal.cMin[c], cal.cMax[c]);
            }

            return imp;
        }

        /**
         * Possibility to add an extra dataset to the Imaris File
         *
         * @param dataset  the new dataset
         * @param position position in the imaris app
         * @throws Error an Imaris Error
         */
        public static void addDataset(IDataSetPrx dataset, int position) throws Error {
            Utils.getImarisApp().SetImage(position, dataset);
            IFramePrx frame = Utils.getImarisApp().GetFactory().CreateFrame();
            Scene.addItem(frame);
        }

        /**
         * Possibility to get datasets other than the current dataset from the Imaris File
         *
         * @param position of the dataset in the imaris app
         * @return the dataset at that position
         * @throws Error an Imaris Error
         */
        public static IDataSetPrx getDataset(int position) throws Error {
            return Utils.getImarisApp().GetImage(position);
        }

        /**
         * Adds the selected ImagePlus to the current Dataset by appending it as new channels.
         * To speed things up, but with a higher memory footprint, we first clone the dataset
         * and append the channels. Finally we replace the current dataset with the new one
         * This avoids the Imaris GUI refreshing as the dataset is being modified, which is very slow
         *
         * @param imp the image to add to the current dataset
         * @throws Error an Imaris Error object
         */
        public static void addChannels(ImagePlus imp) throws Error {
            IDataSetPrx dataset = EasyXT.Dataset.getCurrent();
            IDataSetPrx newDataset = dataset.Clone();

            addChannels(imp, newDataset, 0, 0, 0, 0);
            setCurrent(newDataset);

            // Clear some memory
            dataset.Dispose();
        }

        /**
         * <p>Adds the selected ImagePlus to the provided IDatasetPrx by appending each channel of the ImagePlus as new
         * channels into the provided IDataSetPrx, at a given starting location in XYCZT </p>
         * <p>Sanity checks performed:</p>
         * <ul>
         *     <li>Ensure ImagePlus dimensions are not larger than the provided dataset dimensions (including provided starting locations). Throws an Error if larger than the dataset. </li>
         *     <li>Ensure consistent bit-depth between ImagePlus and IDataSetPrx. throws Error if otherwise</li>
         *     <li>Ensure consistent voxel size (ignore framerate) between ImagePlus and Dataset. Issues warning if otherwise</li>
         * </ul>
         * The user can define the start location XYZT in pixel coordinates.
         *
         * @param imp     the image from which to extract the channels to append
         * @param dataset the receiver dataset
         * @param xStart  start X position, in pixels (from top-left in ImageJ, will translate to bottom-left in Imaris)
         * @param yStart  start Y position, in pixels (from top-left in ImageJ, will translate to bottom-left in Imaris)
         * @param zStart  start Z position, in pixels (Z=0 is the top slice in Image, will translate to bottom slice in
         *                Imaris)
         * @param tStart  start T position, in pixels (from top-left in ImageJ, will translate to bottom-left in Imaris)
         * @throws Error an Imaris Error Object
         */
        public static void addChannels(ImagePlus imp, IDataSetPrx dataset, int xStart, int yStart, int zStart, int tStart) throws Error {
            // Get Metadata on dataset and image

            ImarisCalibration dCal = new ImarisCalibration(dataset);
            int dc = dataset.GetSizeC();

            Calibration iCal = imp.getCalibration();
            int iw = imp.getWidth();
            int ih = imp.getHeight();
            int iz = imp.getNSlices();
            int it = imp.getNFrames();
            int ic = imp.getNChannels();

            int dBitDepth = getBitDepth(dataset);
            int iBitDepth = imp.getBitDepth();

            if (!(dCal.xSize >= (xStart + iw) &&
                    dCal.ySize >= (yStart + ih) &&
                    dCal.zSize >= (zStart + iz) &&
                    dCal.tSize >= (tStart + it))) {

                String errorDetail = "Dataset\t(X,\tY,\tZ,\tT):\t (" + dCal.xSize + ",\t" + dCal.ySize + ",\t" + dCal.zSize + ",\t" + dCal.tSize + ")";
                errorDetail += "\nImage\t(X,\tY,\tZ,\tT):\t (" + iw + ",\t" + ih + ",\t" + iz + ",\t" + it + ")";
                errorDetail += "\nIncl. offset\t(X,\tY,\tZ,\tT):\t (" + (iw + xStart) + ",\t" + (ih + yStart) + ",\t" + (iz + zStart) + ",\t" + (it + tStart) + ")";

                throw new Error("Size Mismatch", "Dataset and ImagePlus do not have the same size in XYZT", errorDetail);
            }

            if (dBitDepth != iBitDepth) {
                String errorDetail = "   Dataset:" + dBitDepth + "-bit";
                errorDetail += "\n    Image:" + iBitDepth + "-bit";
                log.warning("Bit Depth Mismatch : Imaris Dataset and Fiji ImagePlus do not have same bit depth \n " + errorDetail);
                if (iBitDepth <= dBitDepth) {
                    // We can convert
                    if (dBitDepth == 16) new ImageConverter(imp).convertToGray16();
                    if (dBitDepth == 32) new ImageConverter(imp).convertToGray32();
                    log.warning("Image converted: " + imp.getBitDepth() + "-bit to match dataset bit depth (" + dBitDepth + "-bit)");

                } else {
                    log.severe("ImagePlus has higher bit depth than dataset. Cannot convert");
                    throw new Error("Bit depth Mismatch", "Image is " + iBitDepth + "-bit, dataset is " + dBitDepth + "-bit", "");
                }
            }

            // Issue warning in case voxel sizes do not match
            if (dCal.pixelDepth != iCal.pixelDepth &&
                    dCal.pixelHeight != iCal.pixelHeight &&
                    dCal.pixelWidth != iCal.pixelWidth) {

                log.warning("Warning: Voxel Sizes between Dataset and ImagePlus do not match:"
                        + "\n   Dataset Voxel Size\t(X,\tY,\tZ):\t" + dCal.pixelWidth + ",\t" + dCal.pixelHeight + ",\t" + dCal.pixelDepth + ")"
                        + "\n   Image Voxel Size\t(X,\tY,\tZ):\t" + iCal.pixelWidth + ",\t" + iCal.pixelHeight + ",\t" + iCal.pixelDepth + ")");
            }

            // Enlarge the dataset by setting its size to the cumulated number of channels
            dataset.SetSizeC(dc + ic);

            // Now loop through the dimensions of the ImagePlus to add data
            for (int c = 0; c < ic; c++) {

                // Set the color of this channel
                int idx = imp.getStackIndex(c + 1, 1, 1);
                int color = imp.getStack().getProcessor(idx).getColorModel().getRGB(255);

                dataset.SetChannelColorRGBA(c, color);

                // Set the volume from arrays
                for (int z = 0; z < iz; z++) {
                    for (int t = 0; t < it; t++) {
                        idx = imp.getStackIndex(c + 1, z + 1, t + 1);
                        ImageProcessor ip = imp.getStack().getProcessor(idx);
                        switch (dBitDepth) {
                            case 8:
                                dataset.SetDataSubVolumeAs1DArrayBytes(((byte[]) ip.getPixels()), xStart, yStart, zStart + z, c + dc, tStart + t, iw, ih, 1); // last element is sizeZ (one slice at a time)
                                break;
                            case 16:
                                dataset.SetDataSubVolumeAs1DArrayShorts((short[]) ip.getPixels(), xStart, yStart, zStart + z, c + dc, tStart + t, iw, ih, 1);
                                break;
                            case 32:
                                dataset.SetDataSubVolumeAs1DArrayFloats((float[]) ip.getPixels(), xStart, yStart, zStart + z, c + dc, tStart + t, iw, ih, 1);
                                break;
                        }
                    }
                }
            }
        }

        /**
         * Adds the selected ImagePlus to the provided IDatasetPrx by appending it as new channels
         *
         * @param imp     the image to add to the current dataset
         * @param dataset the dataset to which to add the imageplus as channel(s)
         * @throws Error an Imaris Error object
         */
        public static void addChannels(ImagePlus imp, IDataSetPrx dataset) throws Error {
            addChannels(imp, dataset, 0, 0, 0, 0);
        }
    }

    /**
     * This class contains all methods directly related to surfaces. Creation, search and conversion to and from images
     */
    public static class Surfaces {
        /**
         * allows to create a surface {@link SurfacesDetector} object that runs the Imaris surface detection wizard
         *
         * @param channel the 0-based channel index to use
         * @return the resulting detected surfaces object
         * @throws Error an Imaris Error
         */
        public static SurfacesDetector.SurfacesDetectorBuilder create(int channel) throws Error {
            return SurfacesDetector.Channel(channel);
        }

        /**
         * create a new surfaces object from this ImagePlus
         *
         * @param imp the image to get a surface from. Must be 8-bit and binary. It should contain a property called "Time Point"
         *            that contains the time index where this surface is to be inserted
         * @return a surfaces object that should render in Imaris (though pixellated)
         * @throws Error an Imaris Error if there was a problem
         */
        public static ISurfacesPrx create(ImagePlus imp) throws Error {
            // Check if it has a Time Index Property
            Object tInd = imp.getProperty("Time Index");
            if (tInd != null) {
                return create(imp, (int) tInd);
            }
            log.warning("EasyXT cannot find a timepoint associated with this surface mask. Defaulting to Timepoint 0");
            log.warning("Use Surfaces.create(ImagePlus imp, int timepoint) to specify the desired timepoint to insert this surface");

            return create(imp, 0);
        }

        /**
         * create a new surfaces object from this ImagePlus
         *
         * @param imp             the image to get a surface from. Must be 8-bit and binary
         * @param timepointOffset an index to offset the start of the surface creation.
         *                        for single timepoint Images, this is effectively the timepoint at which to place the surface
         * @return a surfaces object that should render in Imaris (though pixellated)
         * @throws Error an Imaris Error if there was a problem
         */
        public static ISurfacesPrx create(ImagePlus imp, int timepointOffset) throws Error {
            // Ensure image is binary
            if (!imp.getProcessor().isBinary()) {
                log.severe("Provided image is not binary");
                throw new Error("Image Type Error", "Image " + imp.getTitle() + " is not binary", "");
            }
            // Divide by 255
            // Duplicate so as not to change it
            ImagePlus tempImage = imp.duplicate();
            // Imaris surface accept ONLY 0-1 image
            int nProcessor = tempImage.getStack().getSize();
            IntStream.range(0, nProcessor).parallel().forEach(index -> {
                tempImage.getStack().getProcessor(index + 1).multiply(1.0 / 255.0);
            });

            // build empty surface object
            ISurfacesPrx surface = Utils.getImarisApp().GetFactory().CreateSurfaces();

            // Go through the timepoints and generate a surface for each timepoint
            for (int t = 0; t < tempImage.getNFrames(); t++) {
                // Temporary ImagePlus required to work!
                ImagePlus tImp = new Duplicator().run(tempImage, 1, 1, 1, imp.getNSlices(), t + 1, t + 1);
                IDataSetPrx data = Dataset.create(tImp);
                surface.AddSurface(data, t + timepointOffset);
            }
            EasyXT.Scene.setName(surface, tempImage.getTitle());

            return surface;
        }

        /**
         * create a new surfaces object from a Label ImagePlus
         *
         * @param impLabel the image to get a Surfaces from.
         *                 A label image, each label will be a surface of the Surfaces object.
         * @return the ISurfacesPrx with individual surface for each label value
         * @throws Error an Imaris Error if there was a problem
         */
        public static ISurfacesPrx createFromLabels(ImagePlus impLabel) throws Error {
            // Check if it has a Time Index Property
            Object tInd = impLabel.getProperty("Time Index");
            if (tInd != null) {
                return createFromLabels(impLabel, (int) tInd);
            }
            log.warning("EasyXT cannot find a timepoint associated with this surface mask. Defaulting to Timepoint 0");
            log.warning("Use Surfaces.create(ImagePlus imp, int timepoint) to specify the desired timepoint to insert this surface");

            return createFromLabels(impLabel, 0);
        }

        ;

        /**
         * @param impLabel        the image to get a Surfaces from.
         *                        A label image, each label will be a surface of the Surfaces object.
         * @param timepointOffset an index to offset the start of the surface creation.
         *                        for single timepoint Images, this is effectively the timepoint at which to place the surface
         * @return the ISurfacesPrx with individual surface for each label value
         * @throws Error an Imaris Error if there was a problem
         */

        public static ISurfacesPrx createFromLabels(ImagePlus impLabel, int timepointOffset) throws Error {

            // build empty surface object
            ISurfacesPrx surface = EasyXT.Utils.getImarisApp().GetFactory().CreateSurfaces();

            for (int t = 0; t < impLabel.getNFrames(); t++) {
                // get the current t stack
                ImagePlus tImpLabel = new Duplicator().run(impLabel, 1, 1, 1, impLabel.getNSlices(), t + 1, t + 1);
                // get the minimum and Maximum value of the current t, Labels
                //int impMin = (int) new StackStatistics(tImpLabel).min; // will always return 0 !
                int impMax = (int) new StackStatistics(tImpLabel).max;

                // we use findAllLabels() and voxelCount() from MorhopholibJ to "simplify" Labels processing
                int[] labels = LabelImages.findAllLabels(tImpLabel);
                int[] voxelCounts = LabelImages.voxelCount(tImpLabel.getStack(), labels);

                for (int idx = 1; idx < labels.length; idx++) {
                    if (voxelCounts[idx] > 1 ) {
                        // duplicate and theshold a Label
                        ImagePlus tempImage = tImpLabel.duplicate();
                        IJ.setThreshold(tempImage, labels[idx], labels[idx]);
                        IJ.run(tempImage, "Convert to Mask", "method=Default background=Dark black");// can't leave options blank, GUI pops-up

                        // imaris requires binary 0-1
                        int nProcessor = tempImage.getStack().getSize();
                        IntStream.range(0, nProcessor).parallel().forEach(index -> {
                            tempImage.getStack().getProcessor(index + 1).multiply(1.0 / 255.0);
                        });

                        // we don't need to check anymore if the binary
                        // - contains pixel , thanks to LabelImages.findAllLabels()
                        // - has > 1 voxel thanks to LabelImages.voxelCount() and the  if (voxelCounts[idx] > 1 )
                        IDataSetPrx data = EasyXT.Dataset.create(tempImage);
                        surface.AddSurface(data, t + timepointOffset);
                        tempImage.close();
                        // TODO: Warning: Because there is no way to set the Surfaces's IDs, there will certainly be a
                        //  discrepancy between the id of an original surface and a modified surface returned using this method...
                    }else if (voxelCounts[idx] == 1) {
                        log.warning("Objects with a label "+labels[idx]+" has only 1 voxel and has been excluded (Imaris issue)");
                    }
                }
                tImpLabel.close();
            }

            return surface;
        }

        /**
         * Get all surfaces objects in the main scene as a list (not within subfolder, groups)
         * You can set the public field {@link ItemQuery#isRecursiveSearch} to true if you want a recursive search
         *
         * @return the surfaces as a list
         * @throws Error an Imaris Error Object
         */
        public static List<ISurfacesPrx> findAll() throws Error {
            return Scene.findAllSurfaces();
        }

        /**
         * Get the first surfaces object with the given name
         * You can set the public field {@link ItemQuery#isRecursiveSearch} to true if you want a recursive search
         *
         * @param name   the name of the surfaces object to get. Returns the first surfaces object if there are multiple
         *               surfaces with the same name (Don't do that)
         * @param parent the parent object, a group
         * @return the requested surfaces, null if not found
         * @throws Error an Imaris Error Object
         */
        public static ISurfacesPrx find(String name, IDataContainerPrx parent) throws Error {
            return Scene.findSurfaces(name, parent);
        }

        /**
         * Overloaded version of {@link #find(String, IDataContainerPrx)}
         *
         * @param name the name of the surface to get
         * @return the chosen surface or null
         * @throws Error an Imaris Error
         */
        public static ISurfacesPrx find(String name) throws Error {
            return Scene.findSurfaces(name);
        }

        /**
         * Get surface mask instead of labels
         *
         * @param surface the surface to extract the mask from
         * @return an 8-bit image with the surface masks
         * @throws Error an Imaris Error
         */
        public static ImagePlus getMaskImage(ISurfacesPrx surface) throws Error {
            ImagePlus masks = getLabelsImage(surface);

            masks.getProcessor().setThreshold(1, Double.MAX_VALUE, ImageProcessor.NO_LUT_UPDATE);
            Prefs.blackBackground = true;
            IJ.run(masks, "Convert to Mask", "method=Default background=Dark black");
            masks.setTitle(Scene.getName(surface) + "-Masks");

            masks.setLut(LUT.createLutFromColor(Utils.getColorFromInt(surface.GetColorRGBA())));
            masks.setDisplayRange(0, 255);

            return masks;
        }

        /**
         * Get surfaces as fast as possible as labels
         *
         * @param surface a surface object see {@link Scene#findSurfaces(String)}
         * @return a Labeled image (ImagePlus)
         * @throws Error an Imaris Error
         */
        public static ImagePlus getLabelsImage(ISurfacesPrx surface) throws Error {
            // Try to be efficient getting the surfaces by using the extents of the individual masks and adding
            // them to a dataset rather than recreate the surface object

            // Getting a single surface mask for a timepoint
            int nSurfaces = surface.GetNumberOfSurfaces();
            int maxSurfaceID = (int) Arrays.stream(surface.GetIds()).max().getAsLong();

            ImarisCalibration cal = new ImarisCalibration(Dataset.getCurrent());

            // Force to only have one channel
            cal.cSize = 1;
            ImagePlus labelImage = IJ.createHyperStack(Scene.getName(surface) + "-Labels", cal.xSize, cal.ySize, 1, cal.zSize, cal.tSize, 32);
            labelImage.setCalibration(cal);

            for (int i = 0; i < nSurfaces; i++) {
                addSurfaceIndexToLabelImage(surface, i, labelImage);
            }

            // the labelImage is 32 bit (because this makes it easier to use internally,
            // we can convert it here to the right bit depth to save memory
            boolean doScaling = ImageConverter.getDoScaling();
            ImageConverter.setDoScaling(false);

            if (maxSurfaceID < 256) new ImageConverter(labelImage).convertToGray8();
            if (maxSurfaceID < 65536) new ImageConverter(labelImage).convertToGray16();
            ImageConverter.setDoScaling(doScaling);

            return labelImage;
        }

        /**
         * Allows to capture a single surface as an ImageJ binary image, within only the extents of the surface
         * This is more computationally cheap, and can be added in place later with {@link Surfaces#create(ImagePlus)}
         *
         * @param surface the surfaces we wish to extract a single surface from
         * @param id      the id of the surface to extract
         * @return an ImagePlus mask
         * @throws Error an Imaris Error
         */
        public static ImagePlus getSurfaceIdAsMask(ISurfacesPrx surface, long id) throws Error {

            List<Long> ids = Arrays.stream(surface.GetIds()).boxed().collect(Collectors.toList());
            int idx = ids.indexOf(new Long(id));

            cSurfaceLayout layout = surface.GetSurfaceDataLayout(idx);

            // A lot of operations we want to do might enlarge the surface, so we give it a bit more room than the extents proposed by Imaris
            layout = Surfaces.padLayout(layout, 5, 5, 5);

            // Extract the dataset
            IDataSetPrx dataset = surface.GetSingleMask(idx, layout.mExtendMinX, layout.mExtendMinY, layout.mExtendMinZ,
                    layout.mExtendMaxX, layout.mExtendMaxY, layout.mExtendMaxZ,
                    layout.mSizeX, layout.mSizeY, layout.mSizeZ);

            // Make an ImagePlus from it
            ImagePlus surfaceImp = Dataset.getImagePlus(dataset);

            // Multiply by 255
            int nProcessor = surfaceImp.getStack().getSize();
            IntStream.range(0, nProcessor).parallel().forEach(index -> {
                surfaceImp.getStack().getProcessor(index + 1).multiply(255.0);
            });

            // Record the timepoint into the Image Properties
            surfaceImp.setProperty("Time Index", surface.GetTimeIndex(idx));
            return surfaceImp;
        }

        /**
         * Internal method to enlarge a given layout by padding it with the pixels in X Y and Z
         *
         * @param layout layout to expand
         * @param extraX extra left-right padding for the X Axis
         * @param extraY extra left-right padding for the Y Axis
         * @param extraZ extra left-right padding for the Z Axis
         * @return the same layout, padded
         */
        private static cSurfaceLayout padLayout(cSurfaceLayout layout, int extraX, int extraY, int extraZ) {
            // Get voxel sizes
            double vX = (layout.mExtendMaxX - layout.mExtendMinX) / layout.mSizeX;
            double vY = (layout.mExtendMaxY - layout.mExtendMinY) / layout.mSizeY;
            double vZ = (layout.mExtendMaxZ - layout.mExtendMinZ) / layout.mSizeZ;

            // Change
            layout.mExtendMinX -= vX * extraX;
            layout.mExtendMinY -= vY * extraY;
            layout.mExtendMinZ -= vZ * extraZ;

            layout.mExtendMaxX += vX * extraX;
            layout.mExtendMaxY += vY * extraY;
            layout.mExtendMaxZ += vZ * extraZ;

            layout.mSizeX += 2 * extraX;
            layout.mSizeY += 2 * extraY;
            layout.mSizeZ += 2 * extraZ;

            return layout;

        }

        /**
         * This internal method appends a single surface defined by the
         * index (0-based, not the ID) into the provided ImagePlus. The ImagePlus is modified in place
         *
         * @param surface the surfaces to query
         * @param index   the index of the surface to add to the image
         * @param image   the image that will hold the labeled surfaces
         * @throws Error an Imaris Error
         */
        private static void addSurfaceIndexToLabelImage(ISurfacesPrx surface, int index, ImagePlus image) throws Error {
            // get the extents to find where to put the data
            Calibration fCal = image.getCalibration();
            // There is no guarantee that the surface will have the same calibration, so we need to coerce it to a multiple of the calibration of the ImagePlus
            // This means checking that the origin is a multiple of the ImagePlus Origin plus x times the pixel size
            cSurfaceLayout layout = adjustBounds(surface.GetSurfaceDataLayout(index), fCal);

            // GetTimepoint
            int t = surface.GetTimeIndex(index);
            long id = surface.GetIds()[index];
            IDataSetPrx currentSurfaceDataset = surface.GetSingleMask(index,
                    layout.mExtendMinX, layout.mExtendMinY, layout.mExtendMinZ,
                    layout.mExtendMaxX, layout.mExtendMaxY, layout.mExtendMaxZ,
                    layout.mSizeX, layout.mSizeY, layout.mSizeZ);

            // We can convert this to an ImagePlus and add it in place to the provided ImagePlus
            ImagePlus temp = Dataset.getImagePlus(currentSurfaceDataset);
            Calibration tCal = temp.getCalibration();

            // Find where the temp image starts
            int startX = (int) Math.round((tCal.xOrigin - fCal.xOrigin) / fCal.pixelWidth);
            int startY = (int) Math.round((tCal.yOrigin - fCal.yOrigin) / fCal.pixelHeight);
            int startZ = (int) Math.round((tCal.zOrigin - fCal.zOrigin) / fCal.pixelDepth);

            // Make sure the startZ is correct
            if (startZ < 0) startZ = 0;
            if ((startZ + temp.getNSlices()) > image.getNSlices()) {
                // Remove Slices startZ = 0;
            }

            // Add the data, along with the desired index
            for (int z = 1; z <= temp.getNSlices(); z++) {
                int position = image.getStackIndex(1, z + startZ, t + 1);
                ImageProcessor fip = image.getStack().getProcessor(position);
                ImageProcessor pip = temp.getStack().getProcessor(z).convertToFloat();

                // The mask is within 0-1 so we just need to multiply by the ID
                // NOTE: We increment the ID by 1 because the surface ID can start at 0
                pip.multiply(id + 1);

                // Fast copy interface using Blitter
                fip.copyBits(pip, startX, startY, Blitter.COPY_ZERO_TRANSPARENT);

                image.getStack().setProcessor(fip, position);
            }
        }

        // TODO add method to recover timepoint from an ImagePlus

        /**
         * This is a private method that helps readjust the size of a surface image to the original image calibration
         * As it happens, Imaris stores smaller surface datasets to save space. How small probably depends on
         * the gaussian blur that is defined when creating surfaces
         *
         * @param originalLayout       the bounds that the surface wants to be set to
         * @param referenceCalibration the reference image calibration to use to redefine those bounds
         * @return a new surface layout with the right pixel size that can be reused when exporting a mask
         */
        private static cSurfaceLayout adjustBounds(cSurfaceLayout originalLayout, Calibration referenceCalibration) {

            // Prepare the new layout
            cSurfaceLayout newLayout = originalLayout.clone();

            // Step 1: coerce origin to a multiple of the origin of the imagePlus x the calibration
            // This means (example with X: layoutXOrigin = imageXOrigin + x*xCal)
            double xmi = Math.floor((originalLayout.mExtendMinX - referenceCalibration.xOrigin) / referenceCalibration.pixelWidth);
            double xma = Math.ceil((originalLayout.mExtendMaxX - referenceCalibration.xOrigin) / referenceCalibration.pixelWidth);

            newLayout.mExtendMinX = (float) (referenceCalibration.xOrigin + xmi * referenceCalibration.pixelWidth);
            newLayout.mExtendMaxX = (float) (referenceCalibration.xOrigin + xma * referenceCalibration.pixelWidth);

            double ymi = Math.floor((originalLayout.mExtendMinY - referenceCalibration.yOrigin) / referenceCalibration.pixelHeight);
            double yma = Math.ceil((originalLayout.mExtendMaxY - referenceCalibration.yOrigin) / referenceCalibration.pixelHeight);

            newLayout.mExtendMinY = (float) (referenceCalibration.yOrigin + ymi * referenceCalibration.pixelHeight);
            newLayout.mExtendMaxY = (float) (referenceCalibration.yOrigin + yma * referenceCalibration.pixelHeight);

            double zmi = Math.floor((originalLayout.mExtendMinZ - referenceCalibration.zOrigin) / referenceCalibration.pixelDepth);
            double zma = Math.ceil((originalLayout.mExtendMaxZ - referenceCalibration.zOrigin) / referenceCalibration.pixelDepth);

            newLayout.mExtendMinZ = (float) (referenceCalibration.zOrigin + zmi * referenceCalibration.pixelDepth);
            newLayout.mExtendMaxZ = (float) (referenceCalibration.zOrigin + zma * referenceCalibration.pixelDepth);

            // Finally adjust number of pixels
            newLayout.mSizeX = (int) (Math.ceil((newLayout.mExtendMaxX - newLayout.mExtendMinX) / referenceCalibration.pixelWidth));
            newLayout.mSizeY = (int) (Math.ceil((newLayout.mExtendMaxY - newLayout.mExtendMinY) / referenceCalibration.pixelHeight));
            newLayout.mSizeZ = (int) (Math.ceil((newLayout.mExtendMaxZ - newLayout.mExtendMinZ) / referenceCalibration.pixelDepth));

            return newLayout;
        }

        /**
         * Returns the Surfaces as an Imaris Dataset.
         * The difference with the direct Imaris API here is that this includes timepoints as well.
         *
         * @param surface the surface to extract the dataset from
         * @return a dataset of the surface mask with all timpeoints included
         * @throws Error an Imaris error
         */
        public static IDataSetPrx getMaskDataset(ISurfacesPrx surface) throws Error {
            IDataSetPrx finalDataset = Dataset.getCurrent().Clone();
            ImarisCalibration cal = new ImarisCalibration(finalDataset);

            // Check if there are channels
            finalDataset.SetSizeC(1);

            Dataset.setBitDepth(8, finalDataset);

            // Loop through each timepoint, and get the dataset, then replace
            for (int t = 0; t < cal.tSize; t++) {
                IDataSetPrx oneTimepoint = getMaskDataset(surface, 1.0, t);
                finalDataset.SetDataVolumeAs1DArrayBytes(oneTimepoint.GetDataVolumeAs1DArrayBytes(0, 0), 0, t);
            }
            return finalDataset;
        }

        /**
         * Returns an Imaris dataset with all surfaces as a mask for the given timepoint
         * This method ensures that the surface dataset will be the same dimensions as the original dataset
         *
         * @param surface    the surface to extract the dataset from
         * @param downsample whether we want a downsampled version of the dataset (in x z and z)
         * @param timepoint  the timepoint (0-based) whose surface we want
         * @return an Imaris binary dataset, where voxels in the surface have value 1
         * @throws Error an Imaris Error
         */
        public static IDataSetPrx getMaskDataset(ISurfacesPrx surface, double downsample, int timepoint) throws Error {
            ImarisCalibration cal = new ImarisCalibration(Utils.getImarisApp().GetDataSet()).getDownsampled(downsample);

            IDataSetPrx data = surface.GetMask((float) cal.xOrigin, (float) cal.yOrigin, (float) cal.zOrigin,
                    (float) cal.xEnd, (float) cal.yEnd, (float) cal.zEnd,
                    cal.xSize, cal.ySize, cal.zSize, timepoint);
            return data;
        }

        /**
         * Returns an Imaris surface filtered with a test minValue &lt; value &gt; maxValue for a defined columnName
         *
         * @param surface    the surface to filter
         * @param columnName ColumnName as displayed in ImageJ Results Table you got from @EasyXT.Stats.export()
         * @param minValue   the minimum value
         * @param maxValue   the maximum value
         * @return filteredSurface the filtered surface
         * @throws Error an Imaris Error
         */
        public static ISurfacesPrx filter(ISurfacesPrx surface, String columnName, double minValue, double maxValue) throws Error {

           return (ISurfacesPrx) Utils.filter( surface,  columnName,  minValue,  maxValue);

        }

        /**
         * Returns an Imaris surface filtered with a test minValue &lt; value for a defined columnName
         *
         * @param surface    the surface to filter
         * @param columnName ColumnName as displayed in ImageJ Results Table you got from @EasyXT.Stats.export()
         * @param minValue   the minimum value
         * @return filteredSurface the filtered surface
         * @throws Error an Imaris Error
         */
        public static ISurfacesPrx filterAbove(ISurfacesPrx surface, String columnName, double minValue) throws Error {

            ISurfacesPrx filteredSurface = EasyXT.Surfaces.filter(surface, columnName, minValue, Double.MAX_VALUE);

            return filteredSurface;

        }

        /**
         * Returns an Imaris surface filtered with a test value &gt; maxValue for a defined columnName
         *
         * @param surface    the surface to filter
         * @param columnName ColumnName as displayed in ImageJ Results Table you got from @EasyXT.Stats.export()
         * @param maxValue   the minimum value
         * @return filteredSurface the filtered surface
         * @throws Error an Imaris Error
         */
        public static ISurfacesPrx filterBelow(ISurfacesPrx surface, String columnName, double maxValue) throws Error {

            ISurfacesPrx filteredSurface = EasyXT.Surfaces.filter(surface, columnName, -1 * Double.MAX_VALUE, maxValue);

            return filteredSurface;

        }

    }

    /**
     * This class contains all methods directly related to spots. Creation, search and conversion to and from images
     */
    public static class Spots {
        /**
         * Convenience method to call {@link SpotsDetector.SpotsDetectorBuilder}. Check that method for optional parameters
         *
         * @param channel the 0-based channel to run the spots detection on
         * @return a {@link SpotsDetector.SpotsDetectorBuilder}
         * @throws Error an Imaris Error Object
         */
        public static SpotsDetector.SpotsDetectorBuilder create(int channel) throws Error {
            return SpotsDetector.Channel(channel);
        }

        /**
         * Allows to create a spots object from the coordinate list at the given timepoint
         * Spots objects in Imaris cannot have more coordinates added afterwards
         * So spots for all timepoints should be made in advance and use {@link #create(List, List, List)} instead
         *
         * @param coordinates coordinates list
         * @param radiusXYZ   radiuses in xyz for the spots, identical for all spots
         * @param timepoint   given timepoint
         * @return an imaris spot object
         * @throws Error an Imaris Error
         */
        public static ISpotsPrx create(List<Point3D> coordinates, Point3D radiusXYZ, Integer timepoint) throws Error {

            // Duplicate the radii and timepoint as many times as there are coordinates
            int n = coordinates.size();

            List<Point3D> radii = new ArrayList<>(n);
            List<Integer> timepoints = new ArrayList<>(n);

            for (int i = 0; i < n; i++) {
                radii.add(radiusXYZ);
                timepoints.add(timepoint);
            }
            return Spots.create(coordinates, radii, timepoints);
        }

        /**
         * Create Spots object based on a list of coordinates, radii and timepoints
         *
         * @param coordinates the coordinates, in calibrated units in X Y Z
         * @param radiiXYZ    the radii for each spot in X Y Z
         * @param timepoints  the 0-based timepoints for each spot
         * @return created spots
         * @throws Error an Imaris Error
         */
        public static ISpotsPrx create(List<Point3D> coordinates, List<Point3D> radiiXYZ, List<Integer> timepoints) throws Error {
            if (coordinates.size() != radiiXYZ.size() || coordinates.size() != timepoints.size()) {
                throw new Error("Inconsistent Sizes", "Coordinates, Radii and timepoints lists must be the same size", "");
            }

            // convert to match Imaris Library constructor
            int n = coordinates.size();
            int[] t = new int[n];

            // Imaris Spots constructor only takes a single value radius for xyz, so we need to extract it for the initialization
            float[] rad = new float[n];
            float[][] coords = new float[n][];
            float[][] rads = new float[n][];

            for (int i = 0; i < coordinates.size(); i++) {
                Point3D p = coordinates.get(i);
                Point3D r = radiiXYZ.get(i);
                coords[i] = new float[]{p.getPoint3f().getX(), p.getPoint3f().getY(), p.getPoint3f().getZ()};
                t[i] = timepoints.get(i).intValue();
                rad[i] = radiiXYZ.get(i).getPoint3f().getX();
                rads[i] = new float[]{r.getPoint3f().getX(), r.getPoint3f().getY(), r.getPoint3f().getZ()};
            }

            ISpotsPrx spots = Utils.getImarisApp().GetFactory().CreateSpots();
            spots.Set(coords, t, rad);

            // Radii can only be set in XYZ afterwards
            spots.SetRadiiXYZ(rads);
            return spots;
        }

        /**
         * Get all spots objects in the main scene as a list (not within a group)
         *
         * @return the properly cast spots as a list
         * @throws Error an Imaris Error Object
         */
        public static List<ISpotsPrx> findAll() throws Error {
            return Scene.findAllSpots();
        }

        /**
         * Get the first Spots object with the given name
         *
         * @param name   the name of the spots object to get. Returns the first spots object if there are multiple spots with
         *               the same name (Don't do that)
         * @param parent the parent object, a group
         * @return the requested spots, null if not found
         * @throws Error an Imaris Error Object
         */
        public static ISpotsPrx find(String name, IDataContainerPrx parent) throws Error {
            return Scene.findSpots(name, parent);
        }

        /**
         * Returns an Imaris surface filtered with a test minValue &lt; value &gt; maxValue for a defined columnName
         *
         * @param spots    the surface to filter
         * @param columnName ColumnName as displayed in ImageJ Results Table you got from @EasyXT.Stats.export()
         * @param minValue   the minimum value
         * @param maxValue   the maximum value
         * @return filteredSurface the filtered surface
         * @throws Error an Imaris Error
         */
        public static ISpotsPrx filter(ISpotsPrx spots, String columnName, double minValue, double maxValue) throws Error {

            return (ISpotsPrx) Utils.filter( spots,  columnName,  minValue,  maxValue);

        }

        /**
         * Returns an Imaris surface filtered with a test minValue &lt; value for a defined columnName
         *
         * @param spots    the surface to filter
         * @param columnName ColumnName as displayed in ImageJ Results Table you got from @EasyXT.Stats.export()
         * @param minValue   the minimum value
         * @return filteredSurface the filtered surface
         * @throws Error an Imaris Error
         */
        public static ISpotsPrx filterAbove(ISpotsPrx spots, String columnName, double minValue) throws Error {

            return EasyXT.Spots.filter(spots, columnName, minValue, Double.MAX_VALUE);

        }

        /**
         * Returns an Imaris surface filtered with a test value &gt; maxValue for a defined columnName
         *
         * @param spots    the surface to filter
         * @param columnName ColumnName as displayed in ImageJ Results Table you got from @EasyXT.Stats.export()
         * @param maxValue   the minimum value
         * @return filteredSurface the filtered surface
         * @throws Error an Imaris Error
         */
        public static ISpotsPrx filterBelow(ISpotsPrx spots, String columnName, double maxValue) throws Error {

            return EasyXT.Spots.filter(spots, columnName, -1 * Double.MAX_VALUE, maxValue);

        }


        /**
         * returns the first Spots object in the main surpass scene with the given name
         *
         * @param name the name of the spots object to get. Returns the first spots object if there are multiple spots with
         *             the same name (Don't do that)
         * @return the requested spots, null if not found
         * @throws Error an Imaris Error Object
         */
        public static ISpotsPrx find(String name) throws Error {
            return Scene.findSpots(name);
        }

        /**
         * Get an ImagePlus of the Spots as a mask (255)
         *
         * @param spots a Spots object see {@link  Scene#findSpots(String, IDataContainerPrx)}
         * @return an a mask of the spots, with teh same size as the original dataset
         * @throws Error an Imaris Error
         */
        public static ImagePlus getMaskImage(ISpotsPrx spots) throws Error {
            return getImage(spots, false, false);
        }

        /**
         * Get an ImagePlus of the spots as a label (object has Imaris-ID value)
         *
         * @param spots     a spots object see {@link  Scene#findSpots(String)}
         * @param isValueId boolean to define if value will be 255 or Imaris-ID value
         * @param isGauss   boolean to decide to apply gaussian filter on each spot
         * @return and ImagePlus 16-bit
         * @throws Error
         */
        private static ImagePlus getImage(ISpotsPrx spots, boolean isValueId, boolean isGauss) throws Error {

            // get the calibration info from Imaris
            ImarisCalibration cal = new ImarisCalibration(Dataset.getCurrent());

            // Get all information from the spots object
            long[] spots_ids = spots.GetIds();
            float[][] spotsCenterXYZ = spots.GetPositionsXYZ();
            float[][] spotsRadiiXYZ = spots.GetRadiiXYZ();
            int[] spotsT = spots.GetIndicesT();

            // Define default vectors along the X and Y axes
            Vector3D vector3D1 = new Vector3D(1, 0, 0);
            Vector3D vector3D2 = new Vector3D(0, 1, 0);
            if (Math.abs(vector3D1.dotProduct(vector3D2)) > 0.001) {
                IJ.log("ERROR : vectors should be perpendicular");
            }

            // Make an object Creator for building the spots
            ObjectCreator3D objCreator = new ObjectCreator3D(cal.xSize, cal.ySize, cal.zSize);
            objCreator.setResolution(cal.pixelWidth, cal.pixelDepth, cal.getUnit());

            ImagePlus finalImp;
            // by default the value is 255
            int val = 255;

            int previousT = spotsT[0];
            ArrayList<ImagePlus> imps = new ArrayList<ImagePlus>(spotsT[spotsT.length - 1]);
            for (int t = 0; t < spotsT.length; t++) {
                // if the current spot is from a different time-point
                if ((cal.tSize > 1) && ((spotsT[t] != previousT) || (t == spotsT.length - 1))) {
                    // store the current status into an ImagePlus
                    // N.B. duplicate is required to store the current time-point
                    imps.add(new ImagePlus("t" + previousT, objCreator.getStack().duplicate()));
                    // and reset the obj_creator
                    objCreator.reset();
                }
                // but if is_value_id is true, use the ID number for the value
                if (isValueId) val = (int) spots_ids[t];
                // add an ellipsoid to obj_creator
                objCreator.createEllipsoidAxesUnit(spotsCenterXYZ[t][0] - cal.xOrigin, spotsCenterXYZ[t][1]- cal.yOrigin, spotsCenterXYZ[t][2] - cal.zOrigin, spotsRadiiXYZ[t][0], spotsRadiiXYZ[t][1], spotsRadiiXYZ[t][2], (float) val, vector3D1, vector3D2, isGauss);
                // set the previous_t
                previousT = spotsT[t];
                if (t % 10 == 0) log.info("Creating Labelled Spots " + (t + 1) + "/" + spotsT.length);
            }

            if (cal.tSize > 1) {
                // https://stackoverflow.com/questions/9572795/convert-list-to-array-in-java
                ImagePlus[] impsArray = imps.toArray(new ImagePlus[0]);
                finalImp = Concatenator.run(impsArray);
            } else {
                finalImp = new ImagePlus("t" + previousT, objCreator.getStack().duplicate());
            }

            finalImp.setDisplayRange(0, val);
            finalImp.setTitle(Files.getOpenFileName());
            finalImp.setCalibration(cal);

            if (!isValueId) {
                IJ.run(finalImp, "8-bit", "");
            }
            return finalImp;
        }

        /**
         * Get an ImagePlus of the spots as a label (object has Imaris-ID value)
         *
         * @param spots a spots object see {@link  Scene#findSpots(String)}
         * @return an ImagePlus
         * @throws Error an Imaris Error
         */
        public static ImagePlus getLabelsImage(ISpotsPrx spots) throws Error {
            return getImage(spots, true, false);
        }
    }

    /**
     * This class handles getting statistics from Imaris as well as sending statistics back to Imaris
     */
    public static class Stats {

        /**
         * returns all available Imaris statistics from the selected item
         *
         * @param item the item to query
         * @return a ResultsTable to be used and displayed by ImageJ
         * @throws Error an Imaris Error Object
         */
        public static ResultsTable export(IDataItemPrx item) throws Error {
            return new StatsQuery(item).get();
        }

        /**
         * returns the selected statistic from the selected item
         *
         * @param item the item to query
         * @param name the name of the statistic, as in the IJ GUI minus the "Ch=1 Img=1", ... text
         * @return a ResultsTable to be used and displayed by ImageJ
         * @throws Error an Imaris Error Object
         */
        public static ResultsTable export(IDataItemPrx item, String name) throws Error {
            return new StatsQuery(item)
                    .selectStatistic(name)
                    .get();
        }

        /**
         * returns only the selected statistic at the selected channel Careful. Imaris results are one-based for channels
         *
         * @param item    the item to query
         * @param name    the name of the statistic, as in the IJ GUI minus the "Ch=1 Img=1", ... text
         * @param channel the channel for which we want the statistic
         * @return a ResultsTable to be used and displayed by ImageJ
         * @throws Error an Imaris Error Object
         */
        public static ResultsTable export(IDataItemPrx item, String name, Integer channel) throws Error {
            return new StatsQuery(item)
                    .selectStatistic(name)
                    .selectChannel(channel)
                    .get();
        }

        /**
         * returns the selected statistics
         *
         * @param item  the item to query
         * @param names the names of the statistic, as in the IJ GUI minus the "Ch=1 Img=1", ... text
         * @return a ResultsTable to be used and displayed by ImageJ
         * @throws Error an Imaris Error Object
         */
        public static ResultsTable export(IDataItemPrx item, List<String> names) throws Error {
            return new StatsQuery(item)
                    .selectStatistics(names)
                    .get();
        }

        /**
         * returns the selected statistics at the selected channel Careful. Imaris results are one-based for channels
         *
         * @param item    the item to query
         * @param names   the names of the statistic, as in the IJ GUI minus the "Ch=1 Img=1", ... text
         * @param channel the channel for which we want the statistic
         * @return a ResultsTable to be used and displayed by ImageJ
         * @throws Error an Imaris Error Object
         */
        public static ResultsTable export(IDataItemPrx item, List<String> names, Integer channel) throws Error {
            return new StatsQuery(item)
                    .selectStatistics(names)
                    .selectChannel(channel)
                    .get();
        }

        /**
         * returns the selected statistics at the selected channels Careful. Imaris results are one-based for channels
         *
         * @param item     the item to query
         * @param names    the names of the statistic, as in the IJ GUI minus the "Ch=1 Img=1", ... text
         * @param channels the channels for which we want the statistic
         * @return a ResultsTable to be used and displayed by ImageJ
         * @throws Error an Imaris Error Object
         */
        public static ResultsTable export(IDataItemPrx item, List<String> names, List<Integer> channels) throws Error {
            return new StatsQuery(item)
                    .selectStatistics(names)
                    .selectChannels(channels)
                    .get();
        }

        /**
         * Extract the given Results Table column as a map where th key is the id of the object and the value is another map with
         * the statistic as the key and the statistic itself as the value.
         * This helps mostly to create a new statistic more easily. See {@link ch.epfl.biop.imaris.demo.AddStatsDemo}
         *
         * @param statistics the results table from which to extract the statistics. The table should have the following columns
         *                   "Label", "Name", "ID", "Timepoint", "Category". After that you can have the columns you want.
         * @param columnName The column you want to use for the statistic. Only one column at a time for now
         * @return a double Map that can be used with {@link #create(IDataItemPrx, String, Map)}
         */
        public static Map<Long, Map<String, Double>> extract(ResultsTable statistics, String columnName) {
            return StatsQuery.extractStatistic(statistics, columnName);
        }

        /**
         * Used in conjunction with {@link #extract(ResultsTable, String)} this allows to prepare a new statistic to be added into
         * the Imaris object. Use the methods of {@link StatsCreator} to set more information about the statistic such as
         * the channel, unit and category.
         *
         * @param item       the item to which to append statistics
         * @param statName   the name of the statistic we wish to add. This is how you will see it in Imaris
         * @param statValues the actual statistics to add. Each key is the id of the object
         *                   each value is a Map where the key is the name of the statistic and the value is the numerical value of the statistic
         * @return the same object, to continue with optional parameter configuration
         */
        public static StatsCreator create(IDataItemPrx item, String statName, Map<Long, Map<String, Double>> statValues) {
            return new StatsCreator(item, statName, statValues);
        }
    }


    /**
     * This class contains methods directly related to Surface and Spots tracking.
     */
    public static class Tracks {

        public static ItemTracker.ItemTrackerBuilder create(ObjectPrx aItem) throws Error {
            return ItemTracker.Item(aItem);
        }

    }


    /**
     * This internal class contains methods that normally do not need to be used and are mostly internal
     */
    public static class Utils {
        /**
         * casts each Imaris Object to its right Class for easier downstream processing Not sure if this is needed but have
         * not tested without
         *
         * @param item the item to return the specific class of
         * @return the same item but cast to its appropriate subclass
         * @throws Error an Imaris Error Object
         */
        static IDataItemPrx castToType(ObjectPrx item) throws Error {
            IFactoryPrx factory = getImarisApp().GetFactory();

            if (factory.IsSpots(item)) {
                return factory.ToSpots(item);
            }
            if (factory.IsSurfaces(item)) {
                return factory.ToSurfaces(item);
            }
            if (factory.IsVolume(item)) {
                return factory.ToVolume(item);
            }
            if (factory.IsLightSource(item)) {
                return factory.ToLightSource(item);
            }
            if (factory.IsFrame(item)) {
                return factory.ToFrame(item);
            }
            if (factory.IsDataContainer(item)) {
                return factory.ToDataContainer(item);
            }
            return null;
        }

        /**
         * Returns instance of Imaris App
         * This is so you can access the Imaris API functionalities directly.
         *
         * @return an Imaris Application connection.
         */
        public static IApplicationPrx getImarisApp() {
            return APP;
        }

        /**
         * Closes an existing Imaris ICE connection before reattempting to connect.
         * This is useful when Imaris has crashed but fiji is still running.
         */
        public static void resetImarisConnection() {
            CloseIceClient();
            log.info("Reconnecting to Imaris ICE Server...");
            mIceClient = new IceClient("ImarisServer", M_END_POINTS, 10000);
            ObjectPrx potentialApp = mIceClient.GetServer().GetObject(0);
            APP = IApplicationPrxHelper.checkedCast(potentialApp);
            log.info("Connection Re-established");
        }

        /**
         * Recover a Color for use to set ImagePlus LUTs
         *
         * @param color the Imaris Color descriptor
         * @return the Java Color
         */
        public static Color getColorFromInt(int color) {
            byte[] bytes = ByteBuffer.allocate(4).putInt(color).array();
            int[] colorArray = new int[3];
            colorArray[0] = bytes[3] & 0xFF;
            colorArray[1] = bytes[2] & 0xFF;
            colorArray[2] = bytes[1] & 0xFF;
            return getColorIntFromIntArray(colorArray);
        }

        /**
         * recover an RGB color for use with ImageJ from a 3 element (R,G,B) array
         *
         * @param color the Java Color
         * @return the java color object to use in ImageJ
         */
        public static Color getColorIntFromIntArray(int[] color) {

            if (color.length < 3) {
                log.warning("You did not provide enough colors. need 3. Returning white.");
                return new Color(255, 255, 255);
            }
            return new Color(color[0], color[1], color[2]);
        }

        public static int getRGBAColor(Color color) {
            return color.getRed() + 256 * color.getGreen() + 256 * 256 * color.getBlue();
        }


        /**
         * Returns an Imaris item filtered with a test minValue &lt; value &gt; maxValue for a defined columnName
         *
         * @param aItem      the item to filter
         * @param columnName ColumnName as displayed in ImageJ Results Table you got from @EasyXT.Stats.export()
         * @param minValue   the minimum value
         * @param maxValue   the maximum value
         * @return aItemFiltered the filtered item
         * @throws Error an Imaris Error
         */
        public static IDataItemPrx filter(IDataItemPrx aItem, String columnName, double minValue, double maxValue) throws Error {
            IFactoryPrx factory = EasyXT.Utils.getImarisApp().GetFactory();
            IDataItemPrx aItemFiltered = null;

            // current @EasyXT.Stats.export() table are string
            // Issue with using imagej= 1.53j ? , to getColumnAsStrings() )
            // workaround use Variable[]
            ResultsTable rt = Stats.export(aItem, columnName);

            double[] ids = Arrays.stream(rt.getColumnAsVariables("ID")).map(var -> var.getValue()).mapToDouble(d -> d).toArray();
            double[] values = Arrays.stream(rt.getColumnAsVariables(columnName)).map(var -> var.getValue()).mapToDouble(d -> d).toArray();

            // Here we'll filtered the ids if they pass the test :  minValue < value < maxValue
            // use List to add item
            List<Integer> filteredIdsList = new ArrayList<>();
            for (int i = 0; i < ids.length; i++) {
                if ( (values[i] >= minValue) && (values[i] <= maxValue)) {
                    filteredIdsList.add((int) ids[i]);
                }
            }

            // spots or surfaces ?
            if (factory.IsSpots(aItem)) {
                // copySpots requires a long[] so need to convert the List
                long[] filteredIds = filteredIdsList.stream().mapToLong(l -> l).toArray();
                ISpotsPrx spots_tofilter = (ISpotsPrx) EasyXT.Utils.castToType(aItem);
                ISpotsPrx spots_filtered ;
                spots_filtered = copySpots(spots_tofilter, filteredIds);
                aItemFiltered = spots_filtered;
            } else if (factory.IsSurfaces(aItem)) {
                // CopySurfaces requires a int[] so need to convert the List
                int[] filteredIds = filteredIdsList.stream().mapToInt(i -> i).toArray();
                ISurfacesPrx surfacesToFilter = (ISurfacesPrx) EasyXT.Utils.castToType(aItem);
                ISurfacesPrx surfacesFiltered ;
                surfacesFiltered = surfacesToFilter.CopySurfaces(filteredIds);
                aItemFiltered = surfacesFiltered;
            }

            return aItemFiltered;

        }

        /**
         * Returns an Imaris Object filtered with a test minValue &lt; value for a defined columnName
         *
         * @param aItem      the item to filter
         * @param columnName ColumnName as displayed in ImageJ Results Table you got from @EasyXT.Stats.export()
         * @param minValue   the minimum value
         * @return filteredSurface the filtered surface
         * @throws Error an Imaris Error
         */
        public static IDataItemPrx filterAbove(IDataItemPrx aItem, String columnName, double minValue) throws Error {

            IDataItemPrx filteredItem = EasyXT.Utils.filter(aItem, columnName, minValue, Double.MAX_VALUE);

            return filteredItem;

        }

        /**
         * Returns an Imaris Object filtered with a test value &gt; maxValue for a defined columnName
         *
         * @param aItem      the item to filter
         * @param columnName ColumnName as displayed in ImageJ Results Table you got from @EasyXT.Stats.export()
         * @param maxValue   the maximum value
         * @return filteredSurface the filtered surface
         * @throws Error an Imaris Error
         */
        public static IDataItemPrx filterBelow(IDataItemPrx aItem, String columnName, double maxValue) throws Error {

            IDataItemPrx filteredItem = EasyXT.Utils.filter(aItem, columnName, -1 * Double.MAX_VALUE, maxValue);

            return filteredItem;

        }

        /**
         * Returns an Imaris spots
         *
         * @param spots       the item to filter
         * @param filteredIds the Ids of the element to copy
         * @return filteredSpots the filtered spots
         * @throws Error an Imaris Error
         */

        public static ISpotsPrx copySpots(ISpotsPrx spots, long[] filteredIds) throws Error {

            long[] ids = spots.GetIds();
            float[][] coords = spots.GetPositionsXYZ();
            int[] t = spots.GetIndicesT();
            float[] rad = spots.GetRadii();
            float[][] rads = spots.GetRadiiXYZ();


            float[][] filtered_coords = new float[filteredIds.length][];
            int[] filtered_t = new int[filteredIds.length];
            float[] filtered_rad = new float[filteredIds.length];
            float[][] filtered_rads = new float[filteredIds.length][];

            for (int i = 0; i < filteredIds.length; i++) {
                // find the filteredIds in ids to get the Index
                int idx = ArrayUtils.indexOf(ids, filteredIds[i]);
                filtered_coords[i] = coords[idx];
                filtered_t[i] = t[idx];
                filtered_rad[i] = rad[idx];
                filtered_rads[i] = rads[idx];
            }
            /*
            List<float[]> coords_list = new ArrayList<>();
            List<Integer> t_list = new ArrayList<>();
            List<Float> rad_list = new ArrayList<>();
            List<float[]> rads_list = new ArrayList<>();

            for (int i = 0; i < filteredIds.length; i++) {
                // find the filteredIds in ids to get the Index
                int idx = ArrayUtils.indexOf(ids, filteredIds[i]);
                // get all infos we need
                coords_list.add(coords[idx]);
                t_list.add(t[idx]);
                rad_list.add(rad[idx]);
                rads_list.add(rads[idx]);
            }

            // convert the List to Arrays
            float[][] filtered_coords = new float[coords_list.size()][];
            int i = 0;
            for (float[] f : coords_list) {
                filtered_coords[i++] = (f != null ? f : null);
            }

            int[] filtered_t;
            filtered_t = t_list.stream().mapToInt(j -> j).toArray();

            float[] filtered_rad;
            filtered_rad = ArrayUtils.toPrimitive(rad_list.toArray(new Float[0]), 0.0F);

            float[][] filtered_rads = new float[coords_list.size()][];
            i = 0;
            for (float[] f : rads_list) {
                filtered_rads[i++] = (f != null ? f : null);
            }*/

            // create new spots
            ISpotsPrx filteredSpots = Utils.getImarisApp().GetFactory().CreateSpots();
            filteredSpots.Set(filtered_coords, filtered_t, filtered_rad);
            // Radii can only be set in XYZ afterwards
            filteredSpots.SetRadiiXYZ(filtered_rads);

            return filteredSpots;
        }

        /**
         * returns the Imaris enum value corresponding to the requested bit depth
         * Mostly useful internally when creating dataets
         *
         * @param bitDepth the bitdepth (8,16 or 32)
         * @return the corresponding Imaris bit depth type
         */
        private static tType getImarisDatasetType(int bitDepth) {
            switch (bitDepth) {
                case 8:
                    return tType.eTypeUInt8;
                case 16:
                    return tType.eTypeUInt16;
                case 32:
                    return tType.eTypeFloat;
                default:
                    return tType.eTypeUnknown;
            }
        }
    }
}
