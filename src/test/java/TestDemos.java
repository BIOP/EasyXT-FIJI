import Imaris.Error;
import ch.epfl.biop.imaris.demo.*;
import org.junit.Test;

/**
 * Execute each demo one at a time - no error should be thrown
 */
public class TestDemos {

    @Test
    public void AddChildObjects() throws Exception {
        AddChildObjects.main("Test Mode");
    }

    @Test
    public void AddStatsDemo() throws Exception {
        AddStatsDemo.main("Test Mode");
    }

    @Test
    public void AllGetters() throws Exception {
        AllGetters.main("Test Mode");
    }

    @Test
    public void FreshStartWithIJAndBIOPImsSample() throws Exception {
        FreshStartWithIJAndBIOPImsSample.main("Test Mode");
    }

    @Test
    public void GetStatisticsDemo() throws Exception {
        GetStatisticsDemo.main("Test Mode");
    }

    @Test
    public void IJSimpleLaunch() throws Exception {
        IJSimpleLaunch.main("Test Mode");
    }

    @Test
    public void MakeAndGetSpotsDemo() throws Exception {
        MakeAndGetSpotsDemo.main("Test Mode");
    }

    @Test
    public void MakeAndGetSurfaceDemo() throws Exception {
        MakeAndGetSurfaceDemo.main("Test Mode");
    }

    @Test
    public void SendNewSurfaceDemo() throws Exception {
        SendNewSurfaceDemo.main("Test Mode");
    }

}
