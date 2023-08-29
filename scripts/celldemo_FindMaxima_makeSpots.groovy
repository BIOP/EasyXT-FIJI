#@RoiManager rm
#@ResultsTable rt 
#@Double (value = 25.0) prominence
#@Double (value = 0.5) xRad_spot 
#@Double (value = 0.5) yRad_spot
#@Double (value = 1.0) zRad_spot
#@Boolean (value=false ) showImages

/*
 * This scripts demoes how to :
 * A - make use of Fiji to detect spots 
 * B - create Imaris spots from the Results Table
 */

// start-up cleaning
rt.reset()
rt.updateResults()
rm.reset();
IJ.run("Close All", "");
EasyXT.Utils.connectToImaris()

// Open an image in Imaris, 
// get it as an ImagePlus in Fiji 
// and get calibration 
def image_path = EasyXT.Samples.getImarisDemoFile("celldemo.ims")
EasyXT.Files.openImage(image_path)

def dataset = EasyXT.Dataset.getCurrent()
def imp = EasyXT.Dataset.getImagePlus( dataset )
if (showImages) imp.show()
def cal = imp.getCalibration();

def c3_imp = new Duplicator().run(imp, 3, 3, 1, 18, 1, 1);
if (showImages) c3_imp.show()

/** A - make use of Fiji to detect spots
 */

// We'll just apply a Median 3D to have less point
IJ.run(c3_imp, "Median 3D...", "x=2 y=2 z=2");

// Use a simple FindMaxima per Slice 
// (more advanced tools can be used like RS-FISH (https://github.com/PreibischLab/RS-FISH#download , update site : "Radial Symmetry") )
(1..imp.getNSlices()).each{ z ->
	c3_imp.setZ(z)
	IJ.run(c3_imp, "Find Maxima...", "prominence="+prominence+" output=[Point Selection]");
	def roi = c3_imp.getRoi();
	if (roi != null ){
		roi.setPosition(z);
		rm.addRoi(roi);
	}	
}

//Make sure to have the necessary measurements 
IJ.run("Set Measurements...", "area mean standard min centroid center stack display redirect=None decimal=3");
rm.runCommand(c3_imp,"Measure");


/** B - create Imaris spots from the Results Table
 */

// Couldn't make it work with "X" and "Y" :'( 
// use "XM, YM" instead ! 
xS = rt.getColumn("XM")
xS_cal = xS.collect{ (it + cal.xOrigin) * cal.pixelWidth }

yS = rt.getColumn("YM")
yS_cal = yS.collect{ (it + cal.yOrigin) * cal.pixelHeight }

zS = rt.getColumn("Slice") // Slices are "int", need to be scaled 
zS_cal = zS.collect{ (it + cal.zOrigin)  * cal.pixelDepth }

coordinates = (0..<rt.getCounter()).collect { it ->
 	pt = new Point3D()
 	pt.setX( xS_cal[it] as double )
 	pt.setY( yS_cal[it] as double )
 	pt.setZ( zS_cal[it] as double )
 	return pt
}

// To create spots we need a list of coordinates and a Point3D (or a list of Point3D) that defines the radius (xyz) of the spot object
//
// here all the spots will have same dimensions
radiusXYZ = new Point3D()
radiusXYZ.setX(xRad_spot as double )
radiusXYZ.setY(yRad_spot as double)
radiusXYZ.setZ(zRad_spot as double )

// Finally create the ImarisSpots 
spots = EasyXT.Spots.create(coordinates , radiusXYZ, 0)// single timepoint -> 0
EasyXT.Scene.setName(spots , "Spots_FindMaxima_Prominence-"+prominence )
EasyXT.Scene.addItem(spots)

println ("Processing Done !")


// imports
import ij.IJ
import ch.epfl.biop.imaris.EasyXT
import ij.plugin.Duplicator
import mcib3d.geom.Point3D // update site 3D Image Suite
