/*
    (c) Copyright [2016] Hewlett Packard Enterprise Development LP

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
    documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
    rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
    persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
    Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
    OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.hpe.application.automation.tools.nv.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.application.automation.tools.nv.model.NvNetworkProfile;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

public class NvNetworkProfileRegistry {
    public static final String DEFAULT_PROFILES_FILE_NAME = "lib/nv/default_profiles.txt";
    public static final String NONE_PROFILE_NAME = "No Emulation";

    private static NvNetworkProfileRegistry instance;
    private Map<String, NvNetworkProfile> profiles = new HashMap<>();
    private Comparator<String> profileNameComparator = new ProfileNameComparator();

    public static NvNetworkProfileRegistry getInstance() {
        if (null == instance) {
            synchronized (NvNetworkProfileRegistry.class) {
                if (null == instance) {
                    instance = new NvNetworkProfileRegistry();
                }
            }
        }

        return instance;
    }

    private NvNetworkProfileRegistry() {
        registerDefaultProfiles();
    }

    private void registerDefaultProfiles() {
        InputStream is = NvNetworkProfileRegistry.class.getClassLoader().getResourceAsStream(DEFAULT_PROFILES_FILE_NAME);
        try {
            register(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void register(InputStream is) throws IOException {
        List<NvNetworkProfile> profiles = new ObjectMapper().readValue(is, new TypeReference<List<NvNetworkProfile>>() {
        });
        for (NvNetworkProfile profile : profiles) {
            profile.setCustom(false);
            this.profiles.put(profile.getProfileName(), profile);
        }
    }

    public synchronized void register(Collection<NvNetworkProfile> profiles) {
        if (null != profiles) {
            NvNetworkProfile existing;
            for (NvNetworkProfile profile : profiles) {
                existing = getNetworkProfile(profile.getProfileName());
                if (null == existing || existing.isCustom()) { // make sure to override only custom profiles
                    profile.setCustom(true);
                    this.profiles.put(profile.getProfileName(), profile);
                }
            }
        }
    }

    public void unregisterCustom() {
        List<String> profilesToRemove = new ArrayList<>();
        for (NvNetworkProfile profile : profiles.values()) {
            if (profile.isCustom()) {
                profilesToRemove.add(profile.getProfileName());
            }
        }

        for (String profileName : profilesToRemove) {
            profiles.remove(profileName);
        }
    }

    public ListBoxModel getNetworkProfilesAsListModel() {
        ListBoxModel items = new ListBoxModel();
        List<String> names = new ArrayList<>(profiles.keySet());
        Collections.sort(names, profileNameComparator);
        items.add(NONE_PROFILE_NAME, NONE_PROFILE_NAME); // adding None as first element
        for (String name : names) {
            if (!name.equals(NONE_PROFILE_NAME)) {
                items.add(name, name);
            }
        }

        return items;
    }

    public NvNetworkProfile getNetworkProfile(String profileName) {
        return profiles.get(profileName);
    }

    public List<NvNetworkProfile> getNetworkProfiles(Collection<String> profileNames) {
        List<NvNetworkProfile> result = new ArrayList<>();
        if (null != profileNames) {
            for (String profileName : profileNames) {
                result.add(profiles.get(profileName));
            }

        }
        return result;
    }

    public boolean exists(String profileName) {
        return null != profiles.get(profileName);
    }

    private static class ProfileNameComparator implements Comparator<String>, Serializable {
        private static final long serialVersionUID = 1;
        @Override
        public int compare(String o1, String o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            return o1.compareToIgnoreCase(o2);
        }
    }
}
