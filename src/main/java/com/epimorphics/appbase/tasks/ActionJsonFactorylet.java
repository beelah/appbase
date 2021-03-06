/******************************************************************
 * File:        ActionJsonFactorylet.java
 * Created by:  Dave Reynolds
 * Created on:  19 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;

import com.epimorphics.appbase.tasks.impl.BaseAction;
import com.epimorphics.appbase.tasks.impl.CompoundAction;
import com.epimorphics.appbase.tasks.impl.JavaAction;
import com.epimorphics.appbase.tasks.impl.ParallelAction;
import com.epimorphics.appbase.tasks.impl.RegexTrigger;
import com.epimorphics.appbase.tasks.impl.SequenceAction;
import com.epimorphics.appbase.tasks.impl.WrappedAction;
import com.epimorphics.util.EpiException;

/**
 * Support for configuring actions, or simple action sequences via
 * JSON configuration files.
 * <p>
 * The configuration file can comprise a single JSON object specifying a single action,
 * or an array of such objects each specifying independent actions. </p>
 * <p> 
 * Each action specification can use the follow keywords:</p>
 * <ul>
 *   <li><code>@type</code> - the type of action, "simple", "sequence", "parallel", can omit if type is simple</li>
 *   <li><code>@name</code> - a name for the action</li>
 *   <li><code>@description</code> - optional description</li>
 *   <li><code>@timeout</code> - optional timeout value in milliseconds</li>
 *   <li><code>@base</code> - the name of a base action which this simple action parameterizes</li>
 *   <li><code>@javaclass</code> - name of a javaclass that implements the base action, alternative to @base for simple actions</li>
 *   <li><code>@actions</code> - array of actions which make up a sequence or parallel action</li>
 *   <li><code>@onError</code> - action to be invoke if this or any component actions fail</li>
 *   <li><code>@trigger</code> - regex which, if it matches an event, triggers the action</li>
 * </ul>
 * <p>All other keys are take to be configuration parameters for the action.</p>
 * 
 * <p>This is not intended to be a full workflow graph!</p>
 */
public class ActionJsonFactorylet implements ActionFactory.Factorylet {
    public static final String TYPE_KEY        = "@type";
    public static final String NAME_KEY        = "@name";
    public static final String DESCRIPTION_KEY = "@description";
    public static final String TIMEOUT_KEY     = "@timeout";
    public static final String BASE_KEY        = "@base";
    public static final String JAVACLASS_KEY   = "@javaclass";
    public static final String ACTIONS_KEY     = "@actions";
    public static final String ON_ERROR_KEY    = "@onError";
    public static final String TRIGGER_KEY     = "@trigger";

    public static final String SIMPLE_TYPE    = "simple";
    public static final String SEQUENCE_TYPE    = "sequence";
    public static final String PAR_TYPE    = "parallel";

    private static final Set<String> ALLOWED_KEYS = new HashSet<>();
    static {
        ALLOWED_KEYS.add(TYPE_KEY);
        ALLOWED_KEYS.add(NAME_KEY);
        ALLOWED_KEYS.add(DESCRIPTION_KEY);
        ALLOWED_KEYS.add(TIMEOUT_KEY);
        ALLOWED_KEYS.add(BASE_KEY);
        ALLOWED_KEYS.add(JAVACLASS_KEY);
        ALLOWED_KEYS.add(ACTIONS_KEY);
        ALLOWED_KEYS.add(ON_ERROR_KEY);
        ALLOWED_KEYS.add(TRIGGER_KEY);
    }
    
    @Override
    public boolean canConfigure(File file) {
        return file.canRead() && file.getName().endsWith(".json");
    }

    @Override
    public Collection<Action> configure(File file) {
        try {
            FileInputStream stream = new FileInputStream(file);
            JsonValue spec = JSON.parseAny(stream);
            stream.close();
            
            if (spec.isArray()) {
                JsonArray a = spec.getAsArray();
                List<Action> actions = new ArrayList<>( a.size() );
                for (Iterator<JsonValue> i = a.iterator(); i.hasNext();) {
                    JsonValue s = i.next();
                    if (s.isObject()) {
                        actions.add( parseAction(s.getAsObject()) );
                    } else {
                        throw new EpiException("Error parsing action specification " + file + ", should be an object or array of objects");
                    }
                }
                return actions;
                
            } else if (spec.isObject()) {
                return Collections.singletonList( parseAction( spec.getAsObject() ) );
            } else {
                throw new EpiException("Error parsing action specification " + file + ", must be object or array at top level");
            }
        
        } catch (IOException e) {
            throw new EpiException("Failed to load action configuration from " + file, e);
        }
    }
    
    private Action parseAction(JsonObject spec) {
        String type = getString(spec, TYPE_KEY, SIMPLE_TYPE);
        BaseAction action = null;
        if (type.equals(SIMPLE_TYPE)) {
            if (spec.hasKey(JAVACLASS_KEY)) {
                action = new JavaAction();
                try {
                    ((JavaAction)action).setAction( getString(spec, JAVACLASS_KEY, null) );
                } catch (Exception e) {
                    throw new EpiException("Problem configuration javaclass action", e);
                }
            } else {
                action = new WrappedAction();
            }
        } else {
            CompoundAction flow = type.equals(SEQUENCE_TYPE) ? new SequenceAction() : new ParallelAction();
            JsonValue av = spec.get(ACTIONS_KEY);
            if (av.isArray()) {
                Iterator<JsonValue> i = av.getAsArray().iterator();
                while (i.hasNext()) {
                    flow.addComponent( parseActionRef(i.next()) );
                }
            } else {
                flow.addComponent( parseActionRef(av) );
            }
            action = flow;
        }
        for (String key : spec.keySet()) {
            if (key.startsWith("@")) {
                if (!ALLOWED_KEYS.contains(key)) {
                    throw new EpiException("Error parsing action specifcation, " + key + " is not a legal reserved key");
                }
                if (key.equals(ON_ERROR_KEY)) {
                    action.setConfig(key, parseActionRef( spec.get(key) ));
                } else if (key.equals(ACTIONS_KEY)) {
                    // already handled
                } else if (key.equals(TRIGGER_KEY)) {
                    action.setTrigger( new RegexTrigger( getString(spec, key, ".*") ) );
                } else {
                    action.setConfig(key, getValue(spec, key));
                }
            } else {
                action.setConfig(key, getValue(spec, key));
            }
        }
        return action;
    }
    
    private Object parseActionRef(JsonValue value) {
        if (value.isString()) {
            return value.getAsString().value();
        } else if (value.isObject()) {
            return parseAction( value.getAsObject() );
        } else {
            throw new EpiException("Illegal action reference (should be string or nested object): " + value);
        }
    }

    private String getString(JsonObject o, String key, String deflt) {
        JsonValue v = o.get(key);
        if (v == null) {
            return deflt;
        } else {
            if (v.isString()) {
                return v.getAsString().value();
            }
        }
        throw new EpiException("Bad action specification, found '" + v + "' when expecting a string");
    }

    private Object getValue(JsonObject o, String key) {
        JsonValue v = o.get(key);
        if (v == null) {
            return null;
        } else {
            if (v.isNumber()) {
                return v.getAsNumber().value();
            } else if (v.isString()) {
                return v.getAsString().value();
            } else if (v.isBoolean()) {
                return v.getAsBoolean().value();
            }
        }
        throw new EpiException("Bad action specification, found '" + v + "' when expecting a primitive");
    }

}
