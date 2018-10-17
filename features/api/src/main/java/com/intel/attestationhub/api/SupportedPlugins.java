/**
 * 
 */
package com.intel.attestationhub.api;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vijay Prakash
 *
 */
public enum SupportedPlugins {
    NOVA_PLUGIN("nova", "Nova Plugin"), KUBERNETES_PLUGIN("kubernetes", "Kubernetes Plugin"), MESOS_PLUGIN("mesos",
	    "Messos Plugin");
    private String pluginName;
    private String description;

    private SupportedPlugins(String pluginName, String description) {
	this.pluginName = pluginName;
	this.description = description;
    }

    public String getPluginName() {
	return pluginName;
    }

    public String getDescription() {
	return description;
    }

    public static List<String> getPluginNames() {
	List<String> list = new ArrayList<>();
	SupportedPlugins[] values = SupportedPlugins.values();
	for (SupportedPlugins supportedPlugins : values) {
	    list.add(supportedPlugins.getPluginName().toUpperCase());
	}
	return list;
    }
}
