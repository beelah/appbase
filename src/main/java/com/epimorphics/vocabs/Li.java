/* CVS $Id: $ */
package com.epimorphics.vocabs; 
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
 
/**
 * Vocabulary definitions from src/main/vocabs/li.ttl 
 * @author Auto-generated by schemagen on 01 Dec 2012 18:43 
 */
public class Li {
    /** <p>The ontology model that holds the vocabulary terms</p> */
    private static OntModel m_model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM, null );
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://www.epimorphics.com/public/vocabulary/lucene-index#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    /** <p>Indicates a property whose value should be added to a faceted browse taxonomy 
     *  index.</p>
     */
    public static final ObjectProperty categoryProp = m_model.createObjectProperty( "http://www.epimorphics.com/public/vocabulary/lucene-index#categoryProp" );
    
    /** <p>Indicates a skos:ConceptScheme which provides a hiearchical structure for 
     *  category prop indexing.</p>
     */
    public static final ObjectProperty conceptScheme = m_model.createObjectProperty( "http://www.epimorphics.com/public/vocabulary/lucene-index#conceptScheme" );
    
    /** <p>Indicates a property whose values should be ignored for indexing.</p> */
    public static final ObjectProperty ignoreProp = m_model.createObjectProperty( "http://www.epimorphics.com/public/vocabulary/lucene-index#ignoreProp" );
    
    /** <p>If true then all properties on the entity which aren't explicitly ingored 
     *  or already indexed should be indexed as simple values.</p>
     */
    public static final ObjectProperty indexAll = m_model.createObjectProperty( "http://www.epimorphics.com/public/vocabulary/lucene-index#indexAll" );
    
    /** <p>Indicates a property whose values should be parsed as free text and indexed, 
     *  but not stored.</p>
     */
    public static final ObjectProperty labelOnlyProp = m_model.createObjectProperty( "http://www.epimorphics.com/public/vocabulary/lucene-index#labelOnlyProp" );
    
    /** <p>Indicates a property whose values should be parsed as free text and indexed 
     *  and stored.</p>
     */
    public static final ObjectProperty labelProp = m_model.createObjectProperty( "http://www.epimorphics.com/public/vocabulary/lucene-index#labelProp" );
    
    /** <p>Indicates a property whose values should be indexed as a simple value node 
     *  and stored.</p>
     */
    public static final ObjectProperty valueProp = m_model.createObjectProperty( "http://www.epimorphics.com/public/vocabulary/lucene-index#valueProp" );
    
    /** <p>A configuration specification for a lucene based entity index</p> */
    public static final OntClass Config = m_model.createClass( "http://www.epimorphics.com/public/vocabulary/lucene-index#Config" );
    
}
