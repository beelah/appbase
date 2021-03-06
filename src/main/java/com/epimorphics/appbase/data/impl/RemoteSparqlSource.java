/******************************************************************
 * File:        RemoteSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  29 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.riot.WebContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.data.SparqlSource;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateRequest;

/**
 * Sparql source for querying remote sparql endpoints. Configuration options:
 * <ul>
 *   <li>endpoint - URL for the SPARQL query endpoint</li>
 *   <li>updateEndpoint - optional URL for the SPARQL update endpoint</li>
 *   <li>graphEndpoint - optional URL for the graph store protocol endpoint</li>
 *   <li>contentType - set the type of the data requested for query results, one of "xml", "json", "tsv", "csv"</li>
 * </ul>
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RemoteSparqlSource extends BaseSparqlSource implements SparqlSource {
    static Logger log = LoggerFactory.getLogger(RemoteSparqlSource.class);
    
    static protected Map<String, String> typeMap = new HashMap<String, String>();
    static {
        typeMap.put("xml", WebContent.contentTypeResultsXML);
        typeMap.put("json", WebContent.contentTypeResultsJSON);
        typeMap.put("tsv", WebContent.contentTypeTextTSV);
        typeMap.put("csv", WebContent.contentTypeTextCSV);
    }
            
    protected String endpoint;
    protected String contentType = WebContent.contentTypeResultsXML;
    protected String updateEndpoint;
    protected String graphEndpoint;
    protected DatasetAccessor accessor;
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public void setUpdateEndpoint(String endpoint) {
        this.updateEndpoint = endpoint;
    }
    
    public void setGraphEndpoint(String endpoint) {
        this.graphEndpoint = endpoint;
    }
    
    /**
     * Set the content type to request from the remote endpoint.
     * Legal values are "xml", "json", "tsv", "csv".
     */
    public void setContentType(String type) {
        contentType = typeMap.get(type);
        if (contentType == null){
            log.error("Illegal contentType for remote source (" + type + ")");
        }
    }

    @Override
    protected QueryExecution start(String queryString) {
        QueryExecution s = QueryExecutionFactory.sparqlService(endpoint, queryString);
        if (contentType != null) {
            QueryEngineHTTP hs = (QueryEngineHTTP) s;
            hs.setSelectContentType(contentType);
        }
        return s;
    }

    @Override
    protected void finish(QueryExecution qexec) {
        qexec.close();
    }

    @Override
    public void update(UpdateRequest update) {
        UpdateExecutionFactory.createRemote(update, updateEndpoint).execute();
    }

    @Override
    public boolean isUpdateable() {
        return updateEndpoint != null;
    }

    @Override
    public DatasetAccessor getAccessor() {
        if (accessor == null) {
            accessor = DatasetAccessorFactory.createHTTP(graphEndpoint);
        }
        return accessor;
    }

    
}
