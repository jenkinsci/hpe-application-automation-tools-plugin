package com.hpe.application.automation.tools.model;

import com.hpe.application.automation.tools.model.EnumDescription;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Created by shepshel on 5/24/2017.
 */
public class CreateTunnelModel {
    public final static EnumDescription runModeLocal = new EnumDescription(
            "RUN_LOCAL", "Run locally");
    public final static EnumDescription runModePlannedHost = new EnumDescription(
            "RUN_PLANNED_HOST", "Run on planned host");
    public final static EnumDescription runModeRemote = new EnumDescription(
            "RUN_REMOTE", "Run remotely");
    public final static List<EnumDescription> runModes = Arrays.asList(
            runModeLocal, runModePlannedHost, runModeRemote);

    private final String srfTunnelName;

    @DataBoundConstructor
    public CreateTunnelModel(String srfTunnelName) {

        this.srfTunnelName = srfTunnelName;
    }



    public String getSrfTunnelName() {
        return srfTunnelName;
    }

}

