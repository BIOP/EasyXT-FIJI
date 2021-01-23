import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.demo.*;
import org.junit.Test;

/**
 * Execute each demo one at a time - no error should be thrown
 */
public class TestDemos {
private static String mode = "Test Mode";

    @Test
    public void AddChildObjects() throws Exception {
        AddChildObjects.main(mode);
    }

    @Test
    public void AddStatsDemo() throws Exception {
        AddStatsDemo.main(mode);
    }

    @Test
    public void AllGetters() throws Exception {
        AllGettersDemo.main(mode);
    }

    @Test
    public void FreshStartWithIJAndBIOPImsSample() throws Exception {
        FreshStartWithIJAndBIOPImsSample.main(mode);
    }

    @Test
    public void GetStatisticsDemo() throws Exception {
        GetStatisticsDemo.main(mode);
    }

    @Test
    public void LaunchIJFromEasyXT() throws Exception {
        EasyXT.main(mode);
    }

    @Test
    public void MakeAndGetSpotsDemo() throws Exception {
        MakeAndGetSpotsDemo.main(mode);
    }

    @Test
    public void MakeAndGetSurfaceDemo() throws Exception {
        MakeAndGetSurfaceDemo.main(mode);
    }

    @Test
    public void SendNewSurfaceDemo() throws Exception {
        ModifySurfaceDemo.main(mode);
    }

}
