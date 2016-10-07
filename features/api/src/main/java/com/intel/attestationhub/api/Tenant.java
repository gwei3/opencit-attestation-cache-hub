package com.intel.attestationhub.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL) // or Include.NON_EMPTY, if that fits your use
			       // case
public class Tenant {
    private String id;
    private String name;
    private boolean deleted;
    private List<Plugin> plugins;

    public Tenant() {
	super();
	plugins = new ArrayList<Plugin>();
    }

    public String getId() {
	return id;
    }

    public void setId(String id) {
	this.id = id;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public List<Plugin> getPlugins() {
	return plugins;
    }

    public void setPlugins(List<Plugin> plugins) {
	this.plugins = plugins;
    }

    public Plugin addPlugin(String name) {
	Plugin plugin = new Plugin();
	plugin.setName(name);
	plugins.add(plugin);
	return plugin;
    }

    public static class Plugin {
	public static final String PLUGIN_PROVIDER = "plugin.provider";

	public String name;
	public List<Property> properties;

	public Plugin() {
	    super();
	    properties = new ArrayList<Property>();
	}

	public String getName() {
	    return name;
	}

	public void setName(String name) {
	    this.name = name;
	}

	public List<Property> getProperties() {
	    return properties;
	}

	public void setProperties(List<Property> properties) {
	    this.properties = properties;
	}

	public void addProperty(String key, String value) {
	    properties.add(new Property(key, value));
	}

	public String extractProviderClass() {
	    String providerClass = null;
	    List<Property> properties = getProperties();
	    for (Property property : properties) {
		if (PLUGIN_PROVIDER.equals(property.getKey())) {
		    providerClass = property.getValue();
		    break;
		}
	    }
	    return providerClass;
	}
    }

    public static class Property {
	public String key;
	public String value;

	public Property() {
	}

	public Property(String key, String value) {
	    super();
	    this.key = key;
	    this.value = value;
	}

	public String getKey() {
	    return key;
	}

	public void setKey(String key) {
	    this.key = key;
	}

	public String getValue() {
	    return value;
	}

	public void setValue(String value) {
	    this.value = value;
	}

    }

    public boolean isDeleted() {
	return deleted;
    }

    public void setDeleted(boolean deleted) {
	this.deleted = deleted;
    }

    public String validate() {
	List<String> errors = new ArrayList<>();
	if (StringUtils.isBlank(name)) {
	    errors.add("Tenant Name cannot be empty");
	}

	if (plugins == null || plugins.size() == 0) {
	    errors.add("Plugin information is mandatory");
	}
	return StringUtils.join(errors, ",");
    }
}
