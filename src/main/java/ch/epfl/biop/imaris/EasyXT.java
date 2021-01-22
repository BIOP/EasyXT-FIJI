/**
 * Copyright (c) 2020 Ecole Polytechnique Fédérale de Lausanne. All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package ch.epfl.biop.imaris;


import Ice.ObjectPrx;
import Imaris.Error;
import Imaris.*;
import com.bitplane.xt.IceClient;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.plugin.ImageCalculator;
import ij.process.*;
import mcib3d.geom.ObjectCreator3D;
import mcib3d.geom.Vector3D;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
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

/**
 * NOTES: This approach might add more things to EasyXT, but makes work in parallel a bit more complicated
 * Nice, can have the same method names, like Spots.getMask() and Surfaces.getMask()
 * TODO: How to add autocomplete for EasyXT, Romain contacts Robert Haase
 * check position of logger
 */

/**
 * Main EasyXT Static class
 *
 * @author Olivier Burri
 * @author Nicolas Chiaruttini
 * @author Romain Guiet
 * This is the main static class you should access when you want to interact with Imaris.
 */
public class EasyXT {
    private static final IApplicationPrx APP;
    private static final String M_END_POINTS = "default -p 4029";
    public static Map<tType, Integer> datatype;
    public static Logger log = Logger.getLogger(EasyXT.class.getName());
    private static IceClient mIceClient;

    /**
     * Static initialisation :
     * Gets the Imaris server
     * Ensure the connection is closed on JVM closing
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

    // TODO Refactor & Comment
    private static void CloseIceClient() {
        if (mIceClient != null) {
            mIceClient.Terminate();
            mIceClient = null;
        }
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

    // Image Management Methods

    /**
     * openImage, opens the file from filepath in a new imaris scene
     *
     * @param filepath path  to an *.ims file
     * @param options  option string cf : xtinterface/structImaris_1_1IApplication.html/FileOpen
     * @throws Error
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
        getImarisApp().FileOpen(filepath.getAbsolutePath(), options);

    }

    /**
     * overloaded method, see {@link #openImage(File, String)}
     *
     * @param filepath to an *.ims file
     * @throws Error
     */
    public static void openImage(File filepath) throws Error {
        openImage(filepath, "");
    }

    /**
     * Saves the current imaris scene to an imaris file
     *
     * @param filepath path to save ims file
     * @param options  option string cf : xtinterface/structImaris_1_1IApplication.html/FileSave eg writer="BMPSeries".
     *                 List of formats available: Imaris5, Imaris3, Imaris2,SeriesAdjustable, TiffSeriesRGBA, ICS,
     *                 OlympusCellR, OmeXml, BMPSeries, MovieFromSlices.
     * @throws Error
     */
    public static void saveImage(File filepath, String options) throws Error {
        if (!filepath.getName().endsWith("ims")) {
            filepath = new File(filepath.getAbsoluteFile() + ".ims");
            log.info("Saved as : " + filepath.getAbsoluteFile());
        }
        getImarisApp().FileSave(filepath.getAbsolutePath(), options);
    }

    /**
     * overloaded method , see {@link #saveImage(File, String)}
     *
     * @param filepath path to save ims file
     * @throws Error
     */
    public static void saveImage(File filepath) throws Error {
        saveImage(filepath, "");
    }

    /**
     * Gets the name of the currently open imaris file, as shown on the top of the Imaris window
     *
     * @return the name of the currently open imaris file
     * @throws Error
     */
    public static String getOpenImageName() throws Error {
        // TODO: check about changing name. It is not the image, it is the imaris file name. The file may contain more than one image.
        return new File(getImarisApp().GetCurrentFileName()).getName();
    }

    /**
     * This Class takes care of downloading and using sample files
     */
    public static class Samples {

        // This is a url pointing to a sample dataset for EasyXT
        private static final String SAMPLE_IMAGE_URL = "https://zenodo.org/record/4449687/files/HeLa_H2BmCherry_GFPtubulin_Mitotracker.ims";

        /**
         * returns a File pointing to a demo dataset that can be used with {@link EasyXT#openImage(File)}
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
                    Files.copy(in, destPath, StandardCopyOption.REPLACE_EXISTING);
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
         * @return
         */
        public static Color getColorIntFromIntArray(int[] color) {

            if (color.length < 3) {
                log.warning("You did not provide enough colors. need 3. Returning white.");
                return new Color(255, 255, 255);
            }
            return new Color(color[0], color[1], color[2]);
        }

    }

    /**
     * This class handles all queries to the surpass scene and to its related Items
     * Method to find spots, surfaces, groups, or arbitrary objects are found inside
     * Methods to get or set item names
     */
    public static class Scene {

        /**
         * Returns a reference to the Surpass Scene, which is basically the Parent Group
         * @return
         * @throws Error
         */
        public static IDataContainerPrx getScene() throws Error {
            return getImarisApp().GetSurpassScene();
        }

        /**
         * Get the name of the requested item, to avoid using GetName()
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
         * Return the first item with the selected name inside the given parent object
         * Returns null if not found
         *
         * @param name   the name of the item to find
         * @param parent the parent object, a group
         * @return the requested item, null if not found
         * @throws Error an Imaris Error Object
         */
        public static IDataItemPrx getItem(String name, IDataContainerPrx parent) throws Error {
            ItemQuery query = new ItemQuery.ItemQueryBuilder().setName(name).setParent(parent)
                    .build();

            return query.get(0);
        }

        /**
         * Return the first item with the selected name inside the surpass scene
         * Returns null if not found
         *
         * @param name the name of the item to find
         * @return the requested item, null if not found
         * @throws Error an Imaris Error Object
         */
        public static IDataItemPrx getItem(String name) throws Error {
            // If you use parent as null, it is the same as getImarisApp.GetSurpassScene()
            return getItem(name, null);
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
        public static ISpotsPrx getSpots(String name, IDataContainerPrx parent) throws Error {
            ItemQuery query = new ItemQuery.ItemQueryBuilder()
                    .setName(name)
                    .setType("Spots")
                    .setParent(parent)
                    .build();

            return (ISpotsPrx) query.get(0);
        }

        /**
         * returns the first spots object in the main surpass scene with the given name
         *
         * @param name the name of the spots object to get. Returns the first spots object if there are multiple spots with
         *             the same name (Don't do that)
         * @return the requested spots, null if not found
         * @throws Error an Imaris Error Object
         */
        public static ISpotsPrx getSpots(String name) throws Error {
            return getSpots(name, Scene.getScene());
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
        public static ISurfacesPrx getSurfaces(String name, IDataContainerPrx parent) throws Error {
            ItemQuery query = new ItemQuery.ItemQueryBuilder().setName(name).setParent(parent).setType("Surfaces").build();

            return (ISurfacesPrx) query.get(0);
        }

        public static ISurfacesPrx getSurfaces(String name) throws Error {
            return getSurfaces(name, null);
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
        public static List<IDataItemPrx> getAll(String type, IDataContainerPrx parent) throws Error {
            ItemQuery query = new ItemQuery.ItemQueryBuilder().setType(type).setParent(parent).build();
            return query.get();
        }

        /**
         * Get all items of the requested type in the main scene as a list (not within subfolder, groups)
         *
         * @param type the type, defined by a String. See {@link ItemQuery.ItemType}
         * @return a list containins the objects
         * @throws Error an Imaris Error Object
         */
        public static List<IDataItemPrx> getAll(String type) throws Error {
            return getAll(type, null);
        }

        /**
         * Get all spots objects in the main scene as a list (not within a group)
         *
         * @return the properly cast spots as a list
         * @throws Error an Imaris Error Object
         */
        public static List<ISpotsPrx> getAllSpots() throws Error {
            List<IDataItemPrx> items = getAll("Spots");

            // Explicitly cast to spots
            List<ISpotsPrx> spots = items.stream().map(item -> {
                return (ISpotsPrx) item;
            }).collect(Collectors.toList());

            return spots;
        }

        /**
         * Get all surfaces objects in the main scene as a list (not within subfolder, groups)
         *
         * @return the surfaces as a list
         * @throws Error an Imaris Error Object
         */
        public static List<ISurfacesPrx> getAllSurfaces() throws Error {
            List<IDataItemPrx> items = getAll("Surfaces");

            // Explicitly cast
            List<ISurfacesPrx> surfs = items.stream().map(item -> {
                return (ISurfacesPrx) item;
            }).collect(Collectors.toList());

            return surfs;
        }

        /**
         * Get all Group objects in the main scene as a list
         *
         * @return the surfaces as a list
         * @throws Error an Imaris Error Object
         */
        public static List<IDataContainerPrx> getAllGroups() throws Error {
            ItemQuery query = new ItemQuery.ItemQueryBuilder().setType("DataContainer").build();
            List<IDataItemPrx> items = query.get();

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
        public static void addToScene(IDataItemPrx item) throws Error {
            addToScene(Scene.getScene(), item);
        }

        /**
         * Adds the provided item as the last child to the provided parent item
         *
         * @param parent The parent item
         * @param item   the item to add as a child
         * @throws Error an Imaris Error Object
         */
        public static void addToScene(IDataContainerPrx parent, IDataItemPrx item) throws Error {
            parent.AddChild(item, -1); // last element is position. -1 to append at the end.
        }

        /**
         * Removes the provided item from its parent,
         * if it's a Group, it removes the children explicitly as well. Unsure of necessary
         *
         * @param item the item in question
         * @throws Error an Imaris Error Object
         */
        public static void removeFromScene(IDataItemPrx item) throws Error {
            // if the item is a group
            IFactoryPrx factory = APP.GetFactory();
            if (factory.IsDataContainer(item)) {
                IDataContainerPrx group = factory.ToDataContainer(item);
                // make sure to remove all elements in it
                for (int grp = 0; grp < group.GetNumberOfChildren(); grp++) {
                    removeFromScene(group.GetChild(grp));
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
        public static void removeFromScene(List<? extends IDataItemPrx> items) throws Error {
            for (IDataItemPrx it : items) {
                removeFromScene(it);
            }
        }

        /**
         * Reset the Imaris Scene
         *
         * @throws Error an Imaris Error Object
         */
        public static void resetScene() throws Error {
            List<ISpotsPrx> spots = getAllSpots();
            List<ISurfacesPrx> surfaces = getAllSurfaces();
            List<IDataContainerPrx> groups = getAllGroups();
            removeFromScene(spots);
            removeFromScene(surfaces);
            removeFromScene(groups);
            //for ( ISpotsPrx sp: spots ) { EasyXT.removeFromScene( sp ); }
            //for ( ISurfacesPrx srf: surfaces ) { EasyXT.removeFromScene( srf ); }
            //for ( IDataContainerPrx grp: groups ) { EasyXT.removeFromScene( grp ); }

            // TODO selectScene
            // From Oli: does this do it?
            selectItem(Scene.getScene());

        }

        /**
         * Creates a "Group" (folder) that can contain other items
         *
         * @param groupName the name to identify the group with
         * @return an item that can be added to a scene ({@link Scene#addToScene(IDataItemPrx)}) or to which other items
         * can be added as children {@link Scene#addToScene(IDataContainerPrx, IDataItemPrx)}
         * IDataContainerPrx extends IDataItemPrx
         * @throws Error
         */
        public static IDataContainerPrx createGroup(String groupName) throws Error {
            IDataContainerPrx group = getImarisApp().GetFactory().CreateDataContainer();
            group.SetName(groupName);
            return group;
        }

        /**
         * Selects the given item on the Imaris Surpass Scene GUI.
         *
         * @param item
         */
        public static void selectItem(IDataItemPrx item) throws Error {
            getImarisApp().SetSurpassSelection(item);
        }
    }

    /**
     * This sublass deals with direct Imaris Datasets, not surface-based datasets
     * This includes methods to transfer, convert and move datasets around Imaris and Fiji
     */
    public static class Dataset {

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
         * (Adapted from existing function in EasyXT-Matlab)
         * <p>
         * TODO discuss parameter as int, thought it would be nice to directly use it with {@link #getBitDepth(IDataSetPrx)} and imp.getBitDepth()
         *
         * @param bitDepth
         * @throws Error
         */
        public static void setBitDepth(IDataSetPrx dataset, int bitDepth) throws Error {
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
         * @param bitDepth
         */
        public static void setBitDepth(int bitDepth) throws Error {
            IDataSetPrx dataset = Dataset.getCurrentDataset();
            setBitDepth(dataset, bitDepth);
        }

        /**
         * Returns an ImagePlus image of a dataset TODO : add a way to select only a subpart of it
         *
         * @param dataset
         * @return
         * @throws Error
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
            ImagePlus imp = new ImagePlus(getImarisApp().GetCurrentFileName(), stack);
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
            imp = HyperStackConverter.toHyperStack(imp, nc, nz, nt);

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
         * Set data from an ImagePlus image into a dataset TODO : add a way to select only a subpart of it
         *
         * @param dataset the dataset to insert the imagePlus into
         * @throws Error an Imaris Error Object
         */
        public static void setImagePlus(IDataSetPrx dataset, ImagePlus imp) throws Error {
            ImarisCalibration cal = new ImarisCalibration(dataset);

            // TODO: Ensure that the image is not larger than the dataset

            int w = cal.xSize;
            int h = cal.ySize;

            int nc = cal.cSize;
            int nz = cal.zSize;
            int nt = cal.tSize;

            int bitDepth = getBitDepth(dataset);

            for (int c = 0; c < nc; c++) {
                for (int z = 0; z < nz; z++) {
                    for (int t = 0; t < nt; t++) {
                        int idx = imp.getStackIndex(c + 1, z + 1, t + 1);
                        ImageProcessor ip = imp.getStack().getProcessor(idx);
                        switch (bitDepth) {
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

            // TODO: Set colors and range of channels based on imageplus
        }

        /**
         * Convenience method to return the currently active dataset
         *
         * @return a IDatasetPrx object containing a reference to all the pixel data
         * @throws Error an Imaris Error Object
         */
        public static IDataSetPrx getCurrentDataset() throws Error {
            return getImarisApp().GetDataSet();
        }

        /**
         * Convenience method to set/replace the current dataset within the current Imaris scene with the one provided
         *
         * @param dataset a IDataSetPrx object containing a reference to all the pixel data
         * @throws Error an Imaris Error Object
         */
        public static void setCurrentDataset(IDataSetPrx dataset) throws Error {
            getImarisApp().SetDataSet(dataset);
        }

        /**
         * Adds the selected ImagePlus to the current Dataset by appending it as new channels If the dataset is visible in
         * the Imaris Scene, this is a lot slower
         *
         * @param imp the image to add to the current dataset
         * @throws Error an Imaris Error object
         */
        public static void addChannels(ImagePlus imp) throws Error {

            IDataSetPrx dataset = EasyXT.Dataset.getCurrentDataset();
            addChannels(dataset, imp, 0, 0, 0, 0);
        }

        /**
         * Adds the selected ImagePlus to the provided IDatasetPrx by appending it as new channels
         *
         * @param dataset
         * @param imp     the image to add to the current dataset
         * @throws Error an Imaris Error object
         */
        public static void addChannels(IDataSetPrx dataset, ImagePlus imp) throws Error {
            addChannels(dataset, imp, 0, 0, 0, 0);
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
         * The user can define the start location XYZT in pixel coordinates
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
        public static void addChannels(IDataSetPrx dataset, ImagePlus imp, int xStart, int yStart, int zStart, int tStart) throws Error {

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
                errorDetail += "\nImage:" + iBitDepth + "-bit";
                // TODO forced conversion below, could or couldn't work, eg 8 -> 16 ok but 16->8 no!
                log.warning("Bit Depth Mismatch : Imaris Dataset and Fiji ImagePlus do not have same bit depth \n " + errorDetail);
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
                int idx = imp.getStackIndex(c + 1, 1, 1);
                int color = imp.getStack().getProcessor(idx).getColorModel().getRGB(255);

                dataset.SetChannelColorRGBA(c, color);

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
    }

    public static class Surfaces {
        /**
         *
         * @param channel
         * @return
         * @throws Error
         */
        public static SurfacesDetector.SurfacesDetectorBuilder create(int channel) throws Error {
            return SurfacesDetector.Channel(channel);
        }

        //TODO make new create() method to create a surfaces object from an imageplus

        /**
         * Get all surfaces objects in the main scene as a list (not within subfolder, groups)
         *
         * @return the surfaces as a list
         * @throws Error an Imaris Error Object
         */
        public static List<ISurfacesPrx> getAllSurfaces() throws Error {
            return Scene.getAllSurfaces();
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
        public static ISurfacesPrx getSurfaces(String name, IDataContainerPrx parent) throws Error {
            return Scene.getSurfaces(name, parent);
        }

        public static ISurfacesPrx getSurfaces(String name) throws Error {
            return Scene.getSurfaces(name);
        }

        /**
         *
         * @param surface
         * @return
         * @throws Error
         */
        public static IDataSetPrx getSurfacesDataset(ISurfacesPrx surface) throws Error {

            IDataSetPrx finalDataset = Dataset.getCurrentDataset().Clone();
            ImarisCalibration cal = new ImarisCalibration(finalDataset);

            // Check if there are channels
            finalDataset.SetSizeC(1);

            // Why do we set it to 8 bit? What type is it?
            Dataset.setBitDepth(finalDataset, 8);

            // Loop through each timepoint, and get the dataset, then replace
            for (int t = 0; t < cal.tSize; t++) {
                IDataSetPrx oneTimepoint = getSurfacesDataset(surface, 1.0, t);
                finalDataset.SetDataVolumeAs1DArrayBytes(oneTimepoint.GetDataVolumeAs1DArrayBytes(0, 0), 0, t);
            }

            return finalDataset;
        }

        /**
         *
         * @param surface
         * @return
         * @throws Error
         */
        public static ImagePlus getSurfacesMask(ISurfacesPrx surface) throws Error {

            // Get raw ImagePlus
            ImagePlus impSurface = Dataset.getImagePlus(getSurfacesDataset(surface));

            // Multiply by 255 to allow to use ImageJ binary functions
            int nProcessor = impSurface.getStack().getSize();
            IntStream.range(0, nProcessor).parallel().forEach(index -> {
                impSurface.getStack().getProcessor(index + 1).multiply(255);
            });

            // Set LUT and display range
            impSurface.setLut(LUT.createLutFromColor(Utils.getColorFromInt(surface.GetColorRGBA())));
            impSurface.setDisplayRange(0, 255);
            impSurface.setTitle(surface.GetName());

            return impSurface;
        }

        // TODO Comment
        public static void setSurfacesMask(ISurfacesPrx surface, ImagePlus imp) throws Error {
            ImarisCalibration cal = new ImarisCalibration(getImarisApp().GetDataSet());

            // Divide by 255 to allow to use ImageJ binary functions
            int nProcessor = imp.getStack().getSize();
            IntStream.range(0, nProcessor).parallel().forEach(index -> {
                imp.getStack().getProcessor(index + 1).multiply(1.0 / 255.0);
            });

            surface.RemoveAllSurfaces();

            for (int t = 0; t < cal.tSize; t++) {
                IDataSetPrx dataset = getSurfacesDataset(surface, 1, t);
                Dataset.setBitDepth(dataset, 8);

                // temporary ImagePlus required to work!
                ImagePlus tImp = new Duplicator().run(imp, 1, 1, 1, cal.zSize, t + 1, t + 1);
                //t_imp.show();
                Dataset.setImagePlus(dataset, tImp);

                surface.AddSurface(dataset, t);
            }

        }

        // TODO Comment
        public static ImagePlus getSurfacesMask(ISurfacesPrx surface, int timepoint) throws Error {
            return getSurfacesMask(surface, 1.0, timepoint);
        }

        // TODO Comment
        public static ImagePlus getSurfacesMask(ISurfacesPrx surface, double downsample, int timepoint) throws Error {

            ImarisCalibration cal = new ImarisCalibration(getImarisApp().GetDataSet()).getDownsampled(downsample);

            IDataSetPrx data = getSurfacesDataset(surface, downsample, timepoint);

            ImagePlus imp = Dataset.getImagePlus(data);
            imp.setCalibration(cal);

            return imp;
        }

        // TODO Comment
        public static IDataSetPrx getSurfacesDataset(ISurfacesPrx surface, double downsample, int timepoint) throws Error {
            ImarisCalibration cal = new ImarisCalibration(getImarisApp().GetDataSet()).getDownsampled(downsample);

            IDataSetPrx data = surface.GetMask((float) cal.xOrigin, (float) cal.yOrigin, (float) cal.zOrigin,
                    (float) cal.xEnd, (float) cal.yEnd, (float) cal.zEnd,
                    cal.xSize, cal.ySize, cal.zSize, timepoint);
            return data;
        }

        /**
         * Return an Label image of the corresponding surface
         *
         * @param surface a surface object see {@link Scene#getSurfaces(String)}
         * @return a Label image (ImagePlus)
         * @throws Error
         */
        //
        //
        public static ImagePlus getSurfacesLabel(ISurfacesPrx surface) throws Error {

            IDataSetPrx dataset = getImarisApp().GetDataSet();

            //int dBitDepth = Dataset.getBitDepth(dataset);

            ImarisCalibration cal = new ImarisCalibration(dataset);

            int numberOfSurfaces = surface.GetNumberOfSurfaces();
            int lastTimepoint = surface.GetTimeIndex(numberOfSurfaces - 1);
            long[] ids = surface.GetIds();

            // Get the whole surface as ImagePlus (0 or 1 ), 8-bit image
            ImagePlus labelImp = Dataset.getImagePlus(getSurfacesDataset(surface));
            if (ids[numberOfSurfaces - 1] > 255) IJ.run(labelImp, "16-bit", "");
            else if (ids[numberOfSurfaces - 1] > 65535) IJ.run(labelImp, "32-bit", "");

            ArrayList<ImagePlus> imps = new ArrayList<ImagePlus>(lastTimepoint);

            // we extract the first timepoint
            int previousT = 0;
            ImagePlus tLabelImp = new Duplicator().run(labelImp, 1, 1, 1, labelImp.getNSlices(), previousT + 1, previousT + 1);

            ImageCalculator ic = new ImageCalculator();
            // next will mutiply each sub-surface of the surface, by a int value
            for (int srf = 0; srf < numberOfSurfaces; srf++) {
                // should be final for the processor step below
                final int val = (int) ids[srf];

                // if the current spot is from a different time-point, or if it's the last spot
                if ((cal.tSize > 1) && ((surface.GetTimeIndex(srf) != previousT) || (srf == numberOfSurfaces - 1))) {
                    // store the current status of t_label_imp into the ArrayList<ImagePlus>
                    // N.B. duplicate is required to store the current time-point
                    imps.add(new ImagePlus("t" + previousT, tLabelImp.getStack().duplicate()));
                    // ... increment previous_t value
                    previousT++;
                    // and set the t_label_imp to the next time-point
                    tLabelImp = new Duplicator().run(labelImp, 1, 1, 1, labelImp.getNSlices(), previousT + 1, previousT + 1);
                }

                // First we tried to use CopySurfaces(id), but it duplicates the Surface
                // (visible with color based on statistics that make surface display to crash)
                // int[] id = {i};
                // ISurfacesPrx current_surface = surface.CopySurfaces(id);
                // IDataSetPrx dataset = getSurfacesDataset(current_surface);

                // here we make a copy of the whole stack
                // To go faster we could use a smaller mask by defining the boundingBox of each sub-surface
                // (but it will also be needed to at the multiply step below)
                IDataSetPrx currentDataset = surface.GetSingleMask(srf,
                        (float) cal.xOrigin, (float) cal.yOrigin, (float) cal.zOrigin,
                        (float) cal.xEnd, (float) cal.yEnd, (float) cal.zEnd,
                        cal.xSize, cal.ySize, cal.zSize);
                ImagePlus currentImp = Dataset.getImagePlus(currentDataset);
                // Change current_imp bit depth to handle larger number of surfaces
                if (ids[numberOfSurfaces - 1] > 255) IJ.run(currentImp, "16-bit", "");
                else if (ids[numberOfSurfaces - 1] > 65535) IJ.run(currentImp, "32-bit", "");

                // Multiply by val to get a Label
                // val could could be replaced by the surface-Imaris-ID )
                int nProcessor = currentImp.getStack().getSize();
                IntStream.range(0, nProcessor).parallel().forEach(index -> {
                    currentImp.getStack().getProcessor(index + 1).multiply(val);
                });

                //now we add the the current_imp to the global t_label_imp
                // using Transparent-zero to take care of interface between touching objects
                ic.run("Transparent-zero stack", tLabelImp, currentImp);
                currentImp.changes = false;
                currentImp.close();

                log.info("label " + (srf + 1) + "/" + numberOfSurfaces + ", val : " + val);

                // set the previous_t
                previousT = surface.GetTimeIndex(srf);
            }

            if (cal.tSize > 1) {
                // https://stackoverflow.com/questions/9572795/convert-list-to-array-in-java
                ImagePlus[] impsArray = imps.toArray(new ImagePlus[0]);
                labelImp = Concatenator.run(impsArray);
            } else {
                labelImp = new ImagePlus("t" + previousT, tLabelImp.getStack().duplicate());
            }

            labelImp.setTitle(getOpenImageName());
            labelImp.setCalibration(cal);

            return labelImp;
        }
    }

    public static class Spots {
        /**
         * TODO
         * @param channel
         * @return
         * @throws Error
         */
        public static SpotsDetector.SpotsDetectorBuilder create(int channel) throws Error {
            return SpotsDetector.Channel(channel);
        }

        /**
         * Get all spots objects in the main scene as a list (not within a group)
         *
         * @return the properly cast spots as a list
         * @throws Error an Imaris Error Object
         */
        public static List<ISpotsPrx> getAllSpots() throws Error {
            return Scene.getAllSpots();
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
        public static ISpotsPrx getSpots(String name, IDataContainerPrx parent) throws Error {
            return Scene.getSpots(name, parent);
        }

        /**
         * returns the first spots object in the main surpass scene with the given name
         *
         * @param name the name of the spots object to get. Returns the first spots object if there are multiple spots with
         *             the same name (Don't do that)
         * @return the requested spots, null if not found
         * @throws Error an Imaris Error Object
         */
        public static ISpotsPrx getSpots(String name) throws Error {
            return Scene.getSpots(name);
        }

        /**
         * Get an ImagePlus of the spots as a mask (255)
         *
         * @param spots a spots object see {@link  Scene#getSpots(String, IDataContainerPrx)}
         * @return an ImagePlus
         * @throws Error
         */
        public static ImagePlus getSpotsMask(ISpotsPrx spots) throws Error {
            return getSpotsImage(spots, false, false);
        }

        /**
         * Get an ImagePlus of the spots as a label (object has Imaris-ID value)
         *
         * @param spots a spots object see {@link  Scene#getSpots(String)}
         * @return an ImagePlus
         * @throws Error
         */
        public static ImagePlus getSpotsLabel(ISpotsPrx spots) throws Error {
            return getSpotsImage(spots, true, false);
        }

        /**
         * Get an ImagePlus of the spots as a label (object has Imaris-ID value)
         *
         * @param spots     a spots object see {@link  Scene#getSpots(String)}
         * @param isValueId boolean to define if value will be 255 or Imaris-ID value
         * @param isGauss   boolean to decide to apply gaussian filter on each spot
         * @return and ImagePlus 16-bit
         * @throws Error
         */
        public static ImagePlus getSpotsImage(ISpotsPrx spots, boolean isValueId, boolean isGauss) throws Error {
            // Prepare the imp to receive spots pixels
            //
            // get the calibration info from Imaris
            ImarisCalibration cal = new ImarisCalibration(Dataset.getCurrentDataset());
            // Create a new imp with the cal and get stack ()
            //ImagePlus imp = IJ.createHyperStack( getOpenImageName(), cal.xSize , cal.ySize, cal.cSize, cal.zSize, cal.tSize , 32);
            //imp.setCalibration( cal );
            //ImageStack stack = imp.getStack();

            long[] spots_ids = spots.GetIds();
            float[][] spotsCenterXYZ = spots.GetPositionsXYZ();
            float[][] spotsRadiiXYZ = spots.GetRadiiXYZ();
            int[] spotsT = spots.GetIndicesT();

            // Define default vectors
            Vector3D vector3D1 = new Vector3D(1, 0, 0);
            Vector3D vector3D2 = new Vector3D(0, 1, 0);
            if (Math.abs(vector3D1.dotProduct(vector3D2)) > 0.001) {
                IJ.log("ERROR : vectors should be perpendicular");
            }

            int previousT = spotsT[0];
            ObjectCreator3D objCreator = new ObjectCreator3D(cal.xSize, cal.ySize, cal.zSize);
            objCreator.setResolution(cal.pixelWidth, cal.pixelDepth, cal.getUnit());

            ImagePlus finalImp;
            // by default the value is 255
            int val = 255;

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
                objCreator.createEllipsoidAxesUnit(spotsCenterXYZ[t][0], spotsCenterXYZ[t][1], spotsCenterXYZ[t][2], spotsRadiiXYZ[t][0], spotsRadiiXYZ[t][1], spotsRadiiXYZ[t][2], (float) val, vector3D1, vector3D2, isGauss);
                // set the previous_t
                previousT = spotsT[t];

                log.info("label " + t + "/" + spotsT.length);
            }

            if (cal.tSize > 1) {
                // https://stackoverflow.com/questions/9572795/convert-list-to-array-in-java
                ImagePlus[] impsArray = imps.toArray(new ImagePlus[0]);
                finalImp = Concatenator.run(impsArray);
            } else {
                finalImp = new ImagePlus("t" + previousT, objCreator.getStack().duplicate());
            }

            finalImp.setDisplayRange(0, val);
            finalImp.setTitle(getOpenImageName());
            finalImp.setCalibration(cal);

            if (!isValueId) {
                IJ.run(finalImp, "8-bit", "");
            }

            return finalImp;

        }
    }

    /**
     * This class handles getting statistics from Imaris as well as sending statistics back to Imaris
     */
    public static class Stats {
        // Statistics related functions

        /**
         * returns all available Imaris statistics from the selected item
         *
         * @param item the item to query
         * @return a ResultsTable to be used and displayed by ImageJ
         * @throws Error an Imaris Error Object
         */
        public static ResultsTable getStatistics(IDataItemPrx item) throws Error {
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
        public static ResultsTable getStatistics(IDataItemPrx item, String name) throws Error {
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
        public static ResultsTable getStatistics(IDataItemPrx item, String name, Integer channel) throws Error {
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
        public static ResultsTable getStatistics(IDataItemPrx item, List<String> names) throws Error {
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
        public static ResultsTable getStatistics(IDataItemPrx item, List<String> names, Integer channel) throws Error {
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
        public static ResultsTable getStatistics(IDataItemPrx item, List<String> names, List<Integer> channels) throws Error {
            return new StatsQuery(item)
                    .selectStatistics(names)
                    .selectChannels(channels)
                    .get();
        }

        /**
         * TODO
         * @param statistics
         * @param columnName
         * @return
         */
        public static Map<Long, Map<String, Double>> extractStatistic(ResultsTable statistics, String columnName) {
            return StatsQuery.extractStatistic(statistics, columnName);
        }

        /**
         * TODO
         * @param item
         * @param statName
         * @param statValues
         * @return
         */
        public static StatsCreator create(IDataItemPrx item, String statName, Map<Long, Map<String, Double>> statValues) {
            return new StatsCreator(item, statName, statValues);
        }
    }
}