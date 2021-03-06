/*
 * Copyright 2013-2014 SmartBear Software
 * Copyright 2014-2015 The TestFX Contributors
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence"); You may
 * not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the Licence for the
 * specific language governing permissions and limitations under the Licence.
 */
package org.testfx.api;

import javafx.application.Application;
import javafx.stage.Stage;

import org.testfx.api.annotation.Unstable;
import org.testfx.toolkit.PrimaryStageApplication;
import org.testfx.toolkit.PrimaryStageFuture;

import static java.lang.Long.parseLong;
import static java.lang.System.getProperty;

/**
 * Stores the contextual information for {@link FxToolkit}:
 * <ul>
 *     <li>the {@link PrimaryStageFuture}</li>
 *     <li>the {@link Application} as a {@link Class} object</li>
 *     <li>the application's arguments</li>
 *     <li>the registered {@link Stage}</li>
 *     <li>the timeout limit for launching an application</li>
 *     <li>the timeout limit for setting up a component</li>
 * </ul>
 */
@Unstable(reason = "class was recently added")
public class FxToolkitContext {

    //---------------------------------------------------------------------------------------------
    // PRIVATE FIELDS.
    //---------------------------------------------------------------------------------------------

    /**
     * The {@link java.util.concurrent.Future Future&lt;Stage&gt;} that can run listeners when completed.
     * Default value: {@link PrimaryStageApplication#PRIMARY_STAGE_FUTURE}.
     */
    private PrimaryStageFuture primaryStageFuture = PrimaryStageApplication.PRIMARY_STAGE_FUTURE;

    /**
     * The {@link Application} as a {@link Class} object to use in {@link Application#launch(Class, String...)}.
     * Default value: {@link PrimaryStageApplication}.
     */
    private Class<? extends Application> applicationClass = PrimaryStageApplication.class;

    /**
     * The application arguments. Default value: an empty {@code String[]}
     */
    private String[] applicationArgs = new String[] {};

    private Stage registeredStage;

    /**
     * The number of milliseconds before timing out launch-related components. Default value: 60,000 (1 minute)
     */
    private long launchTimeoutInMillis = parseLong(getProperty("testfx.launch.timeout", "60000"));

    /**
     * The number of milliseconds before timing out setup-related components. Default value: 30,000 (30 seconds)
     */
    private long setupTimeoutInMillis = parseLong(getProperty("testfx.setup.timeout", "30000"));

    //---------------------------------------------------------------------------------------------
    // GETTER AND SETTER.
    //---------------------------------------------------------------------------------------------

    public PrimaryStageFuture getPrimaryStageFuture() {
        return primaryStageFuture;
    }

    public void setPrimaryStageFuture(PrimaryStageFuture primaryStageFuture) {
        this.primaryStageFuture = primaryStageFuture;
    }

    public Class<? extends Application> getApplicationClass() {
        return applicationClass;
    }

    public void setApplicationClass(Class<? extends Application> applicationClass) {
        this.applicationClass = applicationClass;
    }

    public String[] getApplicationArgs() {
        return applicationArgs;
    }

    public void setApplicationArgs(String[] applicationArgs) {
        this.applicationArgs = applicationArgs;
    }

    public Stage getRegisteredStage() {
        return registeredStage;
    }

    public void setRegisteredStage(Stage registeredStage) {
        this.registeredStage = registeredStage;
    }

    public long getLaunchTimeoutInMillis() {
        return launchTimeoutInMillis;
    }

    public void setLaunchTimeoutInMillis(long launchTimeoutInMillis) {
        this.launchTimeoutInMillis = launchTimeoutInMillis;
    }

    public long getSetupTimeoutInMillis() {
        return setupTimeoutInMillis;
    }

    public void setSetupTimeoutInMillis(long setupTimeoutInMillis) {
        this.setupTimeoutInMillis = setupTimeoutInMillis;
    }

}
