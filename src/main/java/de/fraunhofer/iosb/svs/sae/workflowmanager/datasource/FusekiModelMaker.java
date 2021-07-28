package de.fraunhofer.iosb.svs.sae.workflowmanager.datasource;

import org.apache.jena.graph.GraphMaker;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelMaker;
import org.apache.jena.rdf.model.ModelReader;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.shared.DoesNotExistException;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FusekiModelMaker implements ModelMaker {

    private static final Logger log = LoggerFactory.getLogger(FusekiModelMaker.class);

    private final String connectionUrl;

    public FusekiModelMaker(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    @Override
    public Model createModel(String name, boolean strict) {
        throw new UnsupportedOperationException("No Model creation without content");
    }

    @Override
    public Model createModel(String name) {
        return createModel(name, false);
    }

    public void createModel(String name, Model model) {
        log.debug("Add Model to Fuseki: {}", name);
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) RDFConnectionFuseki.create().destination(connectionUrl + "/data").build()) {
            conn.put(name, model);
        }
    }

    @Override
    public Model openModel(String name, boolean strict) {
        log.debug("Fetching Model from Fuseki: {}", name);
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) getConnectionBuilder().build()) {
            Model model = conn.fetch(name);
            if (model != null) {
                return model;
            } else {
                if (strict) {
                    throw new DoesNotExistException(name);
                } else {
                    // create Graph instead
                    throw new UnsupportedOperationException("Cannot create graph");
                }
            }
        }
    }

    @Override
    public void removeModel(String name) {
        throw new UnsupportedOperationException("No Model deletion");
    }

    @Override
    public boolean hasModel(String name) {
        log.debug("Check if Fuseki has model: {}", name);
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) getConnectionBuilder().build()) {
            //AskBuilder askBuilder = new AskBuilder().addWhere(new WhereBuilder().addGraph(name, "?s", "?p", "?o"));
            String query = "ASK WHERE { GRAPH <" + name + "> { ?s  ?p  ?o}}";
            return conn.queryAsk(query);
        }
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("No Closing");
    }

    @Override
    public GraphMaker getGraphMaker() {
        throw new UnsupportedOperationException("No GraphMaker available");
    }

    @Override
    public ExtendedIterator<String> listModels() {
        log.debug("Get a list with all models");
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) getConnectionBuilder().build()) {
            //AskBuilder askBuilder = new AskBuilder().addWhere(new WhereBuilder().addGraph(name, "?s", "?p", "?o"));
            String query = "Select ?graph Where {Graph ?graph {}}";
            List<String> graphNames = new ArrayList<>();
            conn.querySelect(query, row -> {
                graphNames.add(row.getResource("graph").toString());
            });
            return WrappedIterator.create(graphNames.iterator());
        }

    }

    @Override
    public Model createDefaultModel() {
        throw new UnsupportedOperationException("No Model creation");
    }

    @Override
    public Model createFreshModel() {
        throw new UnsupportedOperationException("No Model creation");
    }

    @Override
    public Model openModel(String name) {
        return openModel(name, false);
    }

    @Override
    public Model openModelIfPresent(String name) {
        return hasModel(name) ? openModel(name) : null;
    }

    @Override
    public Model getModel(String URL) {
        return hasModel(URL) ? openModel(URL) : null;
    }

    @Override
    public Model getModel(String URL, ModelReader loadIfAbsent) {
        Model model = getModel(URL);
        if (model == null) {
            return loadIfAbsent.readModel(createModel(URL), URL);
        } else {
            return model;
        }
    }

    private RDFConnectionRemoteBuilder getConnectionBuilder() {
        return RDFConnectionFuseki.create().destination(connectionUrl);
    }
}
