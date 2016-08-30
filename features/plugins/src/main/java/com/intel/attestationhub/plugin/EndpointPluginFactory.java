/**
 * 
 */
package com.intel.attestationhub.plugin;

import com.intel.attestationhub.api.SupportedPlugins;
import com.intel.attestationhub.plugin.kubernetes.KubernetesPluginImpl;
import com.intel.attestationhub.plugin.mesos.MesosPluginImpl;
import com.intel.attestationhub.plugin.nova.NovaPluginImpl;

/**
 * @author Vijay Prakash
 *
 */
public class EndpointPluginFactory {

    public static EndpointPlugin getPluginImpl(String name) {

	SupportedPlugins currentPluginConst = getPluginConst(name);

	switch (currentPluginConst) {
	case KUBERNETES_PLUGIN:
	    return new KubernetesPluginImpl();
	case MESOS_PLUGIN:
	    return new MesosPluginImpl();
	case NOVA_PLUGIN:
	    return new NovaPluginImpl();
	}
	return null;
    }

    private static SupportedPlugins getPluginConst(String name) {
	for (SupportedPlugins supportedPlugin : SupportedPlugins.values()) {
	    if (supportedPlugin.getPluginName().equalsIgnoreCase(name)) {
		return supportedPlugin;
	    }
	}
	return null;
    }
}
