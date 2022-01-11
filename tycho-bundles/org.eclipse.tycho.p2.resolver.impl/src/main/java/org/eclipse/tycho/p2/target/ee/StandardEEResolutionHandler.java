/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - #519 - Provide better feedback to the user about the cause of a failed resolution
 *******************************************************************************/
package org.eclipse.tycho.p2.target.ee;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;

@SuppressWarnings("restriction")
class StandardEEResolutionHandler extends ExecutionEnvironmentResolutionHandler {

    private ExecutionEnvironmentConfiguration environmentConfiguration;
    private MavenLogger logger;

    public StandardEEResolutionHandler(ExecutionEnvironmentResolutionHints resolutionHints,
            ExecutionEnvironmentConfiguration environmentConfiguration, MavenLogger logger) {
        super(resolutionHints);
        this.environmentConfiguration = environmentConfiguration;
        this.logger = logger;
    }

    @Override
    public void readFullSpecification(Collection<IInstallableUnit> targetPlatformContent) {
        if (environmentConfiguration.ignoreExecutionEnvironment()) {
            //if it is ignored not setting the specification leads to strange errors in downstream mojos...
            environmentConfiguration.setFullSpecificationForCustomProfile(Collections.emptyList());
            //and we might want to inform the user about ignored items...
            logger.info("The following Execution Environments are currently known but are ignored by configuration:");
            Map<String, Collection<String>> map = targetPlatformContent.stream()//
                    .filter(ExecutionEnvironmentResolutionHints::isJreUnit)//
                    .map(StandardEEResolutionHandler::getEE).collect(Collectors.groupingBy(entry -> entry.getValue(),
                            TreeMap::new, Collectors.mapping(Entry::getKey, Collectors.toCollection(TreeSet::new))));
            map.entrySet().forEach(entry -> {
                logger.info(
                        "    " + entry.getKey() + " -> " + entry.getValue().stream().collect(Collectors.joining(", ")));
            });
            return;
        }
        // standard EEs are fully specified - no need to read anything from the target platform
    }

    private static Entry<String, String> getEE(IInstallableUnit specificationUnit) {
        for (IProvidedCapability capability : specificationUnit.getProvidedCapabilities()) {
            String namespace = capability.getNamespace();
            String name = capability.getName();
            String version = capability.getVersion().toString();

            if (JREAction.NAMESPACE_OSGI_EE.equals(namespace)) {
                return new SimpleEntry<>(name, version);
            }
        }
        return new SimpleEntry<>(specificationUnit.getId(), specificationUnit.getVersion().toString());
    }

}
