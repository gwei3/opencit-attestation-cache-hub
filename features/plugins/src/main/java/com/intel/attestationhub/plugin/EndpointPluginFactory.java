/**
 * 
 */
package com.intel.attestationhub.plugin;

import com.intel.attestationhub.api.SupportedPlugins;
import com.intel.attestationhub.plugin.impl.KubernetesPluginImpl;
import com.intel.attestationhub.plugin.impl.MesosPluginImpl;
import com.intel.attestationhub.plugin.impl.NovaPluginImpl;

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
	default:
	    break;
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
