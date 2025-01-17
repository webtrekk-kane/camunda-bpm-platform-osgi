package org.camunda.bpm.extension.osgi.blueprint;

import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import javax.inject.Inject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BlueprintIntegrationTest {

  @Inject
  private BundleContext ctx;
  @Inject
  @Filter(timeout = 30000L)
  private ProcessEngine engine;
  @Inject
  @Filter(timeout = 30000L)
  private RepositoryService repositoryService;
  @Inject
  @Filter(timeout = 30000L)
  private RuntimeService runtimeService;
  @Inject
  @Filter(timeout = 30000L)
  private TaskService taskService;
  @Inject
  @Filter(timeout = 30000L)
  private IdentityService identityService;
  @Inject
  @Filter(timeout = 30000L)
  private FormService formService;
  @Inject
  @Filter(timeout = 30000L)
  private HistoryService historyService;
  @Inject
  @Filter(timeout = 30000L)
  private ManagementService managementService;
  /**
   * to make sure the {@link BlueprintELResolver} found the JavaDelegate
   */
  private static boolean delegateVisited = false;

  public static final String CAMUNDA_VERSION = "7.2.0";

  @Configuration
  public Option[] createConfiguration() {
    Option[] camundaBundles = options(

      mavenBundle("org.camunda.bpm", "camunda-engine").versionAsInProject(),
      mavenBundle("org.camunda.bpm.model", "camunda-bpmn-model").versionAsInProject(),
      mavenBundle("org.camunda.bpm.model", "camunda-cmmn-model").versionAsInProject(),
      mavenBundle("org.camunda.bpm.model", "camunda-xml-model").versionAsInProject(),

//      mavenBundle("org.camunda.commons", "camunda-commons-logging", "1.0.6"),
//      mavenBundle("org.camunda.commons", "camunda-commons-utils", "1.0.6"),

      mavenBundle("joda-time", "joda-time").versionAsInProject(),
      mavenBundle("com.h2database", "h2").versionAsInProject(),
      mavenBundle("org.mybatis", "mybatis").versionAsInProject(),
      mavenBundle("com.fasterxml.uuid", "java-uuid-generator").versionAsInProject(),

//      mavenBundle("org.apache.logging.log4j", "log4j-api", "2.0-beta9"),
//      mavenBundle("org.apache.logging.log4j", "log4j-core", "2.0-beta9").
//        noStart(),

      mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager").versionAsInProject(),
      mavenBundle("org.camunda.bpm.extension.osgi", "camunda-bpm-osgi")
        .versionAsInProject(),

//      mavenBundle("org.slf4j", "slf4j-api", "1.7.7"),
//      mavenBundle("ch.qos.logback", "logback-core", "1.1.2"),
//      mavenBundle("ch.qos.logback", "logback-classic", "1.1.2"),
//      mavenBundle("org.assertj", "assertj-core", "1.5.0"),
      mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core", "1.0.0"),
      mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy", "1.0.0"),
      mavenBundle("org.apache.aries", "org.apache.aries.util", "1.0.0"),

      // make sure compiled classes from src/main are included
      bundle("reference:file:target/classes"));

    return OptionUtils.combine(
      camundaBundles,
      CoreOptions.junitBundles(),
      provision(createTestBundleWithProcessDefinition())
    );
  }

  private InputStream createTestBundleWithProcessDefinition() {
    try {
      return TinyBundles
        .bundle()
        .add(org.camunda.bpm.extension.osgi.Constants.BUNDLE_PROCESS_DEFINTIONS_DEFAULT + "testProcess.bpmn",
          new FileInputStream(new File("src/test/resources/testProcess.bpmn")))
        .set(Constants.BUNDLE_SYMBOLICNAME, "org.camunda.bpm.extension.osgi.example")
        .build();
    } catch (FileNotFoundException fnfe) {
      fail(fnfe.toString());
      return null;
    }
  }

  @Test
  public void exportedServices() {
    assertThat(engine, is(notNullValue()));
    assertThat(formService, is(notNullValue()));
    assertThat(historyService, is(notNullValue()));
    assertThat(identityService, is(notNullValue()));
    assertThat(managementService, is(notNullValue()));
    assertThat(repositoryService, is(notNullValue()));
    assertThat(runtimeService, is(notNullValue()));
    assertThat(taskService, is(notNullValue()));
  }

  @Test(timeout = 35000L)
  public void exportJavaDelegate() throws InterruptedException {
    Hashtable<String, String> properties = new Hashtable<String, String>();
    properties.put("osgi.service.blueprint.compname", "testDelegate");
    ctx.registerService(JavaDelegate.class.getName(), new TestDelegate(), properties);
    // wait a little bit
    ProcessDefinition definition = null;
    do {
      Thread.sleep(500L);
      definition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("Process_1").singleResult();
    } while (definition == null);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(definition.getKey());
    assertThat(processInstance.isEnded(), is(true));
    assertThat(delegateVisited, is(true));
  }

  private static class TestDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      delegateVisited = true;
    }
  }
}
