/******************************************************************
 * File:        CreateErrorAction.java
 * Created by:  Dave Reynolds
 * Created on:  20 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.task;

import java.util.Map;

import com.epimorphics.appbase.tasks.impl.BaseAction;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.util.EpiException;

/**
 * Test action which throws an exception to test error handling
 */
public class CreateErrorAction extends BaseAction {

    @Override
    protected void doRun(Map<String, Object> parameters,
            ProgressMonitorReporter monitor) {
        throw new EpiException("Forcing error from CreateErrorAction");
    }

}
