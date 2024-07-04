/*
 * Copyright (C) 2019-2024 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.cire.herenavigation.provider;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.here.sdk.consent.Consent;
import com.here.sdk.consent.ConsentEngine;
import com.here.sdk.core.Location;
import com.here.sdk.core.LocationListener;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.location.LocationAccuracy;
import com.here.sdk.location.LocationEngine;
import com.here.sdk.location.LocationEngineStatus;
import com.here.sdk.location.LocationFeature;
import com.here.sdk.location.LocationStatusListener;
import com.here.sdk.navigation.LocationSimulator;
import com.here.sdk.navigation.LocationSimulatorOptions;
import com.here.sdk.routing.Route;
import com.here.time.Duration;

import java.util.List;

// A reference implementation using HERE Positioning to get notified on location updates
// from various location sources available from a device and HERE services.
public class HEREPositioningProvider {

    private static final String LOG_TAG = HEREPositioningProvider.class.getName();

    private LocationSimulator locationSimulator;

    private final LocationEngine locationEngine;
    private LocationListener updateListener;

    private final LocationStatusListener locationStatusListener = new LocationStatusListener() {
        @Override
        public void onStatusChanged(@NonNull LocationEngineStatus locationEngineStatus) {
            Log.d(LOG_TAG, "Location engine status: " + locationEngineStatus.name());
        }

        @Override
        public void onFeaturesNotAvailable(@NonNull List<LocationFeature> features) {
            for (LocationFeature feature : features) {
                Log.d(LOG_TAG, "Location feature not available: " + feature.name());
            }
        }
    };

    public HEREPositioningProvider() {
        //ConsentEngine consentEngine;

        try {
           // consentEngine = new ConsentEngine();
            locationEngine = new LocationEngine();
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization failed: " + e.getMessage());
        }

        // Ask user to optionally opt in to HERE's data collection / improvement program.
//        if (consentEngine.getUserConsentState() == Consent.UserReply.NOT_HANDLED) {
//            consentEngine.requestUserConsent();
//        }
    }

    @Nullable
    public Location getLastKnownLocation() {
        return locationEngine.getLastKnownLocation();
    }

    // Does nothing when engine is already running.
    public void startLocating(LocationListener updateListener, LocationAccuracy accuracy) {
        if (locationEngine.isStarted()) {
            return;
        }

        this.updateListener = updateListener;

        // Set listeners to get location updates.
        locationEngine.addLocationListener(updateListener);
        locationEngine.addLocationStatusListener(locationStatusListener);

        locationEngine.start(accuracy);
    }

    public void startMockLocating(LocationListener updateListener, Route route) {
        if (locationSimulator != null) {
            locationSimulator.stop();
        }

        locationSimulator = createLocationSimulator(updateListener, route);
        locationSimulator.start();
    }

    private LocationSimulator createLocationSimulator(LocationListener locationListener, Route route) {
        LocationSimulatorOptions locationSimulatorOptions = new LocationSimulatorOptions();
        locationSimulatorOptions.speedFactor = 4;
        locationSimulatorOptions.notificationInterval = Duration.ofMillis(500);

        LocationSimulator locationSimulator;

        try {
            locationSimulator = new LocationSimulator(route, locationSimulatorOptions);
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization of LocationSimulator failed: " + e.error.name());
        }

        locationSimulator.setListener(locationListener);

        return locationSimulator;
    }

    // Does nothing when engine is already stopped.
    public void stopLocating() {
        if (!locationEngine.isStarted()) {
            return;
        }

        // Remove listeners and stop location engine.
        locationEngine.removeLocationListener(updateListener);
        locationEngine.removeLocationStatusListener(locationStatusListener);
        locationEngine.stop();
    }
}
