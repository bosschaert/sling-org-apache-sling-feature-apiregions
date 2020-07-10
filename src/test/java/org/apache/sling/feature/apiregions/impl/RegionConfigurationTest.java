/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.apiregions.impl;

import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.apache.sling.feature.apiregions.impl.RegionConstants.APIREGIONS_JOINGLOBAL;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.BUNDLE_FEATURE_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.DEFAULT_REGIONS;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.FEATURE_REGION_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.IDBSNVER_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.PROPERTIES_FILE_LOCATION;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.PROPERTIES_RESOURCE_PREFIX;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.REGION_PACKAGE_FILENAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RegionConfigurationTest {
    @Test
    public void testRegionConfigurationNoConfiguration() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);

        try {
            new RegionConfiguration(ctx);
            fail("Expected exception. Configuration is enabled but is missing configuration");
        } catch (Exception e) {
            // good
        }
    }

    @Test
    public void testLoadBSNVerMap() throws Exception {
        String e = getClass().getResource("/empty.properties").toURI().toString();
        String f = getClass().getResource("/idbsnver1.properties").toURI().toString();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(f);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(e);

        RegionConfiguration re = new RegionConfiguration(ctx);
        assertEquals(2, re.bsnVerMap.size());
        assertEquals(Collections.singletonList("g:b1:1"),
                re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b1", new Version(1,0,0))));
        assertEquals(new HashSet<>(Arrays.asList("g:b2:1.2.3", "g2:b2:1.2.4")),
                new HashSet<>(re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b2", new Version(1,2,3)))));
        assertEquals(f, re.getRegistrationProperties().get(IDBSNVER_FILENAME));
    }

    @Test
    public void testLoadBSNVerMapAndConfig() throws Exception {
        String e = getClass().getResource("/empty.properties").toURI().toString();
        String f = getClass().getResource("/idbsnver1.properties").toURI().toString();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(f);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(e);

        RegionConfiguration re = new RegionConfiguration(ctx);
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(RegionConstants.PROP_idbsnver, "g3:b3:2.7=b3~2.7");
        re.setConfig("new.config", props);

        assertEquals(3, re.bsnVerMap.size());
        assertEquals(Collections.singletonList("g:b1:1"),
                re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b1", new Version(1,0,0))));
        assertEquals(new HashSet<>(Arrays.asList("g:b2:1.2.3", "g2:b2:1.2.4")),
                new HashSet<>(re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b2", new Version(1,2,3)))));
        assertEquals(Collections.singletonList("g3:b3:2.7"),
                re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b3", new Version(2,7,0))));

        re.removeConfig("new.config");
        assertEquals(2, re.bsnVerMap.size());
        assertEquals(Collections.singletonList("g:b1:1"),
                re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b1", new Version(1,0,0))));
        assertEquals(new HashSet<>(Arrays.asList("g:b2:1.2.3", "g2:b2:1.2.4")),
                new HashSet<>(re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b2", new Version(1,2,3)))));
    }

    @Test
    public void testLoadBundleFeatureMap() throws Exception {
        String e = getClass().getResource("/empty.properties").toURI().toString();
        String f = getClass().getResource("/bundles1.properties").toURI().toString();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(f);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(e);

        RegionConfiguration re = new RegionConfiguration(ctx);
        assertEquals(3, re.bundleFeatureMap.size());
        assertEquals(Collections.singleton("org.sling:something:1.2.3:slingosgifeature:myclassifier"),
                re.bundleFeatureMap.get("org.sling:b1:1"));
        assertEquals(Collections.singleton("org.sling:something:1.2.3:slingosgifeature:myclassifier"),
                re.bundleFeatureMap.get("org.sling:b2:1"));
        assertEquals(new HashSet<>(Arrays.asList("some.other:feature:123", "org.sling:something:1.2.3:slingosgifeature:myclassifier")),
                re.bundleFeatureMap.get("org.sling:b3:1"));
        assertEquals(f,  re.getRegistrationProperties().get(BUNDLE_FEATURE_FILENAME));
    }

    @Test
    public void testLoadBundleFeatureMapAndConfig() throws Exception {
        String e = getClass().getResource("/empty.properties").toURI().toString();
        String f = getClass().getResource("/bundles1.properties").toURI().toString();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(f);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(e);

        RegionConfiguration re = new RegionConfiguration(ctx);
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(RegionConstants.PROP_bundleFeatures, "g3:b3:2.7=fg1:fa1:3.0");
        re.setConfig("new.config", props);

        assertEquals(4, re.bundleFeatureMap.size());
        assertEquals(Collections.singleton("fg1:fa1:3.0"),
                re.bundleFeatureMap.get("g3:b3:2.7"));
        assertEquals(Collections.singleton("org.sling:something:1.2.3:slingosgifeature:myclassifier"),
                re.bundleFeatureMap.get("org.sling:b1:1"));
        assertEquals(Collections.singleton("org.sling:something:1.2.3:slingosgifeature:myclassifier"),
                re.bundleFeatureMap.get("org.sling:b2:1"));
        assertEquals(new HashSet<>(Arrays.asList("some.other:feature:123", "org.sling:something:1.2.3:slingosgifeature:myclassifier")),
                re.bundleFeatureMap.get("org.sling:b3:1"));

        re.removeConfig("new.config");
        assertEquals(3, re.bundleFeatureMap.size());
        assertEquals(Collections.singleton("org.sling:something:1.2.3:slingosgifeature:myclassifier"),
                re.bundleFeatureMap.get("org.sling:b1:1"));
        assertEquals(Collections.singleton("org.sling:something:1.2.3:slingosgifeature:myclassifier"),
                re.bundleFeatureMap.get("org.sling:b2:1"));
        assertEquals(new HashSet<>(Arrays.asList("some.other:feature:123", "org.sling:something:1.2.3:slingosgifeature:myclassifier")),
                re.bundleFeatureMap.get("org.sling:b3:1"));
    }

    @Test
    public void testLoadFeatureRegionMap() throws Exception {
        String e = getClass().getResource("/empty.properties").toURI().toString();
        String f = getClass().getResource("/features1.properties").toURI().toString();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(f);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(e);

        RegionConfiguration re = new RegionConfiguration(ctx);
        assertEquals(2, re.featureRegionMap.size());
        assertEquals(Collections.singleton("global"),
                re.featureRegionMap.get("an.other:feature:123"));
        assertEquals(new HashSet<>(Arrays.asList("global", "internal")),
                re.featureRegionMap.get("org.sling:something:1.2.3"));
        assertEquals(f,  re.getRegistrationProperties().get(FEATURE_REGION_FILENAME));
    }

    @Test
    public void testLoadFeatureRegionMapAndConfig() throws Exception {
        String e = getClass().getResource("/empty.properties").toURI().toString();
        String f = getClass().getResource("/features1.properties").toURI().toString();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(f);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(e);

        RegionConfiguration re = new RegionConfiguration(ctx);
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(RegionConstants.PROP_featureRegions, "fg1:fa1:3.0=newregion");
        re.setConfig("new.config", props);

        assertEquals(3, re.featureRegionMap.size());
        assertEquals(Collections.singleton("newregion"),
                re.featureRegionMap.get("fg1:fa1:3.0"));
        assertEquals(Collections.singleton("global"),
                re.featureRegionMap.get("an.other:feature:123"));
        assertEquals(new HashSet<>(Arrays.asList("global", "internal")),
                re.featureRegionMap.get("org.sling:something:1.2.3"));

        re.removeConfig("new.config");
        assertEquals(2, re.featureRegionMap.size());
        assertEquals(Collections.singleton("global"),
                re.featureRegionMap.get("an.other:feature:123"));
        assertEquals(new HashSet<>(Arrays.asList("global", "internal")),
                re.featureRegionMap.get("org.sling:something:1.2.3"));
    }

    @Test
    public void testLoadRegionPackageMap() throws Exception {
        String e = getClass().getResource("/empty.properties").toURI().toString();
        String f = getClass().getResource("/regions1.properties").toURI().toString();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(f);

        RegionConfiguration re = new RegionConfiguration(ctx);
        assertEquals(2, re.regionPackageMap.size());
        assertEquals(Collections.singleton("xyz"),
                re.regionPackageMap.get("internal"));
        assertEquals(new HashSet<>(Arrays.asList("a.b.c", "d.e.f", "test")),
                re.regionPackageMap.get("global"));
        assertEquals(f,  re.getRegistrationProperties().get(REGION_PACKAGE_FILENAME));
    }

    @Test
    public void testLoadRegionPackageMapAndConfig() throws Exception {
        String e = getClass().getResource("/empty.properties").toURI().toString();
        String f = getClass().getResource("/regions1.properties").toURI().toString();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(f);

        RegionConfiguration re = new RegionConfiguration(ctx);
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(RegionConstants.PROP_regionPackage, "internal=r.i.p");
        re.setConfig("new.config", props);

        assertEquals(2, re.regionPackageMap.size());
        assertEquals(new HashSet<>(Arrays.asList("xyz", "r.i.p")),
                re.regionPackageMap.get("internal"));
        assertEquals(new HashSet<>(Arrays.asList("a.b.c", "d.e.f", "test")),
                re.regionPackageMap.get("global"));

        re.removeConfig("new.config");
        assertEquals(2, re.regionPackageMap.size());
        assertEquals(Collections.singleton("xyz"),
                re.regionPackageMap.get("internal"));
        assertEquals(new HashSet<>(Arrays.asList("a.b.c", "d.e.f", "test")),
                re.regionPackageMap.get("global"));
    }

    @Test
    public void testJoinRegionsToGlobal() throws Exception {
        String e = getClass().getResource("/empty.properties").toURI().toString();
        String f = getClass().getResource("/regions2.properties").toURI().toString();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(APIREGIONS_JOINGLOBAL)).thenReturn("obsolete,deprecated");
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(f);

        RegionConfiguration re = new RegionConfiguration(ctx);
        assertEquals(1, re.regionPackageMap.size());
        assertEquals(new HashSet<>(Arrays.asList("xyz", "a.b.c", "d.e.f", "test")),
                re.regionPackageMap.get("global"));
    }

    @Test
    public void testBegin() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).
            thenReturn(getClass().getResource("/idbsnver1.properties").toURI().toString());
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).
            thenReturn(getClass().getResource("/bundles1.properties").toURI().toString());
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).
            thenReturn(getClass().getResource("/features1.properties").toURI().toString());
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).
            thenReturn(getClass().getResource("/regions1.properties").toURI().toString());

        RegionConfiguration re = new RegionConfiguration(ctx);
        assertTrue(re.bsnVerMap.size() > 0);
        assertTrue(re.bundleFeatureMap.size() > 0);
        assertTrue(re.featureRegionMap.size() > 0);
        assertTrue(re.regionPackageMap.size() > 0);

        ResolverHookImpl hook = (ResolverHookImpl) new RegionEnforcer(re).begin(null);
        assertEquals(re.bsnVerMap, hook.configuration.bsnVerMap);
        assertEquals(re.bundleFeatureMap, hook.configuration.bundleFeatureMap);
        assertEquals(re.featureRegionMap, hook.configuration.featureRegionMap);
        assertEquals(re.regionPackageMap, hook.configuration.regionPackageMap);
    }

    @Test
    public void testURLs() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        String location = new File(getClass().getResource("/props1/idbsnver.properties").
                getFile()).getParentFile().toURI().toString();
        Mockito.when(ctx.getProperty(PROPERTIES_FILE_LOCATION)).thenReturn(location);

        RegionConfiguration re = new RegionConfiguration(ctx);
        assertTrue(re.bsnVerMap.size() > 0);
        assertTrue(re.bundleFeatureMap.size() > 0);
        assertTrue(re.featureRegionMap.size() > 0);
        assertTrue(re.regionPackageMap.size() > 0);
    }

    @Test
    public void testClassloaderURLs() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_FILE_LOCATION)).
            thenReturn("classloader://props1");

        RegionConfiguration re = new RegionConfiguration(ctx);
        assertTrue(re.bsnVerMap.size() > 0);
        assertTrue(re.bundleFeatureMap.size() > 0);
        assertTrue(re.featureRegionMap.size() > 0);
        assertTrue(re.regionPackageMap.size() > 0);
    }

    @Test
    public void testOrderingOfRegionsInFeatures() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_FILE_LOCATION)).
            thenReturn("classloader://props2");

        RegionConfiguration re = new RegionConfiguration(ctx);
        assertEquals(Arrays.asList("r0", "r1", "r2", "r3"),
                new ArrayList<>(re.featureRegionMap.get("org.sling:something:1.2.3")));
    }

    @Test
    public void testUnModifiableMaps() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_FILE_LOCATION)).
            thenReturn("classloader://props1");

        RegionConfiguration re = new RegionConfiguration(ctx);
        assertTrue(re.bsnVerMap.size() > 0);
        assertBSNVerMapUnmodifiable(re.bsnVerMap);
        assertTrue(re.bundleFeatureMap.size() > 0);
        assertMapUnmodifiable(re.bundleFeatureMap);
        assertTrue(re.featureRegionMap.size() > 0);
        assertMapUnmodifiable(re.featureRegionMap);
        assertTrue(re.regionPackageMap.size() > 0);
        assertMapUnmodifiable(re.regionPackageMap);
    }

    @Test
    public void testDefaultRegions() throws Exception {
        testDefaultRegions("foo.bar,foo.zar", new HashSet<>(Arrays.asList("foo.bar", "foo.zar")));
        testDefaultRegions("test", Collections.singleton("test"));
        testDefaultRegions("", Collections.emptySet());
        testDefaultRegions(null, Collections.emptySet());
    }

    @Test
    public void testStoreLoadPersistedConfig() throws Exception {
        File f = File.createTempFile("testStorePersistedConfig", ".tmp");

        try {
            Bundle bundle = Mockito.mock(Bundle.class);
            Mockito.when(bundle.getDataFile("bundleLocationToFeature.properties"))
                .thenReturn(f);

            BundleContext ctx = Mockito.mock(BundleContext.class);
            Mockito.when(ctx.getBundle()).thenReturn(bundle);
            Mockito.when(ctx.getProperty(PROPERTIES_FILE_LOCATION)).
                thenReturn("classloader://props1");

            RegionConfiguration cfg = new RegionConfiguration(ctx);

            ConcurrentMap<String, Map.Entry<String, Version>> m = cfg.getBundleLocationConfigMap();
            m.put("foo://bar", new AbstractMap.SimpleEntry<>("blah", new Version(1,0,0,"suffix")));
            m.put("foo://tar", new AbstractMap.SimpleEntry<>("a.b.c", new Version(9,8,7)));
            cfg.storeLocationToConfigMap(ctx);

            RegionConfiguration cfg2 = new RegionConfiguration(ctx);
            ConcurrentMap<String, Map.Entry<String, Version>> m2 = cfg2.getBundleLocationConfigMap();
            assertEquals(m, m2);
        } finally {
            f.delete();
        }
    }

    @Test
    public void testUpdateLocationCacheOnConfigUpdate() throws Exception {
        // Set up a typical configuration scenario

        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(PROPERTIES_FILE_LOCATION)).
            thenReturn("classloader://props4");
        RegionConfiguration cfg = new RegionConfiguration(ctx);

        assertEquals(2, cfg.getBsnVerMap().size());
        assertEquals(Collections.singletonList("g:b1:1"),
                cfg.getBsnVerMap().get(new AbstractMap.SimpleEntry<>("b1", new Version(1,0,0))));
        assertEquals(Collections.singletonList("g:b2:1.2.3"),
                cfg.getBsnVerMap().get(new AbstractMap.SimpleEntry<>("b2", new Version(1,2,3))));

        // Now start invoking the resolver hook, this should fill in some of the location cache
        assertEquals("Precondition", 0, cfg.getBundleLocationConfigMap().size());
        ResolverHookImpl rhi = new ResolverHookImpl(cfg);

        BundleRequirement req = mockRequirement("b4", new Version(9,9,9,"something"), ctx);
        BundleCapability cap1 = mockCapability("org.foo.bar", "b1", new Version(1,0,0), ctx);
        BundleCapability cap2 = mockCapability("org.foo.bar", "b2", new Version(1,2,3), ctx);

        ArrayList<BundleCapability> caps1 = new ArrayList<>(Arrays.asList(cap1, cap2));
        rhi.filterMatches(req, caps1);
        assertEquals("caps should be empty it b4 doesn't have regions configuration yet",
                Collections.emptyList(), caps1);

        // At this point the bundle location to feature map should have some cached info
        assertEquals(3, cfg.getBundleLocationConfigMap().size());
        assertEquals(new AbstractMap.SimpleEntry<>("b1", new Version(1,0,0)),
                cfg.getBundleLocationConfigMap().get("http://b1"));
        assertEquals(new AbstractMap.SimpleEntry<>("b2", new Version(1,2,3)),
                cfg.getBundleLocationConfigMap().get("http://b2"));
        assertEquals(new AbstractMap.SimpleEntry<>("b4", new Version(9,9,9,"something")),
                cfg.getBundleLocationConfigMap().get("http://b4"));


        // Now update the configuration, there is new mappings for bundles b3 and b4 and
        // the features of b2 have changed
        Dictionary<String,Object> d = new Hashtable<>();
        d.put("mapping.bundleid.bsnver",
                new String[] {
                        "g:b3:1.2=a.b3.c~1.2",
                        "g:b4:9.9.9.something=b4~9.9.9.something"});
        d.put("mapping.bundleid.features",
                new String[] {
                        "g:b4:9.9.9.something=org.sling:something:1.2.3"
                });

        cfg.setConfig("my.factory.pid", d);
        assertEquals(4, cfg.getBsnVerMap().size());
        assertEquals(Collections.singletonList("g:b1:1"),
                cfg.getBsnVerMap().get(new AbstractMap.SimpleEntry<>("b1", new Version(1,0,0))));
        assertEquals(Collections.singletonList("g:b2:1.2.3"),
                cfg.getBsnVerMap().get(new AbstractMap.SimpleEntry<>("b2", new Version(1,2,3))));
        assertEquals(Collections.singletonList("g:b3:1.2"),
                cfg.getBsnVerMap().get(new AbstractMap.SimpleEntry<>("a.b3.c", new Version(1,2,0))));
        assertEquals(Collections.singletonList("g:b4:9.9.9.something"),
                cfg.getBsnVerMap().get(new AbstractMap.SimpleEntry<>("b4", new Version(9,9,9,"something"))));

        // Redo a resolve action and check that the cache is now re-filled
        ArrayList<BundleCapability> caps2 = new ArrayList<>(Arrays.asList(cap1, cap2));
        rhi.filterMatches(req, caps2);

        ArrayList<BundleCapability> expected = new ArrayList<>(Arrays.asList(cap1, cap2));
        assertEquals("Now b4 from the req should have visibility to both caps as b4 is now in the feature",
                expected, caps2);
    }

    private BundleRequirement mockRequirement(String bsn, Version bver, BundleContext mockContext) {
        BundleRevision br = mockBundleRevision(bsn, bver, mockContext);

        BundleRequirement req = Mockito.mock(BundleRequirement.class);
        Mockito.when(req.getRevision()).thenReturn(br);
        Mockito.when(req.getNamespace()).thenReturn(PackageNamespace.PACKAGE_NAMESPACE);
        return req;
    }

    private BundleCapability mockCapability(String pkg, String bsn, Version bver, BundleContext mockContext) {
        Map<String, Object> attrs =
                Collections.<String, Object>singletonMap(PackageNamespace.PACKAGE_NAMESPACE, pkg);

        BundleRevision br = mockBundleRevision(bsn, bver, mockContext);

        BundleCapability cap = Mockito.mock(BundleCapability.class);
        Mockito.when(cap.getRevision()).thenReturn(br);
        Mockito.when(cap.getAttributes()).thenReturn(attrs);
        Mockito.when(cap.getNamespace()).thenReturn(PackageNamespace.PACKAGE_NAMESPACE);
        return cap;
    }

    private BundleRevision mockBundleRevision(String bsn, Version bver, BundleContext mockContext) {
        Bundle b = Mockito.mock(Bundle.class);
        Mockito.when(b.getSymbolicName()).thenReturn(bsn);
        Mockito.when(b.getVersion()).thenReturn(bver);
        String bundleLocation = "http://" + bsn;
        Mockito.when(b.getLocation()).thenReturn(bundleLocation);
        Mockito.when(b.getBundleId()).thenReturn(System.currentTimeMillis()); // Just some random unique ID

        BundleRevision br = Mockito.mock(BundleRevision.class);
        Mockito.when(br.getBundle()).thenReturn(b);

        // Make the bundlecontext aware...
        Mockito.when(mockContext.getBundle(bundleLocation)).thenReturn(b);

        return br;
    }

    private void testDefaultRegions(String defProp, Set<String> expected)
            throws IOException, URISyntaxException, NoSuchFieldException, IllegalAccessException {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(ctx.getProperty(DEFAULT_REGIONS)).thenReturn(defProp);
        Mockito.when(ctx.getProperty(PROPERTIES_FILE_LOCATION)).
        thenReturn("classloader://props1");

        RegionConfiguration re = new RegionConfiguration(ctx);
        ResolverHook hook = new RegionEnforcer(re).begin(Collections.emptySet());
        Field f = ResolverHookImpl.class.getDeclaredField("configuration");
        f.setAccessible(true);

        assertEquals(expected, ((RegionConfiguration)f.get(hook)).getDefaultRegions());
    }

    private void assertBSNVerMapUnmodifiable(Map<Map.Entry<String, Version>, List<String>> m) {
        Map.Entry<Map.Entry<String, Version>, List<String>> entry = m.entrySet().iterator().next();
        try {
            List<String> c = entry.getValue();
            c.add("test");
            fail("Changing a value should have thrown an exception");
        } catch (Exception ex) {
            // good
        }

        try {
            m.put(new AbstractMap.SimpleEntry<>("hi", Version.parseVersion("1.2.3")),
                    Collections.singletonList("xyz"));
            fail("Adding a new value should have thrown an exception");
        } catch (Exception ex) {
            // good
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void assertMapUnmodifiable(Map<String, ? extends Set<String>> m) {
        Map.Entry<String, ? extends Set<String>> entry = m.entrySet().iterator().next();
        try {
            Set<String> s = entry.getValue();
            s.add("testing");
            fail("Changing a value should have thrown an exception");
        } catch (Exception ex) {
            // good
        }

        try {
            Map m2 = m;
            m2.put("foo", Collections.<String>emptySet());
            fail("Adding a new value should have thrown an exception");
        } catch (Exception ex) {
            // good
        }
    }

}
