import ch.epfl.biop.imaris.EasyXT
import ch.epfl.biop.imaris.StatsQuery

// Using StatsQuery to get global statistics

image_path = EasyXT.Samples.getImarisDemoFile("celldemo.ims")
println image_path

EasyXT.Files.openImage(image_path)

def volume = EasyXT.Scene.findItem( "Volume")
def globalStats = new StatsQuery( volume ).globalStats().get()
globalStats.show("Global Statistics")


