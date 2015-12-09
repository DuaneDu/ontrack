package net.nemerosa.ontrack.service.security;

import net.nemerosa.ontrack.extension.api.ExtensionManager;
import net.nemerosa.ontrack.model.extension.*;
import net.nemerosa.ontrack.model.support.StartupService;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExtensionManagerImpl implements ExtensionManager, StartupService {

    private final Logger logger = LoggerFactory.getLogger(ExtensionManager.class);

    private final ApplicationContext applicationContext;
    private Collection<? extends Extension> extensions;

    @Autowired
    public ExtensionManagerImpl(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public int startupOrder() {
        return 2;
    }

    /**
     * Startup: loads the extensions & features from the application context.
     * <p>
     * This cannot be done at construction time because of dependency cycle between
     * some extensions that need access to the extension manager.
     */
    @Override
    public void start() {
        logger.info("[extensions] Loading the extensions");
        extensions = applicationContext.getBeansOfType(Extension.class).values();
        logger.info("[extensions] Number of loaded extensions: {}", extensions.size());
        Collection<? extends ExtensionFeature> extensionFeatures = applicationContext.getBeansOfType(ExtensionFeature.class).values();
        logger.info("[extensions] Extension features:");
        for (ExtensionFeature feature : extensionFeatures) {
            logger.info("[extensions] * {} [{}]", feature.getName(), feature.getId());
        }
        // Detects cycles
        getExtensionList();
    }

    @Override
    public <T extends Extension> Collection<T> getExtensions(Class<T> extensionType) {
        // Filters the extensions
        List<Extension> collection = extensions.stream()
                .filter(extensionType::isInstance)
                .collect(Collectors.<Extension>toList());
        //noinspection unchecked
        return (Collection<T>) collection;
    }

    @Override
    public List<ExtensionFeature> getExtensionFeatures() {
        return applicationContext.getBeansOfType(ExtensionFeature.class).values().stream()
                .sorted(Comparator.comparing(ExtensionFeature::getName))
                .collect(Collectors.toList());
    }

    @Override
    public ExtensionList getExtensionList() {
        // Gets the list of all extensions
        List<ExtensionFeatureDescription> extensionFeatures = applicationContext.getBeansOfType(ExtensionFeature.class).values().stream()
                .map(ExtensionFeature::getFeatureDescription)
                .sorted(Comparator.comparing(ExtensionFeatureDescription::getName))
                .collect(Collectors.toList());

        // Computing the dependencies

        DefaultDirectedGraph<String, DefaultEdge> g = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Adds the extensions as vertexes
        extensionFeatures.forEach(extensionFeatureDescription ->
                        g.addVertex(extensionFeatureDescription.getId())
        );

        // Adds the dependencies as edges
        extensionFeatures.forEach(source ->
                source.getOptions().getDependencies().forEach(target ->
                                g.addEdge(source.getId(), target.getId())
                ));

        // Cycle detection
        CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(g);

        // If there are cycles
        if (cycleDetector.detectCycles()) {
            List<List<String>> cycles = new ArrayList<>();
            Set<String> cycleVertices = cycleDetector.findCycles();
            while (!cycleVertices.isEmpty()) {
                List<String> subCycleList = new ArrayList<>();
                // Get a vertex involved in a cycle.
                Iterator<String> iterator = cycleVertices.iterator();
                String cycle = iterator.next();
                // Get all vertices involved with this vertex.
                Set<String> subCycle = cycleDetector.findCyclesContainingVertex(cycle);
                for (String sub : subCycle) {
                    subCycleList.add(sub);
                    // Remove vertex so that this cycle is not encountered again.
                    cycleVertices.remove(sub);
                }
                // Adds to the list of cycles
                cycles.add(subCycleList);
            }
            // Throws an exception
            throw new ExtensionCycleException(cycles);
        }

        // OK
        return new ExtensionList(
                extensionFeatures
        );
    }

}
