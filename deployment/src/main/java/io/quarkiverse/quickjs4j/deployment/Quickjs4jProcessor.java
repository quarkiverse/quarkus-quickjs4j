package io.quarkiverse.quickjs4j.deployment;

import io.quarkiverse.quickjs4j.ScriptInterfaceFactory;
import io.quarkiverse.quickjs4j.util.ScriptInterfaceUtils;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class Quickjs4jProcessor {

    private static final String FEATURE = "quickjs4j-cdi";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public void build(BuildProducer<AdditionalIndexedClassesBuildItem> producer) {
        producer.produce(new AdditionalIndexedClassesBuildItem(ScriptInterfaceFactory.class.getName()));
        producer.produce(new AdditionalIndexedClassesBuildItem(ScriptInterfaceUtils.class.getName()));
    }

}
