#@ File image_path

import org.apache.commons.io.FilenameUtils

import ij.*
import ch.epfl.biop.imaris.*

/*
 *  Get image info : dir, name and create the output_dir
 */

dir = image_path.getParent()
image_name = image_path.getName()
image_basename = FilenameUtils.getBaseName( image_name )
//println dir + image_name

// create an output dir and prepare output file path
def output_dir = new File( dir , "output" )
output_dir.mkdirs()
output_path = new File( output_dir , image_basename+".ims" )


imp= IJ.openImage( image_path.toString() )

EasyXT.Scene.reset()
dataset = EasyXT.Dataset.create(imp)
EasyXT.Dataset.setCurrent( dataset )
EasyXT.Files.saveImage( output_path )

println "Saving "+ image_basename + " done!"


return
