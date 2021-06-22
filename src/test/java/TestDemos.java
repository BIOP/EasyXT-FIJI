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
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.demo.*;
import org.junit.Ignore;
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
    public void MakeSurfaceFromMaskDemo() throws Exception {
        MakeSurfaceFromMaskDemo.main(mode);
    }

    @Test
    public void TrackSurfaceDemo() throws Exception {
        MakeSurfaceFromMaskDemo.main(mode);
    }

}
