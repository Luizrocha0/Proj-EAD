package com.rotavital.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectFile;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectFile("docs/arquitetura/u2-parte-2/atendimento-integral.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.rotavital.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty,json:target/cucumber/atendimento.json")
public class AtendimentoCucumberIT {
}
