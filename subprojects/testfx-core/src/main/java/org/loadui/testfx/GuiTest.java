/*
 * Copyright 2013-2014 SmartBear Software
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the Licence for the specific language governing permissions
 * and limitations under the Licence.
 */
package org.loadui.testfx;

import javafx.scene.Parent;
import org.junit.Before;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

public abstract class GuiTest extends FxTest {

    @Before
    public void internalSetup() throws Exception {
        FxToolkit toolkit = new FxToolkit();
        target(toolkit.registerPrimaryStage());
        toolkit.setupSceneRoot(() -> getRootNode());
        WaitForAsyncUtils.waitForFxEvents();
        toolkit.setupStage((stage) -> {
            stage.show();
            stage.toBack();
            stage.toFront();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    //---------------------------------------------------------------------------------------------
    // PROTECTED METHODS.
    //---------------------------------------------------------------------------------------------

    // Runs in JavaFX Application Thread.
    protected abstract Parent getRootNode();

}
